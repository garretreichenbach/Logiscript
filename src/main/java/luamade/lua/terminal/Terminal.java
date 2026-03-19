package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.lua.peripheral.PeripheralsApi;
import luamade.lua.util.UtilApi;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Implements a Unix-like terminal/shell interface for computers in the game.
 * This allows scripts to create a command-line interface for interacting with the computer.
 */
public class Terminal extends LuaMadeUserdata {
	private static final String STARTUP_SCRIPT_PATH = "/etc/startup.lua";
	private static final String DEFAULT_PROMPT_TEMPLATE = "{name}:{dir} $ ";

	private final ComputerModule module;
	private final Console console;
	private final FileSystem fileSystem;
	private final Map<String, Command> commands = new HashMap<>();
	private final Map<Integer, BackgroundJob> backgroundJobs = new ConcurrentHashMap<>();
	private final ExecutorService scriptExecutor;
	private final ScheduledExecutorService scriptScheduler;
	private volatile Semaphore scriptSlots;
	private volatile int maxParallelSlots;
	private final List<String> history = new ArrayList<>();
	private final AtomicInteger nextJobId = new AtomicInteger(1);
	private final AtomicInteger activeScripts = new AtomicInteger(0);
	private int historyIndex;
	private String currentInput = "";
	private boolean running;
	private boolean autoPromptEnabled = true;
	private String promptTemplate = DEFAULT_PROMPT_TEMPLATE;

	public Terminal(ComputerModule module, Console console, FileSystem fileSystem) {
		this.module = module;
		this.console = console;
		this.fileSystem = fileSystem;
		maxParallelSlots = ConfigManager.getScriptMaxParallel();
		scriptSlots = new Semaphore(maxParallelSlots, true);
		scriptExecutor = Executors.newCachedThreadPool(new ScriptThreadFactory("luamade-script"));
		scriptScheduler = Executors.newSingleThreadScheduledExecutor(new ScriptThreadFactory("luamade-script-watchdog"));

		// Register built-in commands
		registerBuiltInCommands();
	}

	/**
	 * Starts the terminal
	 */
	@LuaMadeCallable
	public void start() {
		if(running) return;

		running = true;
		bootTerminal(false);
	}

	/**
	 * Stops the terminal
	 */
	@LuaMadeCallable
	public void stop() {
		running = false;
	}

	@LuaMadeCallable
	public void reboot() {
		if(!running) {
			running = true;
		}
		bootTerminal(true);
	}

	/**
	 * Handles input from the user
	 */
	@LuaMadeCallable
	public void handleInput(String input) {
		if(!running) {
			return;
		}

		String submittedInput = input == null ? "" : input;

		// Commit the typed command into the console transcript before processing.
		console.appendInline(valueOf(submittedInput));

		// Move to a new line after the inline prompt when Enter is pressed.
		console.print(valueOf(""));

		String commandLine = submittedInput.trim();
		if(!commandLine.isEmpty()) {
			String expandedHistoryCommand = expandHistoryCommand(commandLine);
			if(expandedHistoryCommand == null) {
				if(autoPromptEnabled) {
					printPrompt();
				}
				return;
			}

			if(!expandedHistoryCommand.equals(commandLine)) {
				console.print(valueOf(expandedHistoryCommand));
			}
			commandLine = expandedHistoryCommand;

			history.add(commandLine);
			historyIndex = history.size();
		}

		// Process the command
		List<String> tokens = parseCommandTokens(commandLine);
		if(tokens.isEmpty()) {
			if(autoPromptEnabled) {
				printPrompt();
			}
			return;
		}

		String commandName = tokens.get(0);
		String args = joinTokens(tokens, 1);

		// Execute the command
		Command command = commands.get(commandName);
		if(command != null) {
			command.execute(args);
		} else {
			String resolvedScriptPath = resolveScriptPath(commandName);
			if(resolvedScriptPath != null) {
				runScriptAtPath(resolvedScriptPath, tokens.subList(1, tokens.size()));
			} else {
				console.print(valueOf("Unknown command: " + commandName));
			}
		}

		if(autoPromptEnabled) {
			printPrompt();
		}
	}

	/**
	 * Executes a Lua script with arguments
	 */
	private boolean executeScript(String scriptPath, String script, List<String> args) {
		try {
			// Create a sandboxed Lua environment
			Globals globals = createSandboxedGlobals();
			
			// Set up arguments
			LuaTable argsTable = new LuaTable();
			for(int i = 0; i < args.size(); i++) {
				argsTable.set(i + 1, valueOf(args.get(i)));
			}
			globals.set("args", argsTable);
			
			// Execute the script
			LuaValue chunk = globals.load(script, scriptPath);
			chunk.call();
			return true;
		} catch(LuaError error) {
			String message = error.getMessage();
			if(message == null || message.trim().isEmpty()) {
				message = error.toString();
			}
			console.print(valueOf("Lua error in " + scriptPath + ": " + message));
			return false;
		} catch(Exception e) {
			console.print(valueOf("Error executing script: " + e.getMessage()));
			return false;
		}
	}

	private Future<Boolean> submitScriptTask(String scriptPath, String script, List<String> args) {
		return scriptExecutor.submit(() -> {
			activeScripts.incrementAndGet();
			try {
				return executeScript(scriptPath, script, args);
			} finally {
				activeScripts.decrementAndGet();
				scriptSlots.release();
			}
		});
	}

