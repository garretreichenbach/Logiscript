package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.lua.peripheral.PeripheralsApi;
import luamade.lua.util.UtilApi;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.BaseLib;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JseMathLib;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

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

		// Add to history
		if(!submittedInput.trim().isEmpty()) {
			history.add(submittedInput);
			historyIndex = history.size();
		}

		// Process the command
		List<String> tokens = parseCommandTokens(submittedInput.trim());
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

	/**
	 * Creates a sandboxed Lua globals environment for script execution
	 */
	private Globals createSandboxedGlobals() {
		Globals globals = new Globals();
		
		// Load only safe libraries
		globals.load(new BaseLib());
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
		globals.set("print", console.get("print"));
		globals.set("fs", fileSystem);
		globals.set("term", this);
		// Backward compatibility for older scripts that used `terminal`.
		globals.set("terminal", this);
		globals.set("net", module.getNetworkInterface());
		globals.set("peripheral", new PeripheralsApi(module));
		globals.set("gfx", new TextGraphicsApi(console));
		globals.set("shell", createShellCompatibilityApi());

		LuaTable utilLibrary = loadBuiltinLibrary(globals, "scripts/lib/util.lua", "util");
		LuaTable vectorLibrary = loadBuiltinLibrary(globals, "scripts/lib/vector.lua", "vector");

		UtilApi nativeUtil = new UtilApi();
		if(utilLibrary != null) {
			utilLibrary.set("now", nativeUtil.get("now"));
			utilLibrary.set("sleep", nativeUtil.get("sleep"));
		} else {
			globals.set("util", nativeUtil);
		}

		if(vectorLibrary == null) {
			globals.set("vector", new LuaTable());
		}
		
		return globals;
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

		// Which command
		commands.put("which", new Command("which", "Shows where a command or file resolves") {
			@Override
			public void execute(String args) {
				String target = args == null ? "" : args.trim();
				if(target.isEmpty()) {
					console.print(valueOf("Error: Usage: which <command-or-path>"));
					return;
				}

				if(commands.containsKey(target)) {
					console.print(valueOf(target + ": shell built-in"));
					return;
				}

				String resolvedPath = fileSystem.normalizePath(target);
				if(fileSystem.exists(resolvedPath)) {
					console.print(valueOf(resolvedPath));
				} else {
					console.print(valueOf(target + " not found"));
				}
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
					console.print(valueOf("Error: Usage: head <file> [lines]"));
					return;
				}

				String[] parts = trimmed.split("\\s+");
				String file = parts[0];
				int lineCount = parsePositiveLineCount(parts.length > 1 ? parts[1] : null, 10);
				if(lineCount < 1) {
					console.print(valueOf("Error: lines must be a positive integer"));
					return;
				}

				String content = fileSystem.read(file);
				if(content == null) {
					console.print(valueOf("Error: File not found or is a directory"));
					return;
				}

				String[] lines = content.split("\\n", -1);
				int max = Math.min(lineCount, lines.length);
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
					console.print(valueOf("Error: Usage: tail <file> [lines]"));
					return;
				}

				String[] parts = trimmed.split("\\s+");
				String file = parts[0];
				int lineCount = parsePositiveLineCount(parts.length > 1 ? parts[1] : null, 10);
				if(lineCount < 1) {
					console.print(valueOf("Error: lines must be a positive integer"));
					return;
				}

				String content = fileSystem.read(file);
				if(content == null) {
					console.print(valueOf("Error: File not found or is a directory"));
					return;
				}

				String[] lines = content.split("\\n", -1);
				int start = Math.max(0, lines.length - lineCount);
				for(int i = start; i < lines.length; i++) {
					console.print(valueOf(lines[i]));
				}
			}
		});

		// Wc command
		commands.put("wc", new Command("wc", "Prints line, word, and byte counts") {
			@Override
			public void execute(String args) {
				String file = args == null ? "" : args.trim();
				if(file.isEmpty()) {
					console.print(valueOf("Error: Usage: wc <file>"));
					return;
				}

				String content = fileSystem.read(file);
				if(content == null) {
					console.print(valueOf("Error: File not found or is a directory"));
					return;
				}

				int lineCount = content.isEmpty() ? 0 : content.split("\\n", -1).length;
				int wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
				int byteCount = content.getBytes(StandardCharsets.UTF_8).length;
				console.print(valueOf(lineCount + " " + wordCount + " " + byteCount + " " + fileSystem.normalizePath(file)));
			}
		});

		// Echo command
		commands.put("echo", new Command("echo", "Displays a message") {
			@Override
			public void execute(String args) {
				console.print(valueOf(args));
			}
		});

		// Ls command
		commands.put("ls", new Command("ls", "Lists files in a directory") {
			@Override
			public void execute(String args) {
				List<String> files = fileSystem.list(args);
				if(files.isEmpty()) {
					console.print(valueOf("Directory is empty"));
				} else {
					for(String file : files) {
						if(fileSystem.isDir(fileSystem.normalizePath(args + "/" + file))) {
							console.print(valueOf(file + "/"));
						} else {
							console.print(valueOf(file));
						}
					}
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
					console.print(valueOf("Error: Directory not found"));
				}
			}
		});

		// Pwd command
		commands.put("pwd", new Command("pwd", "Prints the current working directory") {
			@Override
			public void execute(String args) {
				console.print(valueOf(fileSystem.getCurrentDir()));
			}
		});

		// Mkdir command
		commands.put("mkdir", new Command("mkdir", "Creates a new directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No directory name specified"));
					return;
				}

				if(fileSystem.makeDir(args)) {
					console.print(valueOf("Directory created"));
				} else {
					console.print(valueOf("Error: Could not create directory"));
				}
			}
		});

		// Cat command
		commands.put("cat", new Command("cat", "Displays the contents of a file") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				String content = fileSystem.read(args);
				if(content != null) {
					console.print(valueOf(content));
				} else {
					console.print(valueOf("Error: File not found or is a directory"));
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

				if(fileSystem.write(args, "")) {
					console.print(valueOf("File created"));
				} else {
					console.print(valueOf("Error: Could not create file"));
				}
			}
		});

		// Rm command
		commands.put("rm", new Command("rm", "Removes a file or directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				if(fileSystem.delete(args)) {
					console.print(valueOf("File deleted"));
				} else {
					console.print(valueOf("Error: Could not delete file"));
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
				String[] parts = args.split("\\s+");
				if(parts.length < 2) {
					console.print(valueOf("Error: Usage: cp <source> <destination>"));
					return;
				}

				String source = parts[0];
				String dest = parts[1];

				String content = fileSystem.read(source);
				if(content != null) {
					if(fileSystem.write(dest, content)) {
						console.print(valueOf("File copied"));
					} else {
						console.print(valueOf("Error: Could not write destination file"));
					}
				} else {
					console.print(valueOf("Error: Source file not found or is a directory"));
				}
			}
		});

		// Move command
		commands.put("mv", new Command("mv", "Moves or renames a file") {
			@Override
			public void execute(String args) {
				String[] parts = args.split("\\s+");
				if(parts.length < 2) {
					console.print(valueOf("Error: Usage: mv <source> <destination>"));
					return;
				}

				String source = parts[0];
				String dest = parts[1];

				String content = fileSystem.read(source);
				if(content != null) {
					if(fileSystem.write(dest, content) && fileSystem.delete(source)) {
						console.print(valueOf("File moved"));
					} else {
						console.print(valueOf("Error: Could not move file"));
					}
				} else {
					console.print(valueOf("Error: Source file not found or is a directory"));
				}
			}
		});

		// Edit command (simple write)
		commands.put("edit", new Command("edit", "Creates or overwrites a file with text") {
			@Override
			public void execute(String args) {
				String[] parts = args.split("\\s+", 2);
				if(parts.length < 2) {
					console.print(valueOf("Error: Usage: edit <file> <content>"));
					return;
				}

				String file = parts[0];
				String content = parts[1];

				if(fileSystem.write(file, content)) {
					console.print(valueOf("File written"));
				} else {
					console.print(valueOf("Error: Could not write file"));
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
				String raw = args == null ? "" : args.trim();
				if(raw.isEmpty()) {
					console.print(valueOf("Error: Usage: kill <job-id>"));
					return;
				}

				int jobId;
				try {
					jobId = Integer.parseInt(raw);
				} catch(NumberFormatException exception) {
					console.print(valueOf("Error: job-id must be an integer"));
					return;
				}

				BackgroundJob job = backgroundJobs.get(jobId);
				if(job == null) {
					console.print(valueOf("Error: job not found"));
					return;
				}

				Future<Boolean> future = job.getFuture();
				if(future != null && !future.isDone()) {
					job.setStatus(JobStatus.CANCELED);
					future.cancel(true);
					console.print(valueOf("Canceled job #" + jobId));
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