	private boolean acquireScriptSlot() {
		refreshRuntimeLimits();
		ScriptOverloadMode mode = ScriptOverloadMode.fromConfigValue(ConfigManager.getScriptOverloadMode());

		try {
			switch(mode) {
				case HARD_STOP:
					return scriptSlots.tryAcquire();
				case STALL:
					scriptSlots.acquire();
					return true;
				case HYBRID:
				default:
					int waitMs = ConfigManager.getScriptQueueWaitMs();
					if(waitMs <= 0) {
						return scriptSlots.tryAcquire();
					}
					return scriptSlots.tryAcquire(waitMs, TimeUnit.MILLISECONDS);
			}
		} catch(InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private void refreshRuntimeLimits() {
		int configuredMaxParallel = ConfigManager.getScriptMaxParallel();
		if(configuredMaxParallel == maxParallelSlots || activeScripts.get() > 0) {
			return;
		}

		synchronized(this) {
			if(configuredMaxParallel == maxParallelSlots || activeScripts.get() > 0) {
				return;
			}

			maxParallelSlots = configuredMaxParallel;
			scriptSlots = new Semaphore(maxParallelSlots, true);
		}
	}

	private boolean executeScriptWithBudget(String scriptPath, String script, List<String> args, long timeoutMs) {
		if(!acquireScriptSlot()) {
			ScriptOverloadMode mode = ScriptOverloadMode.fromConfigValue(ConfigManager.getScriptOverloadMode());
			if(mode == ScriptOverloadMode.HARD_STOP) {
				console.print(valueOf("Error: Script rejected due to server load (hard-stop mode)"));
			} else if(mode == ScriptOverloadMode.HYBRID) {
				console.print(valueOf("Error: Script queue wait expired under load"));
			} else {
				console.print(valueOf("Error: Script scheduling interrupted"));
			}
			return false;
		}

		Future<Boolean> future = submitScriptTask(scriptPath, script, args);
		try {
			Boolean success = future.get(timeoutMs, TimeUnit.MILLISECONDS);
			return Boolean.TRUE.equals(success);
		} catch(TimeoutException timeoutException) {
			future.cancel(true);
			console.print(valueOf("Error: Script exceeded time budget (" + timeoutMs + "ms)"));
			return false;
		} catch(InterruptedException interruptedException) {
			Thread.currentThread().interrupt();
			future.cancel(true);
			console.print(valueOf("Error: Script execution interrupted"));
			return false;
		} catch(ExecutionException executionException) {
			console.print(valueOf("Error executing script: " + executionException.getMessage()));
			return false;
		}
	}

	private int startBackgroundScript(String scriptPath, String script, List<String> args) {
		refreshBackgroundJobs();
		if(!acquireScriptSlot()) {
			return -1;
		}

		int jobId = nextJobId.getAndIncrement();
		BackgroundJob job = new BackgroundJob(jobId, scriptPath);
		Future<Boolean> future = submitScriptTask(scriptPath, script, args);
		job.setFuture(future);
		backgroundJobs.put(jobId, job);

		scriptScheduler.schedule(() -> {
			if(!future.isDone()) {
				job.setStatus(JobStatus.TIMED_OUT);
				future.cancel(true);
			}
		}, ConfigManager.getScriptTimeoutMs(), TimeUnit.MILLISECONDS);

		return jobId;
	}

	private void refreshBackgroundJobs() {
		for(BackgroundJob job : backgroundJobs.values()) {
			if(job.getStatus() != JobStatus.RUNNING) {
				continue;
			}

			Future<Boolean> future = job.getFuture();
			if(future == null || !future.isDone()) {
				continue;
			}

			try {
				Boolean success = future.get();
				job.setStatus(Boolean.TRUE.equals(success) ? JobStatus.COMPLETED : JobStatus.FAILED);
			} catch(CancellationException cancellationException) {
				if(job.getStatus() == JobStatus.RUNNING) {
					job.setStatus(JobStatus.CANCELED);
				}
			} catch(InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				job.setStatus(JobStatus.FAILED);
			} catch(ExecutionException executionException) {
				job.setStatus(JobStatus.FAILED);
			}
		}
	}

	private void runScriptAtPath(String scriptPath, List<String> args) {
		String script = fileSystem.read(scriptPath);
		if(script == null) {
			console.print(valueOf("Error: Could not read script file: " + scriptPath));
			return;
		}

		console.print(valueOf("Running script: " + scriptPath));
		executeScriptWithBudget(scriptPath, script, args, ConfigManager.getScriptTimeoutMs());
	}

	private void bootTerminal(boolean clearConsole) {
		if(clearConsole) {
			console.clearTextContents();
		}

		resetRuntimeConfiguration();
		boolean startupHandled = runStartupScript();
		if(!startupHandled) {
			printDefaultStartupBanner();
		}

		if(autoPromptEnabled && running) {
			printPrompt();
		}
	}

	private void resetRuntimeConfiguration() {
		autoPromptEnabled = true;
		promptTemplate = DEFAULT_PROMPT_TEMPLATE;
	}

	private boolean runStartupScript() {
		if(!fileSystem.exists(STARTUP_SCRIPT_PATH) || fileSystem.isDir(STARTUP_SCRIPT_PATH)) {
			return false;
		}

		String script = fileSystem.read(STARTUP_SCRIPT_PATH);
		if(script == null) {
			console.print(valueOf("Error: Could not read startup script: " + STARTUP_SCRIPT_PATH));
			return false;
		}

		return executeScriptWithBudget(STARTUP_SCRIPT_PATH, script, new ArrayList<String>(), ConfigManager.getStartupScriptTimeoutMs());
	}

	private void printDefaultStartupBanner() {
		console.print(valueOf("LuaMade Terminal v1.0"));
		console.print(valueOf("Type 'help' for a list of commands"));
	}

	private String resolveScriptPath(String commandOrPath) {
		if(commandOrPath == null || commandOrPath.trim().isEmpty()) {
			return null;
		}

		String trimmed = commandOrPath.trim();
		String normalized = fileSystem.normalizePath(trimmed);
		if(isLuaFile(normalized)) {
			return normalized;
		}

		if(!trimmed.endsWith(".lua")) {
			String normalizedWithExt = fileSystem.normalizePath(trimmed + ".lua");
			if(isLuaFile(normalizedWithExt)) {
				return normalizedWithExt;
			}
		}

		if(!trimmed.startsWith("/")) {
			String binCandidate = fileSystem.normalizePath("/bin/" + trimmed);
			if(isLuaFile(binCandidate)) {
				return binCandidate;
			}
			if(!trimmed.endsWith(".lua")) {
				String binCandidateWithExt = fileSystem.normalizePath("/bin/" + trimmed + ".lua");
				if(isLuaFile(binCandidateWithExt)) {
					return binCandidateWithExt;
				}
			}
		}

		return null;
	}

	private boolean isLuaFile(String path) {
		return path != null && fileSystem.exists(path) && !fileSystem.isDir(path);
	}

	private List<String> parseCommandTokens(String input) {
		List<String> tokens = new ArrayList<>();
		if(input == null || input.isEmpty()) {
			return tokens;
		}

		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;

		for(int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if(c == '\\' && i + 1 < input.length()) {
				char next = input.charAt(i + 1);
				if(next == '\\' || next == '"' || next == '\'') {
					current.append(next);
					i++;
					continue;
				}
			}

			if((c == '"' || c == '\'') && (!inQuotes || c == quoteChar)) {
				if(inQuotes) {
					inQuotes = false;
					quoteChar = 0;
				} else {
					inQuotes = true;
					quoteChar = c;
				}
				continue;
			}

			if(Character.isWhitespace(c) && !inQuotes) {
				if(current.length() > 0) {
					tokens.add(current.toString());
					current.setLength(0);
				}
				continue;
			}

			current.append(c);
		}

		if(current.length() > 0) {
			tokens.add(current.toString());
		}

		return tokens;
	}

	private String joinTokens(List<String> tokens, int startIndex) {
		if(tokens == null || startIndex >= tokens.size()) {
			return "";
		}

		StringBuilder builder = new StringBuilder();
		for(int i = startIndex; i < tokens.size(); i++) {
			if(builder.length() > 0) {
				builder.append(' ');
			}
			builder.append(tokens.get(i));
		}
		return builder.toString();
	}

	private String expandHistoryCommand(String commandLine) {
		if(commandLine == null) {
			return "";
		}

		String trimmed = commandLine.trim();
		if(!trimmed.startsWith("!") || trimmed.length() < 2) {
			return trimmed;
		}

		String indexToken = trimmed.substring(1);
		for(int i = 0; i < indexToken.length(); i++) {
			if(!Character.isDigit(indexToken.charAt(i))) {
				console.print(valueOf("Error: Usage: !<history-number>"));
				return null;
			}
		}

		int requestedIndex;
		try {
			requestedIndex = Integer.parseInt(indexToken);
		} catch(NumberFormatException numberFormatException) {
			console.print(valueOf("Error: History index is out of range"));
			return null;
		}

		if(requestedIndex < 1 || requestedIndex > history.size()) {
			console.print(valueOf("Error: History index out of range (1-" + history.size() + ")"));
			return null;
		}

		return history.get(requestedIndex - 1);
	}

	/**
	 * Creates a sandboxed Lua globals environment for script execution
	 */
	private Globals createSandboxedGlobals() {
		Globals globals = new Globals();
		
		// Load only safe libraries
		globals.load(new BaseLib());
		// String/Table libs register themselves via package.loaded in LuaJ.
		globals.load(new PackageLib());
		globals.load(new StringLib());
		globals.load(new TableLib());
		globals.load(new JseMathLib());
		globals.load(new Bit32Lib());
		LuaC.install(globals);
		
		// Remove unsafe functions
		globals.set("dofile", NIL);
		globals.set("loadfile", NIL);
		globals.set("load", NIL);
		globals.set("require", NIL);
		globals.set("package", NIL);
		
		// Expose safe APIs
		globals.set("console", console);
 		globals.set("print", createConsolePrintBridge());
		globals.set("fs", fileSystem);
		LuaTable terminalApi = createSafeTerminalApiProxy();
		globals.set("term", terminalApi);
		// Backward compatibility for older scripts that used `terminal`.
		globals.set("terminal", terminalApi);
		// Expose raw userdata for advanced scripts that rely on userdata behavior.
		globals.set("termRaw", this);
		globals.set("net", module.getNetworkInterface());
		globals.set("peripheral", new PeripheralsApi(module));
		globals.set("input", module.getInputApi());
		globals.set("shell", createShellCompatibilityApi());

		LuaTable utilLibrary = loadBuiltinLibrary(globals, "scripts/lib/util.lua", "util");

		UtilApi nativeUtil = new UtilApi();
		if(utilLibrary != null) {
			utilLibrary.set("now", nativeUtil.get("now"));
			utilLibrary.set("sleep", nativeUtil.get("sleep"));
		} else {
			globals.set("util", nativeUtil);
		}
		return globals;
	}

	private LuaFunction createConsolePrintBridge() {
		return new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs vargs) {
				console.print(vargs);
				return NONE;
			}
		};
	}

	private LuaTable createSafeTerminalApiProxy() {
		LuaTable proxy = new LuaTable();
		LuaTable metatable = new LuaTable();

		metatable.set(INDEX, new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue table, LuaValue key) {
				if(!key.isstring()) {
					return NIL;
				}

				try {
					return Terminal.this.get(key);
				} catch(LuaError error) {
					// Unknown methods should resolve to nil like regular Lua tables.
					return NIL;
				}
			}
		});

		proxy.setmetatable(metatable);
		return proxy;
	}

	private LuaTable createShellCompatibilityApi() {
		LuaTable shell = new LuaTable();
		shell.set("run", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs vargs) {
				int startIndex = vargs.narg() > 0 && vargs.arg1().istable() ? 2 : 1;
				if(vargs.narg() < startIndex || vargs.arg(startIndex).isnil()) {
					return FALSE;
				}

				String commandOrPath = vargs.arg(startIndex).tojstring();
				String resolvedPath = resolveScriptPath(commandOrPath);
				if(resolvedPath == null) {
					console.print(valueOf("Error: shell.run could not resolve script: " + commandOrPath));
					return FALSE;
				}

				List<String> scriptArgs = new ArrayList<>();
				for(int i = startIndex + 1; i <= vargs.narg(); i++) {
					scriptArgs.add(vargs.arg(i).tojstring());
				}

				String script = fileSystem.read(resolvedPath);
				if(script == null) {
					console.print(valueOf("Error: Could not read script file: " + resolvedPath));
					return FALSE;
				}

				boolean success = executeScript(resolvedPath, script, scriptArgs);
				return success ? TRUE : FALSE;
			}
		});

		return shell;
	}

	private LuaTable loadBuiltinLibrary(Globals globals, String resourcePath, String globalName) {
		String source = readBuiltinResource(resourcePath);
		if(source == null) {
			return null;
		}

		try {
			LuaValue chunk = globals.load(source, resourcePath);
			LuaValue result = chunk.call();
			if(result.istable()) {
				LuaTable table = (LuaTable) result;
				globals.set(globalName, table);
				return table;
			}
		} catch(LuaError error) {
			console.print(valueOf("Lua error loading builtin library '" + globalName + "': " + error.getMessage()));
		} catch(Exception exception) {
			console.print(valueOf("Error loading builtin library '" + globalName + "': " + exception.getMessage()));
		}

		return null;
	}

	private String readBuiltinResource(String resourcePath) {
		InputStream input = getClass().getClassLoader().getResourceAsStream(resourcePath);
		if(input == null) {
			return null;
		}

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
			StringBuilder builder = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				if(builder.length() > 0) {
					builder.append('\n');
				}
				builder.append(line);
			}
			return builder.toString();
		} catch(IOException exception) {
			console.print(valueOf("Error reading builtin resource: " + resourcePath));
			return null;
		}
	}

	/**
	 * Registers a new command
	 */
	@LuaMadeCallable
	public void registerCommand(String name, LuaValue callback) {
		if(name == null || name.isEmpty() || callback == null || !callback.isfunction()) {
			return;
		}

		commands.put(name, new LuaCommand(name, callback));
	}

	/**
	 * Gets the previous command from history
	 */
	@LuaMadeCallable
	public String getPreviousCommand() {
		if(history.isEmpty()) {
			return "";
		}

		if(historyIndex > 0) {
			historyIndex--;
		}

		if(historyIndex < history.size()) {
			return history.get(historyIndex);
		}

		return "";
	}

	/**
	 * Gets the next command from history
	 */
	@LuaMadeCallable
	public String getNextCommand() {
		if(history.isEmpty()) {
			return "";
		}

		if(historyIndex < history.size() - 1) {
			historyIndex++;
			return history.get(historyIndex);
		} else {
			historyIndex = history.size();
			return currentInput;
		}
	}

	/**
	 * Sets the current input
	 */
	@LuaMadeCallable
	public void setCurrentInput(String input) {
		currentInput = input;
	}

	@LuaMadeCallable
	public boolean runCommand(String commandLine) {
		if(commandLine == null) {
			return false;
		}

		handleInput(commandLine);
		return true;
	}

	@LuaMadeCallable
	public boolean isBusy() {
		return activeScripts.get() > 0;
	}

	@LuaMadeCallable
	public String httpGet(String url) {
		return fetchWebData(url);
	}

	@LuaMadeCallable
	public void setPromptTemplate(String template) {
		if(template == null || template.trim().isEmpty()) {
			promptTemplate = DEFAULT_PROMPT_TEMPLATE;
			return;
		}

		if(template.length() > 256) {
			promptTemplate = template.substring(0, 256);
			return;
		}

		promptTemplate = template;
	}

	@LuaMadeCallable
	public String getPromptTemplate() {
		return promptTemplate;
	}

	@LuaMadeCallable
	public void resetPromptTemplate() {
		promptTemplate = DEFAULT_PROMPT_TEMPLATE;
	}

	@LuaMadeCallable
	public void setAutoPrompt(boolean enabled) {
		autoPromptEnabled = enabled;
	}

	@LuaMadeCallable
	public boolean isAutoPromptEnabled() {
		return autoPromptEnabled;
	}

	/**
	 * Prints the command prompt
	 */
	private void printPrompt() {
		console.appendInline(valueOf(formatPrompt()));
	}

	private String formatPrompt() {
		String template = promptTemplate == null || promptTemplate.isEmpty() ? DEFAULT_PROMPT_TEMPLATE : promptTemplate;
		String promptPath = getPromptPath();
		String hostname = module.getNetworkInterface() != null ? module.getNetworkInterface().getHostname() : "unknown";

		return template
			.replace("{name}", module.getPromptComputerName())
			.replace("{display}", module.getDisplayName())
			.replace("{hostname}", hostname)
			.replace("{dir}", promptPath);
	}

	private String getPromptPath() {
		String currentDir = fileSystem.getCurrentDir();
		if(currentDir == null || currentDir.isEmpty()) {
			return "/";
		}
		// Only show virtual filesystem style paths in the prompt.
		return currentDir.startsWith("/") ? currentDir : "/" + currentDir;
	}

	private String fsErrorOr(String fallback) {
		String fsError = fileSystem.getLastError();
		if(fsError == null || fsError.trim().isEmpty()) {
			return fallback;
		}
		return "Error: " + fsError;
	}

	/**
	 * Registers the built-in commands
	 */
	private void registerBuiltInCommands() {
		// Help command
		commands.put("help", new Command("help", "Displays a list of available commands") {
			@Override
			public void execute(String args) {
				console.print(valueOf("Available commands:"));
				for(Command command : commands.values()) {
					console.print(valueOf("    " + command.getName() + " - " + command.getDescription()));
				}
			}
		});

		// History command
		commands.put("history", new Command("history", "Shows command history") {
			@Override
			public void execute(String args) {
				if(history.isEmpty()) {
					console.print(valueOf("No history"));
					return;
				}

				for(int i = 0; i < history.size(); i++) {
					console.print(valueOf((i + 1) + "  " + history.get(i)));
				}
			}
		});

		// Which command
		commands.put("which", new Command("which", "Shows where a command or file resolves") {
			@Override
			public void execute(String args) {
				WhichArgs parsed = parseWhichArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.target == null || parsed.target.trim().isEmpty()) {
					console.print(valueOf("Error: Usage: which [-a] <command-or-path>"));
					return;
				}

				List<String> matches = findWhichMatches(parsed.target, parsed.showAll);
				if(matches.isEmpty()) {
					console.print(valueOf(parsed.target + " not found"));
					return;
				}

				for(String match : matches) {
					console.print(valueOf(match));
				}
			}
		});

		commands.put("fsauth", new Command("fsauth", "Unlocks or clears password-gated filesystem scopes") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					console.print(valueOf("Error: Usage: fsauth <password> | fsauth --clear"));
					return;
				}

				if("--clear".equals(trimmed)) {
					fileSystem.clearAuth();
					console.print(valueOf("Cleared filesystem auth session"));
					return;
				}

				if(fileSystem.auth(trimmed)) {
					console.print(valueOf("Filesystem scopes unlocked"));
				} else {
					console.print(valueOf(fsErrorOr("Error: Invalid filesystem password")));
				}
			}
		});

		commands.put("protect", new Command("protect", "Protects a path with a password and optional operations") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.size() < 2) {
					console.print(valueOf("Error: Usage: protect <path> <password> [read,write,delete,list|copy|move|paste|all]"));
					return;
				}

				String path = parts.get(0);
				String password = parts.get(1);
				String operations = parts.size() > 2 ? joinTokens(parts, 2) : "all";

				if(fileSystem.protect(path, password, operations)) {
					console.print(valueOf("Protection enabled for " + fileSystem.normalizePath(path)));
				} else {
					console.print(valueOf(fsErrorOr("Error: Could not protect path")));
				}
			}
		});

		commands.put("unprotect", new Command("unprotect", "Removes password protection from a path") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.size() < 2) {
					console.print(valueOf("Error: Usage: unprotect <path> <password>"));
					return;
				}

				if(fileSystem.unprotect(parts.get(0), parts.get(1))) {
					console.print(valueOf("Protection removed for " + fileSystem.normalizePath(parts.get(0))));
				} else {
					console.print(valueOf(fsErrorOr("Error: Could not remove protection")));
				}
			}
		});

		commands.put("perms", new Command("perms", "Shows protected filesystem scopes or rule for a path") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					List<String> rules = fileSystem.listPermissions();
					if(rules.isEmpty()) {
						console.print(valueOf("No protected scopes configured"));
						return;
					}

					for(String rule : rules) {
						console.print(valueOf(rule));
					}
					return;
				}

				console.print(valueOf(fileSystem.getPermissions(trimmed)));
			}
		});

		// Name command
		commands.put("name", new Command("name", "Gets or sets the displayed computer name") {
			@Override
			public void execute(String args) {
				String value = args == null ? "" : args.trim();
				if(value.isEmpty()) {
					console.print(valueOf("Display name: " + module.getDisplayName()));
					return;
				}

				if("--reset".equals(value)) {
					module.resetDisplayName();
					console.print(valueOf("Display name reset to: " + module.getDisplayName()));
					return;
				}

				if(module.setDisplayName(value)) {
					console.print(valueOf("Display name set to: " + module.getDisplayName()));
				} else {
					console.print(valueOf("Error: Invalid name (1-32 chars, no newlines)"));
				}
			}
		});

		// Head command
		commands.put("head", new Command("head", "Prints the first lines of a file") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					console.print(valueOf("Error: Usage: head [-n lines] <file>"));
					return;
				}

				HeadTailArgs parsed = parseHeadTailArgs(parseCommandTokens(trimmed), 10);
				if(parsed == null) {
					return;
				}

				String content = fileSystem.read(parsed.filePath);
				if(content == null) {
					console.print(valueOf("Error: File not found or is a directory"));
					return;
				}

				String[] lines = content.split("\\n", -1);
				int max = Math.min(parsed.lineCount, lines.length);
				for(int i = 0; i < max; i++) {
					console.print(valueOf(lines[i]));
				}
			}
		});

		// Tail command
		commands.put("tail", new Command("tail", "Prints the last lines of a file") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					console.print(valueOf("Error: Usage: tail [-n lines] <file>"));
					return;
				}

				HeadTailArgs parsed = parseHeadTailArgs(parseCommandTokens(trimmed), 10);
				if(parsed == null) {
					return;
				}

				String content = fileSystem.read(parsed.filePath);
				if(content == null) {
					console.print(valueOf("Error: File not found or is a directory"));
					return;
				}

				String[] lines = content.split("\\n", -1);
				int start = Math.max(0, lines.length - parsed.lineCount);
				for(int i = start; i < lines.length; i++) {
					console.print(valueOf(lines[i]));
				}
			}
		});

		// Wc command
		commands.put("wc", new Command("wc", "Prints line, word, and byte counts") {
			@Override
			public void execute(String args) {
				WcArgs parsed = parseWcArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.filePaths.isEmpty()) {
					console.print(valueOf("Error: Usage: wc [-l] [-w] [-c] <file>..."));
					return;
				}

				long totalLines = 0L;
				long totalWords = 0L;
				long totalBytes = 0L;
				int printed = 0;

				for(String rawPath : parsed.filePaths) {
					String normalizedPath = fileSystem.normalizePath(rawPath);
					String content = fileSystem.read(normalizedPath);
					if(content == null) {
						console.print(valueOf(fsErrorOr("Error: File not found or is a directory")));
						continue;
					}

					int lineCount = content.isEmpty() ? 0 : content.split("\\n", -1).length;
					int wordCount = countWords(content);
					int byteCount = content.getBytes(StandardCharsets.UTF_8).length;

					totalLines += lineCount;
					totalWords += wordCount;
					totalBytes += byteCount;
					printed++;

					console.print(valueOf(formatWcOutput(parsed, lineCount, wordCount, byteCount, normalizedPath)));
				}

				if(printed > 1) {
					console.print(valueOf(formatWcOutput(parsed, totalLines, totalWords, totalBytes, "total")));
				}
			}
		});

		// Echo command
		commands.put("echo", new Command("echo", "Displays a message") {
			@Override
			public void execute(String args) {
				List<String> tokens = parseCommandTokens(args == null ? "" : args);
				boolean noNewline = false;
				int startIndex = 0;

				if(!tokens.isEmpty() && "-n".equals(tokens.get(0))) {
					noNewline = true;
					startIndex = 1;
				}

				String message = joinTokens(tokens, startIndex);
				if(noNewline) {
					console.appendInline(valueOf(message));
				} else {
					console.print(valueOf(message));
				}
			}
		});

		// Ls command
		commands.put("ls", new Command("ls", "Lists files in a directory") {
			@Override
			public void execute(String args) {
				LsArgs parsed = parseLsArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null) {
					console.print(valueOf("Error: Usage: ls [-a] [-l] [-R] [path]"));
					return;
				}

				String targetPath = parsed.path == null ? fileSystem.getCurrentDir() : parsed.path;
				String normalizedTargetPath = fileSystem.normalizePath(targetPath);

				if(!fileSystem.exists(normalizedTargetPath)) {
					console.print(valueOf(fsErrorOr("Error: Path not found")));
					return;
				}

				if(!fileSystem.isDir(normalizedTargetPath)) {
					printLsEntry(normalizedTargetPath, baseName(normalizedTargetPath), parsed);
					return;
				}

				if(!printLsDirectory(normalizedTargetPath, parsed, false)) {
					console.print(valueOf(fsErrorOr("Error: Could not list directory")));
				}
			}
		});

		// Find command
		commands.put("find", new Command("find", "Finds files and directories") {
			@Override
			public void execute(String args) {
				FindArgs parsed = parseFindArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null) {
					console.print(valueOf("Error: Usage: find [path] [-name <glob>] [-type f|d] [-maxdepth <n>]"));
					return;
				}

				String targetPath = parsed.path == null ? fileSystem.getCurrentDir() : parsed.path;
				String normalizedTargetPath = fileSystem.normalizePath(targetPath);
				if(!fileSystem.exists(normalizedTargetPath)) {
					console.print(valueOf(fsErrorOr("Error: Path not found")));
					return;
				}

				if(!walkFind(normalizedTargetPath, parsed, 0)) {
					console.print(valueOf(fsErrorOr("Error: find traversal failed")));
				}
			}
		});

		// Grep command
		commands.put("grep", new Command("grep", "Searches for text patterns") {
			@Override
			public void execute(String args) {
				GrepArgs parsed = parseGrepArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.pattern == null || parsed.path == null) {
					console.print(valueOf("Error: Usage: grep [-n] [-i] [-r] <pattern> <path>"));
					return;
				}

				String normalizedPath = fileSystem.normalizePath(parsed.path);
				if(!fileSystem.exists(normalizedPath)) {
					console.print(valueOf(fsErrorOr("Error: Path not found")));
					return;
				}

				if(fileSystem.isDir(normalizedPath) && !parsed.recursive) {
					console.print(valueOf("Error: Path is a directory (use -r)"));
					return;
				}

				if(!grepPath(normalizedPath, parsed)) {
					console.print(valueOf(fsErrorOr("Error: grep failed")));
				}
			}
		});

		// Stat command
		commands.put("stat", new Command("stat", "Shows file or directory metadata") {
			@Override
			public void execute(String args) {
				StatArgs parsed = parseStatArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.paths.isEmpty()) {
					console.print(valueOf("Error: Usage: stat <path>..."));
					return;
				}

				for(int i = 0; i < parsed.paths.size(); i++) {
					String normalizedPath = fileSystem.normalizePath(parsed.paths.get(i));
					if(!printStatEntry(normalizedPath)) {
						console.print(valueOf(fsErrorOr("Error: Could not stat " + normalizedPath)));
					}

					if(i < parsed.paths.size() - 1) {
						console.print(valueOf(""));
					}
				}
			}
		});

		// Tree command
		commands.put("tree", new Command("tree", "Displays a directory tree") {
			@Override
			public void execute(String args) {
				TreeArgs parsed = parseTreeArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null) {
					console.print(valueOf("Error: Usage: tree [-a] [-L depth] [path]"));
					return;
				}

				String targetPath = parsed.path == null ? fileSystem.getCurrentDir() : parsed.path;
				String normalizedTargetPath = fileSystem.normalizePath(targetPath);
				if(!fileSystem.exists(normalizedTargetPath)) {
					console.print(valueOf(fsErrorOr("Error: Path not found")));
					return;
				}

				console.print(valueOf(normalizedTargetPath));
				if(!walkTree(normalizedTargetPath, "", true, 0, parsed)) {
					console.print(valueOf(fsErrorOr("Error: tree traversal failed")));
				}
			}
		});

		// Cd command
		commands.put("cd", new Command("cd", "Changes the current directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					args = "/";
				}

				if(fileSystem.changeDir(args)) {
					// Success, nothing to print
				} else {
					console.print(valueOf(fsErrorOr("Error: Directory not found")));
				}
			}
		});

		// Pwd command
		commands.put("pwd", new Command("pwd", "Prints the current working directory") {
			@Override
			public void execute(String args) {
				PwdArgs parsed = parsePwdArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null) {
					console.print(valueOf("Error: Usage: pwd [-L|-P]"));
					return;
				}

				String current = fileSystem.getCurrentDir();
				if(parsed.physical) {
					console.print(valueOf(fileSystem.normalizePath(current)));
					return;
				}

				console.print(valueOf(current));
			}
		});

		// Mkdir command
		commands.put("mkdir", new Command("mkdir", "Creates a new directory") {
			@Override
			public void execute(String args) {
				MkdirArgs parsed = parseMkdirArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.paths.isEmpty()) {
					console.print(valueOf("Error: Usage: mkdir [-p] <directory>..."));
					return;
				}

				boolean createdAny = false;
				for(String rawPath : parsed.paths) {
					String normalizedPath = fileSystem.normalizePath(rawPath);
					if(fileSystem.exists(normalizedPath)) {
						if(fileSystem.isDir(normalizedPath) && parsed.parents) {
							createdAny = true;
							continue;
						}
						console.print(valueOf("Error: Directory already exists: " + normalizedPath));
						continue;
					}

					if(fileSystem.makeDir(normalizedPath)) {
						createdAny = true;
					} else {
						console.print(valueOf(fsErrorOr("Error: Could not create directory")));
					}
				}

				if(createdAny) {
					console.print(valueOf("Directory created"));
				}
			}
		});

		// Cat command
		commands.put("cat", new Command("cat", "Displays the contents of a file") {
			@Override
			public void execute(String args) {
				CatArgs parsed = parseCatArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.filePaths.isEmpty()) {
					console.print(valueOf("Error: Usage: cat [-n] [-A] <file>..."));
					return;
				}

				int lineNumber = 1;
				for(String rawPath : parsed.filePaths) {
					String normalizedPath = fileSystem.normalizePath(rawPath);
					String content = fileSystem.read(normalizedPath);
					if(content == null) {
						console.print(valueOf(fsErrorOr("Error: File not found or is a directory")));
						continue;
					}

					String[] lines = content.split("\\n", -1);
					for(String line : lines) {
						String rendered = parsed.showAllChars ? renderCatAll(line) : line;
						if(parsed.showLineNumbers) {
							console.print(valueOf(lineNumber + "\t" + rendered));
							lineNumber++;
						} else {
							console.print(valueOf(rendered));
						}
					}
				}
			}
		});

		// Touch command
		commands.put("touch", new Command("touch", "Creates a new empty file") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				if(fileSystem.touch(args)) {
					console.print(valueOf("File touched"));
				} else {
					console.print(valueOf(fsErrorOr("Error: Could not create file")));
				}
			}
		});

		// Rm command
		commands.put("rm", new Command("rm", "Removes a file or directory") {
			@Override
			public void execute(String args) {
				RmArgs parsed = parseRmArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null) {
					console.print(valueOf("Error: Usage: rm [-r] [-f] <path>..."));
					return;
				}

				if(parsed.paths.isEmpty()) {
					if(parsed.force) {
						return;
					}
					console.print(valueOf("Error: No file specified"));
					return;
				}

				boolean deletedAny = false;
				for(String rawPath : parsed.paths) {
					String normalizedPath = fileSystem.normalizePath(rawPath);
					if("/".equals(normalizedPath)) {
						if(!parsed.force) {
							console.print(valueOf("Error: Refusing to remove root '/'"));
						}
						continue;
					}

					if(parsed.recursive) {
						if(deleteRecursive(normalizedPath, parsed.force)) {
							deletedAny = true;
						} else if(!parsed.force) {
							console.print(valueOf(fsErrorOr("Error: Could not delete file")));
						}
						continue;
					}

					if(fileSystem.delete(normalizedPath)) {
						deletedAny = true;
					} else if(!parsed.force) {
						console.print(valueOf(fsErrorOr("Error: Could not delete file")));
					}
				}

				if(deletedAny) {
					console.print(valueOf("File deleted"));
				}
			}
		});

		// Clear command
		commands.put("clear", new Command("clear", "Clears the terminal") {
			@Override
			public void execute(String args) {
				console.clearTextContents();
			}
		});

		// Exit command
		commands.put("exit", new Command("exit", "Exits the terminal") {
			@Override
			public void execute(String args) {
				stop();
			}
		});

		// Reboot command
		commands.put("reboot", new Command("reboot", "Reloads startup script and resets terminal UI") {
			@Override
			public void execute(String args) {
				reboot();
			}
		});

		// Copy command
		commands.put("cp", new Command("cp", "Copies a file") {
			@Override
			public void execute(String args) {
				CpArgs parsed = parseCpArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.source == null || parsed.destination == null) {
					console.print(valueOf("Error: Usage: cp [-r] <source> <destination>"));
					return;
				}

				String source = fileSystem.normalizePath(parsed.source);
				String dest = fileSystem.normalizePath(parsed.destination);

				if(!fileSystem.exists(source)) {
					console.print(valueOf(fsErrorOr("Error: Source file not found")));
					return;
				}

				if(fileSystem.isDir(source)) {
					if(!parsed.recursive) {
						console.print(valueOf("Error: Source is a directory (use -r)"));
						return;
					}

					String destinationRoot = resolveCopyDirectoryDestination(source, dest);
					if(destinationRoot == null || !copyRecursive(source, destinationRoot)) {
						console.print(valueOf(fsErrorOr("Error: Could not copy directory")));
						return;
					}

					console.print(valueOf("File copied"));
					return;
				}

				String content = fileSystem.read(source);
				if(content != null) {
					String fileDestination = resolveCopyFileDestination(source, dest);
					if(fileDestination != null && fileSystem.write(fileDestination, content)) {
						console.print(valueOf("File copied"));
					} else {
						console.print(valueOf(fsErrorOr("Error: Could not write destination file")));
					}
				} else {
					console.print(valueOf(fsErrorOr("Error: Source file not found or is a directory")));
				}
			}
		});

		// Move command
		commands.put("mv", new Command("mv", "Moves or renames a file") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.size() < 2) {
					console.print(valueOf("Error: Usage: mv <source> <destination>"));
					return;
				}

				String source = parts.get(0);
				String dest = parts.get(1);

				String content = fileSystem.read(source);
				if(content != null) {
					if(fileSystem.write(dest, content) && fileSystem.delete(source)) {
						console.print(valueOf("File moved"));
					} else {
						console.print(valueOf(fsErrorOr("Error: Could not move file")));
					}
				} else {
					console.print(valueOf(fsErrorOr("Error: Source file not found or is a directory")));
				}
			}
		});

		// Edit command (simple write)
		commands.put("edit", new Command("edit", "Creates or overwrites a file with text") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.size() < 2) {
					console.print(valueOf("Error: Usage: edit <file> <content>"));
					return;
				}

				String file = parts.get(0);
				String content = joinTokens(parts, 1);

				if(fileSystem.write(file, content)) {
					console.print(valueOf("File written"));
				} else {
					console.print(valueOf(fsErrorOr("Error: Could not write file")));
				}
			}
		});

		// Nano command
		commands.put("nano", new Command("nano", "Opens a file in the editor pane") {
			@Override
			public void execute(String args) {
				String file = args == null ? "" : args.trim();
				if(file.isEmpty()) {
					console.print(valueOf("Error: Usage: nano <file>"));
					return;
				}

				if(!module.openFileInEditor(file)) {
					console.print(valueOf("Error: Could not open editor for file"));
				}
			}
		});

		// Run command (explicitly run a Lua script)
		commands.put("run", new Command("run", "Runs a Lua script") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.isEmpty()) {
					console.print(valueOf("Error: Usage: run <script> [args]"));
					return;
				}

				String scriptPath = resolveScriptPath(parts.get(0));
				if(scriptPath == null) {
					console.print(valueOf("Error: Script file not found"));
					return;
				}

				runScriptAtPath(scriptPath, parts.subList(1, parts.size()));
			}
		});

		// Httpget command
		commands.put("httpget", new Command("httpget", "Fetches HTTP/HTTPS data; optionally save to a file") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.isEmpty()) {
					console.print(valueOf("Error: Usage: httpget <url> [output-file]"));
					return;
				}

				String payload = fetchWebData(parts.get(0));
				if(payload == null) {
					return;
				}

				if(parts.size() > 1) {
					String outputPath = parts.get(1);
					if(fileSystem.write(outputPath, payload)) {
						console.print(valueOf("Saved " + payload.getBytes(StandardCharsets.UTF_8).length + " bytes to " + fileSystem.normalizePath(outputPath)));
					} else {
						console.print(valueOf("Error: Could not write output file"));
					}
					return;
				}

				console.print(valueOf(payload));
			}
		});

		// Runbg command (run script in background)
		commands.put("runbg", new Command("runbg", "Runs a Lua script in background") {
			@Override
			public void execute(String args) {
				List<String> parts = parseCommandTokens(args == null ? "" : args.trim());
				if(parts.isEmpty()) {
					console.print(valueOf("Error: Usage: runbg <script> [args]"));
					return;
				}

				String scriptPath = resolveScriptPath(parts.get(0));
				if(scriptPath == null) {
					console.print(valueOf("Error: Script file not found"));
					return;
				}

				String script = fileSystem.read(scriptPath);
				if(script == null) {
					console.print(valueOf("Error: Could not read script file: " + scriptPath));
					return;
				}

				int jobId = startBackgroundScript(scriptPath, script, parts.subList(1, parts.size()));
				if(jobId < 0) {
					ScriptOverloadMode mode = ScriptOverloadMode.fromConfigValue(ConfigManager.getScriptOverloadMode());
					if(mode == ScriptOverloadMode.HARD_STOP) {
						console.print(valueOf("Error: Background script rejected due to server load"));
					} else {
						console.print(valueOf("Error: Background script queue is full or timed out"));
					}
					return;
				}

				console.print(valueOf("Started background job #" + jobId + " (" + scriptPath + ")"));
			}
		});

		// Jobs command
		commands.put("jobs", new Command("jobs", "Lists background script jobs") {
			@Override
			public void execute(String args) {
				refreshBackgroundJobs();
				if(backgroundJobs.isEmpty()) {
					console.print(valueOf("No background jobs"));
					return;
				}

				for(BackgroundJob job : backgroundJobs.values()) {
					console.print(valueOf("#" + job.getId() + " [" + job.getStatus().name().toLowerCase() + "] " + job.getScriptPath()));
				}
			}
		});

		// Kill command
		commands.put("kill", new Command("kill", "Stops a background job") {
			@Override
			public void execute(String args) {
				KillArgs parsed = parseKillArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.jobId < 1) {
					console.print(valueOf("Error: Usage: kill [-TERM|-KILL|-INT|-HUP|-15|-9|-2|-1] <job-id>"));
					return;
				}

				int jobId = parsed.jobId;

				BackgroundJob job = backgroundJobs.get(jobId);
				if(job == null) {
					console.print(valueOf("Error: job not found"));
					return;
				}

				Future<Boolean> future = job.getFuture();
				if(future != null && !future.isDone()) {
					job.setStatus(JobStatus.CANCELED);
					future.cancel(true);
					if(parsed.signalName == null) {
						console.print(valueOf("Canceled job #" + jobId));
					} else {
						console.print(valueOf("Canceled job #" + jobId + " with signal " + parsed.signalName));
					}
				} else {
					refreshBackgroundJobs();
					console.print(valueOf("Job #" + jobId + " is already " + job.getStatus().name().toLowerCase()));
				}
			}
		});
	}

	public String getTextContents() {
		return console.getTextContents();
	}

	private int parsePositiveLineCount(String raw, int defaultValue) {
		if(raw == null || raw.trim().isEmpty()) {
			return defaultValue;
		}

		try {
			return Integer.parseInt(raw.trim());
		} catch(NumberFormatException ignored) {
			return -1;
		}
	}

	private HeadTailArgs parseHeadTailArgs(List<String> tokens, int defaultLineCount) {
		if(tokens == null || tokens.isEmpty()) {
			console.print(valueOf("Error: missing file operand"));
			return null;
		}

		String file = null;
		int lineCount = defaultLineCount;
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if(token == null || token.isEmpty()) {
				continue;
			}

			if("-n".equals(token)) {
				if(i + 1 >= tokens.size()) {
					console.print(valueOf("Error: missing line count for -n"));
					return null;
				}
				++i;
				lineCount = parsePositiveLineCount(tokens.get(i), defaultLineCount);
				if(lineCount < 1) {
					console.print(valueOf("Error: lines must be a positive integer"));
					return null;
				}
				continue;
			}

			if(token.startsWith("-n") && token.length() > 2) {
				lineCount = parsePositiveLineCount(token.substring(2), defaultLineCount);
				if(lineCount < 1) {
					console.print(valueOf("Error: lines must be a positive integer"));
					return null;
				}
				continue;
			}

			if(file == null) {
				file = token;
			} else {
				console.print(valueOf("Error: too many arguments"));
				return null;
			}
		}

		if(file == null || file.trim().isEmpty()) {
			console.print(valueOf("Error: missing file operand"));
			return null;
		}

		return new HeadTailArgs(file, lineCount);
	}

	private boolean printLsDirectory(String directoryPath, LsArgs options, boolean printHeader) {
		List<String> children = fileSystem.list(directoryPath);
		if(children == null) {
			return false;
		}

		if(printHeader) {
			console.print(valueOf(directoryPath + ":"));
		}

		List<String> displayNames = new ArrayList<>();
		List<String> recursiveChildren = new ArrayList<>();

		for(String child : children) {
			if(!options.showAll && child.startsWith(".")) {
				continue;
			}
			displayNames.add(child);
		}
		Collections.sort(displayNames);

		for(String child : displayNames) {
			String childPath = fileSystem.normalizePath(directoryPath + "/" + child);
			printLsEntry(childPath, child, options);
			if(options.recursive && fileSystem.isDir(childPath)) {
				recursiveChildren.add(childPath);
			}
		}

		if(options.recursive) {
			for(String childDir : recursiveChildren) {
				console.print(valueOf(""));
				if(!printLsDirectory(childDir, options, true)) {
					return false;
				}
			}
		}

		if(displayNames.isEmpty() && !options.recursive) {
			console.print(valueOf("Directory is empty"));
		}

		return true;
	}

	private void printLsEntry(String normalizedPath, String displayName, LsArgs options) {
		boolean isDirectory = fileSystem.isDir(normalizedPath);
		String name = isDirectory ? displayName + "/" : displayName;
		if(!options.longFormat) {
			console.print(valueOf(name));
			return;
		}

		long size = 0L;
		VirtualFileStats stats = getVirtualFileStats(normalizedPath);
		if(stats != null) {
			size = stats.size;
		}

		String type = isDirectory ? "d" : "-";
		console.print(valueOf(type + " " + size + " " + name));
	}

	private VirtualFileStats getVirtualFileStats(String normalizedPath) {
		if(normalizedPath == null || normalizedPath.isEmpty()) {
			return null;
		}

		luamade.lua.fs.VirtualFile file = fileSystem.getFile(normalizedPath);
		if(file == null || file.getInternalFile() == null) {
			return null;
		}

		return new VirtualFileStats(file.getInternalFile().length());
	}

	private String baseName(String normalizedPath) {
		if(normalizedPath == null || normalizedPath.isEmpty() || "/".equals(normalizedPath)) {
			return "/";
		}

		int index = normalizedPath.lastIndexOf('/');
		if(index < 0 || index >= normalizedPath.length() - 1) {
			return normalizedPath;
		}
		return normalizedPath.substring(index + 1);
	}

	private boolean deleteRecursive(String normalizedPath, boolean force) {
		if(!fileSystem.exists(normalizedPath)) {
			return force;
		}

		if(fileSystem.isDir(normalizedPath)) {
			List<String> children = fileSystem.list(normalizedPath);
			for(String child : children) {
				String childPath = fileSystem.normalizePath(normalizedPath + "/" + child);
				if(!deleteRecursive(childPath, force)) {
					return false;
				}
			}
		}

		return fileSystem.delete(normalizedPath) || force;
	}

	private String resolveCopyFileDestination(String sourceFile, String destination) {
		if(fileSystem.exists(destination) && fileSystem.isDir(destination)) {
			return fileSystem.normalizePath(destination + "/" + baseName(sourceFile));
		}
		return destination;
	}

	private String resolveCopyDirectoryDestination(String sourceDirectory, String destination) {
		if(fileSystem.exists(destination)) {
			if(!fileSystem.isDir(destination)) {
				return null;
			}
			return fileSystem.normalizePath(destination + "/" + baseName(sourceDirectory));
		}

		return destination;
	}

	private boolean copyRecursive(String sourcePath, String destinationPath) {
		if(fileSystem.isDir(sourcePath)) {
			if(fileSystem.exists(destinationPath)) {
				if(!fileSystem.isDir(destinationPath)) {
					return false;
				}
			} else if(!fileSystem.makeDir(destinationPath)) {
				return false;
			}

			List<String> children = fileSystem.list(sourcePath);
			for(String child : children) {
				String childSourcePath = fileSystem.normalizePath(sourcePath + "/" + child);
				String childDestinationPath = fileSystem.normalizePath(destinationPath + "/" + child);
				if(!copyRecursive(childSourcePath, childDestinationPath)) {
					return false;
				}
			}
			return true;
		}

		String content = fileSystem.read(sourcePath);
		return content != null && fileSystem.write(destinationPath, content);
	}

	private LsArgs parseLsArgs(List<String> tokens) {
		LsArgs parsed = new LsArgs();
		if(tokens == null) {
			return parsed;
		}

		for(String token : tokens) {
			if(token.startsWith("-") && token.length() > 1) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					switch(flag) {
						case 'a':
							parsed.showAll = true;
							break;
						case 'l':
							parsed.longFormat = true;
							break;
						case 'R':
							parsed.recursive = true;
							break;
						default:
							return null;
					}
				}
				continue;
			}

			if(parsed.path != null) {
				return null;
			}
			parsed.path = token;
		}

		return parsed;
	}

	private RmArgs parseRmArgs(List<String> tokens) {
		RmArgs parsed = new RmArgs();
		if(tokens == null) {
			return parsed;
		}

		for(String token : tokens) {
			if(token.startsWith("-") && token.length() > 1) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					switch(flag) {
						case 'r':
						case 'R':
							parsed.recursive = true;
							break;
						case 'f':
							parsed.force = true;
							break;
						default:
							return null;
					}
				}
				continue;
			}

			parsed.paths.add(token);
		}

		return parsed;
	}

	private CpArgs parseCpArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		CpArgs parsed = new CpArgs();
		List<String> operands = new ArrayList<>();
		for(String token : tokens) {
			if(token.startsWith("-") && token.length() > 1 && operands.isEmpty()) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					if(flag == 'r' || flag == 'R') {
						parsed.recursive = true;
					} else {
						return null;
					}
				}
				continue;
			}

			operands.add(token);
		}

		if(operands.size() != 2) {
			return null;
		}

		parsed.source = operands.get(0);
		parsed.destination = operands.get(1);
		return parsed;
	}

	private FindArgs parseFindArgs(List<String> tokens) {
		FindArgs parsed = new FindArgs();
		if(tokens == null) {
			return parsed;
		}

		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if(token == null || token.isEmpty()) {
				continue;
			}

			if("-name".equals(token)) {
				if(i + 1 >= tokens.size()) {
					return null;
				}
				++i;
				parsed.nameGlob = tokens.get(i);
				continue;
			}

			if("-type".equals(token)) {
				if(i + 1 >= tokens.size()) {
					return null;
				}
				++i;
				String kind = tokens.get(i);
				if(!"f".equals(kind) && !"d".equals(kind)) {
					return null;
				}
				parsed.typeFilter = kind;
				continue;
			}

			if("-maxdepth".equals(token)) {
				if(i + 1 >= tokens.size()) {
					return null;
				}
				try {
					++i;
					parsed.maxDepth = Integer.parseInt(tokens.get(i));
				} catch(NumberFormatException numberFormatException) {
					return null;
				}
				if(parsed.maxDepth < 0) {
					return null;
				}
				continue;
			}

			if(token.startsWith("-")) {
				return null;
			}

			if(parsed.path != null) {
				return null;
			}
			parsed.path = token;
		}

		return parsed;
	}

	private boolean walkFind(String path, FindArgs filters, int depth) {
		if(path == null || filters == null) {
			return false;
		}

		boolean isDirectory = fileSystem.isDir(path);
		if(matchesFindFilters(path, isDirectory, filters)) {
			console.print(valueOf(path));
		}

		if(!isDirectory) {
			return true;
		}
		if(filters.maxDepth >= 0 && depth >= filters.maxDepth) {
			return true;
		}

		List<String> children = fileSystem.list(path);
		if(children == null) {
			return false;
		}
		Collections.sort(children);

		for(String child : children) {
			String childPath = fileSystem.normalizePath(path + "/" + child);
			if(!walkFind(childPath, filters, depth + 1)) {
				return false;
			}
		}

		return true;
	}

	private boolean matchesFindFilters(String normalizedPath, boolean isDirectory, FindArgs filters) {
		if(filters == null) {
			return true;
		}

		if(filters.typeFilter != null) {
			if("d".equals(filters.typeFilter) && !isDirectory) {
				return false;
			}
			if("f".equals(filters.typeFilter) && isDirectory) {
				return false;
			}
		}

		if(filters.nameGlob != null) {
			String name = baseName(normalizedPath);
			return globMatches(name, filters.nameGlob);
		}

		return true;
	}

	private boolean globMatches(String text, String glob) {
		if(glob == null || glob.isEmpty()) {
			return true;
		}

		StringBuilder regex = new StringBuilder();
		regex.append('^');
		for(int i = 0; i < glob.length(); i++) {
			char c = glob.charAt(i);
			switch(c) {
				case '*':
					regex.append(".*");
					break;
				case '?':
					regex.append('.');
					break;
				case '.':
				case '\\':
				case '+':
				case '(':
				case ')':
				case '[':
				case ']':
				case '{':
				case '}':
				case '^':
				case '$':
				case '|':
					regex.append('\\').append(c);
					break;
				default:
					regex.append(c);
			}
		}
		regex.append('$');

		return Pattern.matches(regex.toString(), text == null ? "" : text);
	}

	private GrepArgs parseGrepArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		GrepArgs parsed = new GrepArgs();
		List<String> operands = new ArrayList<>();
		for(String token : tokens) {
			if(token.startsWith("-") && token.length() > 1 && operands.isEmpty()) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					switch(flag) {
						case 'n':
							parsed.showLineNumbers = true;
							break;
						case 'i':
							parsed.ignoreCase = true;
							break;
						case 'r':
						case 'R':
							parsed.recursive = true;
							break;
						default:
							return null;
					}
				}
				continue;
			}

			operands.add(token);
		}

		if(operands.size() != 2) {
			return null;
		}

		parsed.pattern = operands.get(0);
		parsed.path = operands.get(1);
		return parsed;
	}

	private WhichArgs parseWhichArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		WhichArgs parsed = new WhichArgs();
		for(String token : tokens) {
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(token.startsWith("-") && parsed.target == null) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					if(flag == 'a') {
						parsed.showAll = true;
					} else {
						return null;
					}
				}
				continue;
			}

			if(parsed.target != null) {
				return null;
			}
			parsed.target = token;
		}

		return parsed;
	}

	private List<String> findWhichMatches(String target, boolean showAll) {
		LinkedHashSet<String> matches = new LinkedHashSet<>();

		if(commands.containsKey(target)) {
			matches.add(target + ": shell built-in");
			if(!showAll) {
				return new ArrayList<>(matches);
			}
		}

		for(String candidate : resolveWhichPathCandidates(target)) {
			if(candidate == null || candidate.isEmpty()) {
				continue;
			}
			if(fileSystem.exists(candidate) && !fileSystem.isDir(candidate)) {
				matches.add(candidate);
				if(!showAll) {
					break;
				}
			}
		}

		return new ArrayList<>(matches);
	}

	private List<String> resolveWhichPathCandidates(String target) {
		LinkedHashSet<String> candidates = new LinkedHashSet<>();
		if(target == null || target.trim().isEmpty()) {
			return new ArrayList<>(candidates);
		}

		String trimmed = target.trim();
		candidates.add(fileSystem.normalizePath(trimmed));

		if(!trimmed.endsWith(".lua")) {
			candidates.add(fileSystem.normalizePath(trimmed + ".lua"));
		}

		if(!trimmed.startsWith("/")) {
			candidates.add(fileSystem.normalizePath("/bin/" + trimmed));
			if(!trimmed.endsWith(".lua")) {
				candidates.add(fileSystem.normalizePath("/bin/" + trimmed + ".lua"));
			}
		}

		return new ArrayList<>(candidates);
	}

	private PwdArgs parsePwdArgs(List<String> tokens) {
		PwdArgs parsed = new PwdArgs();
		if(tokens == null) {
			return parsed;
		}

		for(String token : tokens) {
			if("-L".equals(token)) {
				parsed.physical = false;
				continue;
			}
			if("-P".equals(token)) {
				parsed.physical = true;
				continue;
			}
			return null;
		}

		return parsed;
	}

	private CatArgs parseCatArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		CatArgs parsed = new CatArgs();
		for(String token : tokens) {
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(token.startsWith("-") && token.length() > 1 && parsed.filePaths.isEmpty()) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					if(flag == 'n') {
						parsed.showLineNumbers = true;
					} else if(flag == 'A') {
						parsed.showAllChars = true;
					} else {
						return null;
					}
				}
				continue;
			}

			parsed.filePaths.add(token);
		}

		return parsed;
	}

	private WcArgs parseWcArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		WcArgs parsed = new WcArgs();
		for(String token : tokens) {
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(token.startsWith("-") && token.length() > 1 && parsed.filePaths.isEmpty()) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					switch(flag) {
						case 'l':
							parsed.showLines = true;
							break;
						case 'w':
							parsed.showWords = true;
							break;
						case 'c':
							parsed.showBytes = true;
							break;
						default:
							return null;
					}
				}
				continue;
			}

			parsed.filePaths.add(token);
		}

		if(!parsed.showLines && !parsed.showWords && !parsed.showBytes) {
			parsed.showLines = true;
			parsed.showWords = true;
			parsed.showBytes = true;
		}

		return parsed;
	}

	private MkdirArgs parseMkdirArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		MkdirArgs parsed = new MkdirArgs();
		for(String token : tokens) {
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(token.startsWith("-") && parsed.paths.isEmpty()) {
				for(int i = 1; i < token.length(); i++) {
					char flag = token.charAt(i);
					if(flag == 'p') {
						parsed.parents = true;
					} else {
						return null;
					}
				}
				continue;
			}

			parsed.paths.add(token);
		}

		return parsed;
	}

	private KillArgs parseKillArgs(List<String> tokens) {
		if(tokens == null || tokens.isEmpty()) {
			return null;
		}

		KillArgs parsed = new KillArgs();
		int index = 0;

		String first = tokens.get(0);
		if(first != null && first.startsWith("-") && first.length() > 1) {
			String signalToken = first.substring(1).toUpperCase(Locale.ROOT);
			String normalizedSignal = normalizeKillSignal(signalToken);
			if(normalizedSignal == null) {
				return null;
			}
			parsed.signalName = normalizedSignal;
			index = 1;
		}

		if(index >= tokens.size()) {
			return null;
		}

		if(tokens.size() - index != 1) {
			return null;
		}

		try {
			parsed.jobId = Integer.parseInt(tokens.get(index));
		} catch(NumberFormatException numberFormatException) {
			return null;
		}

		return parsed;
	}

	private String normalizeKillSignal(String signalToken) {
		if(signalToken == null || signalToken.isEmpty()) {
			return null;
		}

		if(signalToken.startsWith("SIG") && signalToken.length() > 3) {
			signalToken = signalToken.substring(3);
		}

		switch(signalToken) {
			case "9":
			case "KILL":
				return "KILL";
			case "15":
			case "TERM":
				return "TERM";
			case "2":
			case "INT":
				return "INT";
			case "1":
			case "HUP":
				return "HUP";
			default:
				return null;
		}
	}

	private String formatWcOutput(WcArgs parsed, long lineCount, long wordCount, long byteCount, String label) {
		StringBuilder output = new StringBuilder();
		if(parsed.showLines) {
			output.append(lineCount).append(' ');
		}
		if(parsed.showWords) {
			output.append(wordCount).append(' ');
		}
		if(parsed.showBytes) {
			output.append(byteCount).append(' ');
		}
		output.append(label == null ? "" : label);
		return output.toString().trim();
	}

	private String renderCatAll(String line) {
		if(line == null) {
			return "$";
		}

		StringBuilder out = new StringBuilder();
		for(int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);
			if(c == '\t') {
				out.append("^I");
				continue;
			}
			if(c < 32 || c == 127) {
				out.append('^').append((char) ((c == 127) ? '?' : (c + 64)));
				continue;
			}
			out.append(c);
		}
		out.append('$');
		return out.toString();
	}

	private int countWords(String text) {
		if(text == null || text.trim().isEmpty()) {
			return 0;
		}
		return text.trim().split("\\s+").length;
	}

	private StatArgs parseStatArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		StatArgs parsed = new StatArgs();
		for(String token : tokens) {
			if(token == null || token.trim().isEmpty()) {
				continue;
			}
			parsed.paths.add(token);
		}
		return parsed;
	}

	private TreeArgs parseTreeArgs(List<String> tokens) {
		TreeArgs parsed = new TreeArgs();
		if(tokens == null) {
			return parsed;
		}

		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(token.startsWith("-") && token.length() > 1) {
				if("-L".equals(token)) {
					if(i + 1 >= tokens.size()) {
						return null;
					}
					try {
						++i;
						parsed.maxDepth = Integer.parseInt(tokens.get(i));
					} catch(NumberFormatException numberFormatException) {
						return null;
					}
					if(parsed.maxDepth < 0) {
						return null;
					}
					continue;
				}

				for(int flagIndex = 1; flagIndex < token.length(); flagIndex++) {
					char flag = token.charAt(flagIndex);
					if(flag == 'a') {
						parsed.showAll = true;
					} else {
						return null;
					}
				}
				continue;
			}

			if(parsed.path != null) {
				return null;
			}
			parsed.path = token;
		}

		return parsed;
	}

	private boolean printStatEntry(String normalizedPath) {
		if(!fileSystem.exists(normalizedPath)) {
			return false;
		}

		boolean isDirectory = fileSystem.isDir(normalizedPath);
		luamade.lua.fs.VirtualFile virtualFile = fileSystem.getFile(normalizedPath);
		if(virtualFile == null || virtualFile.getInternalFile() == null) {
			return false;
		}

		File internal = virtualFile.getInternalFile();
		long size = internal.length();
		long modified = internal.lastModified();

		console.print(valueOf("Path: " + normalizedPath));
		console.print(valueOf("Type: " + (isDirectory ? "directory" : "file")));
		console.print(valueOf("Size: " + size + " bytes"));
		console.print(valueOf("Modified: " + new Date(modified)));
		console.print(valueOf("Permissions: " + fileSystem.getPermissions(normalizedPath)));
		return true;
	}

	private boolean walkTree(String normalizedPath, String prefix, boolean isLast, int depth, TreeArgs args) {
		if(!fileSystem.isDir(normalizedPath)) {
			return true;
		}

		if(args.maxDepth >= 0 && depth >= args.maxDepth) {
			return true;
		}

		List<String> children = fileSystem.list(normalizedPath);
		if(children == null) {
			return false;
		}

		List<String> filtered = new ArrayList<>();
		for(String child : children) {
			if(!args.showAll && child.startsWith(".")) {
				continue;
			}
			filtered.add(child);
		}
		Collections.sort(filtered);

		for(int i = 0; i < filtered.size(); i++) {
			String child = filtered.get(i);
			boolean childIsLast = (i == filtered.size() - 1);
			String branch = childIsLast ? "`-- " : "|-- ";
			String childPath = fileSystem.normalizePath(normalizedPath + "/" + child);

			String displayName = child;
			if(fileSystem.isDir(childPath)) {
				displayName += "/";
			}
			console.print(valueOf(prefix + branch + displayName));

			String childPrefix = prefix + (childIsLast ? "    " : "|   ");
			if(!walkTree(childPath, childPrefix, childIsLast, depth + 1, args)) {
				return false;
			}
		}

		return true;
	}

	private boolean grepPath(String normalizedPath, GrepArgs args) {
		if(fileSystem.isDir(normalizedPath)) {
			if(!args.recursive) {
				return false;
			}

			List<String> children = fileSystem.list(normalizedPath);
			Collections.sort(children);
			for(String child : children) {
				String childPath = fileSystem.normalizePath(normalizedPath + "/" + child);
				if(!grepPath(childPath, args)) {
					return false;
				}
			}
			return true;
		}

		return grepFile(normalizedPath, args);
	}

	private boolean grepFile(String normalizedFilePath, GrepArgs args) {
		String content = fileSystem.read(normalizedFilePath);
		if(content == null) {
			return false;
		}

		String[] lines = content.split("\\n", -1);
		for(int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if(!lineMatches(line, args.pattern, args.ignoreCase)) {
				continue;
			}

			if(args.showLineNumbers) {
				console.print(valueOf(normalizedFilePath + ":" + (i + 1) + ":" + line));
			} else {
				console.print(valueOf(normalizedFilePath + ":" + line));
			}
		}

		return true;
	}

	private boolean lineMatches(String line, String pattern, boolean ignoreCase) {
		if(pattern == null || pattern.isEmpty()) {
			return true;
		}
		if(line == null) {
			return false;
		}

		if(ignoreCase) {
			return line.toLowerCase(Locale.ROOT).contains(pattern.toLowerCase(Locale.ROOT));
		}

		return line.contains(pattern);
	}

	private static final class HeadTailArgs {
		private final String filePath;
		private final int lineCount;

		private HeadTailArgs(String filePath, int lineCount) {
			this.filePath = filePath;
			this.lineCount = lineCount;
		}
	}

	private static final class LsArgs {
		private boolean showAll;
		private boolean longFormat;
		private boolean recursive;
		private String path;
	}

	private static final class RmArgs {
		private final List<String> paths = new ArrayList<>();
		private boolean recursive;
		private boolean force;
	}

	private static final class CpArgs {
		private boolean recursive;
		private String source;
		private String destination;
	}

	private static final class FindArgs {
		private String path;
		private String nameGlob;
		private String typeFilter;
		private int maxDepth = -1;
	}

	private static final class GrepArgs {
		private boolean showLineNumbers;
		private boolean ignoreCase;
		private boolean recursive;
		private String pattern;
		private String path;
	}

	private static final class WhichArgs {
		private boolean showAll;
		private String target;
	}

	private static final class PwdArgs {
		private boolean physical;
	}

	private static final class CatArgs {
		private boolean showLineNumbers;
		private final List<String> filePaths = new ArrayList<>();
		private boolean showAllChars;
	}

	private static final class WcArgs {
		private boolean showLines;
		private boolean showWords;
		private boolean showBytes;
		private final List<String> filePaths = new ArrayList<>();
	}

	private static final class MkdirArgs {
		private final List<String> paths = new ArrayList<>();
		private boolean parents;
	}

	private static final class KillArgs {
		private int jobId;
		private String signalName;
	}

	private static final class StatArgs {
		private final List<String> paths = new ArrayList<>();
	}

	private static final class TreeArgs {
		private boolean showAll;
		private int maxDepth = -1;
		private String path;
	}

	private static final class VirtualFileStats {
		private final long size;

		private VirtualFileStats(long size) {
			this.size = Math.max(size, 0L);
		}
	}

	private String fetchWebData(String rawUrl) {
		if(!ConfigManager.isWebFetchEnabled()) {
			console.print(valueOf("Error: Web fetch is disabled by server config"));
			return null;
		}

		URL url;
		try {
			url = new URL(rawUrl);
		} catch(MalformedURLException malformedURLException) {
			console.print(valueOf("Error: Invalid URL"));
			return null;
		}

		String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
		if(!"http".equals(protocol) && !"https".equals(protocol)) {
			console.print(valueOf("Error: Only http:// and https:// URLs are allowed"));
			return null;
		}

		String host = url.getHost() == null ? "" : url.getHost().toLowerCase(Locale.ROOT);
		if(host.isEmpty()) {
			console.print(valueOf("Error: URL must include a hostname"));
			return null;
		}

		if(ConfigManager.isWebFetchTrustedDomainsOnly() && !ConfigManager.isTrustedWebDomain(host)) {
			console.print(valueOf("Error: Domain is not in trusted allowlist"));
			return null;
		}

		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(ConfigManager.getWebFetchTimeoutMs());
			connection.setReadTimeout(ConfigManager.getWebFetchTimeoutMs());
			connection.setRequestProperty("User-Agent", "LuaMade/1.0");

			int status = connection.getResponseCode();
			if(status < 200 || status >= 300) {
				console.print(valueOf("Error: HTTP status " + status));
				return null;
			}

			InputStream input = connection.getInputStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			int maxBytes = ConfigManager.getWebFetchMaxBytes();
			int total = 0;

			while(true) {
				int read = input.read(buffer);
				if(read < 0) {
					break;
				}

				total += read;
				if(total > maxBytes) {
					console.print(valueOf("Error: Response exceeded web_fetch_max_bytes limit (" + maxBytes + ")"));
					return null;
				}

				output.write(buffer, 0, read);
			}

			return output.toString(StandardCharsets.UTF_8.name());
		} catch(IOException ioException) {
			console.print(valueOf("Error fetching URL: " + ioException.getMessage()));
			return null;
		} finally {
			if(connection != null) {
				connection.disconnect();
			}
		}
	}

	/**
	 * Command interface
	 */
	private abstract static class Command {
		private final String name;
		private final String description;

		protected Command(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public abstract void execute(String args);
	}

	/**
	 * Lua command implementation
	 */
	private static class LuaCommand extends Command {
		private final LuaValue callback;

		public LuaCommand(String name, LuaValue callback) {
			super(name, "User-defined command");
			this.callback = callback;
		}

		@Override
		public void execute(String args) {
			callback.call(valueOf(args));
		}
	}

	private enum ScriptOverloadMode {
		HARD_STOP,
		STALL,
		HYBRID;

		private static ScriptOverloadMode fromConfigValue(int value) {
			switch(value) {
				case 0:
					return HARD_STOP;
				case 1:
					return STALL;
				case 2:
				default:
					return HYBRID;
			}
		}
	}

	private enum JobStatus {
		RUNNING,
		COMPLETED,
		FAILED,
		TIMED_OUT,
		CANCELED
	}

	private static final class BackgroundJob {
		private final int id;
		private final String scriptPath;
		private volatile JobStatus status = JobStatus.RUNNING;
		private volatile Future<Boolean> future;

		private BackgroundJob(int id, String scriptPath) {
			this.id = id;
			this.scriptPath = scriptPath;
		}

		private int getId() {
			return id;
		}

		private String getScriptPath() {
			return scriptPath;
		}

		private JobStatus getStatus() {
			return status;
		}

		private void setStatus(JobStatus status) {
			this.status = status;
		}

		private Future<Boolean> getFuture() {
			return future;
		}

		private void setFuture(Future<Boolean> future) {
			this.future = future;
		}
	}

	private static final class ScriptThreadFactory implements ThreadFactory {
		private final String namePrefix;
		private final AtomicInteger sequence = new AtomicInteger(1);

		private ScriptThreadFactory(String namePrefix) {
			this.namePrefix = namePrefix;
		}

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, namePrefix + "-" + sequence.getAndIncrement());
			thread.setDaemon(true);
			return thread;
		}
	}
}