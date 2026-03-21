package luamade.lua.terminal;

import luamade.LuaMade;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Implements a Unix-like terminal/shell interface for computers in the game.
 * This allows scripts to create a command-line interface for interacting with the computer.
 */
public class Terminal extends LuaMadeUserdata {
	private static final String STARTUP_SCRIPT_PATH = "/etc/startup.lua";
	private static final String DEFAULT_PROMPT_TEMPLATE = "{name}:{dir} $ ";
	private static final Pattern SAFE_REQUIRE_MODULE_PATTERN = Pattern.compile("[a-z0-9_]+(?:\\.[a-z0-9_]+)*");
	private static final List<String> REQUIRE_LIBRARY_ROOTS = Arrays.asList("/lib", "/etc/lib");
	private static final int MAX_LOADER_SOURCE_CHARS = 262_144;
	private static final String CLIPBOARD_BUNDLE_HEADER = "LOGISCRIPT_CLIPBOARD_V1";
	private static final int CLIPBOARD_IMPORT_MAX_FILES = 128;
	private static final int CLIPBOARD_IMPORT_MAX_FILE_CHARS = 1_000_000;
	private static final int CLIPBOARD_IMPORT_MAX_TOTAL_CHARS = 8_000_000;
	private static final LuaValue REQUIRE_LOADING_SENTINEL = valueOf("__luamade_require_loading__");

	private final ComputerModule module;
	private final Console console;
	private final FileSystem fileSystem;
	private final PackageManagerService packageManagerService;
	private final Map<String, Command> commands = new ConcurrentHashMap<>();
	private final Map<Integer, BackgroundJob> backgroundJobs = new ConcurrentHashMap<>();
	private final ExecutorService scriptExecutor;
	private final ThreadLocal<ScriptExecutionContext> scriptContextThreadLocal = new ThreadLocal<>();
	private final AtomicBoolean promptDeferredByCommand = new AtomicBoolean(false);
	private final List<String> history = new ArrayList<>();
	private final AtomicInteger nextJobId = new AtomicInteger(1);
	private final AtomicInteger activeScripts = new AtomicInteger(0);
	private volatile Semaphore scriptSlots;
	private volatile int maxParallelSlots;
	private volatile ScriptExecutionContext activeForegroundContext;
	private volatile Future<Boolean> activeForegroundFuture;
	private int historyIndex;
	private String currentInput = "";
	private boolean running;
	private boolean autoPromptEnabled = true;
	private String promptTemplate = DEFAULT_PROMPT_TEMPLATE;

	public Terminal(ComputerModule module, Console console, FileSystem fileSystem) {
		this.module = module;
		this.console = console;
		this.fileSystem = fileSystem;
		packageManagerService = new PackageManagerService(fileSystem, console);
		maxParallelSlots = ConfigManager.getScriptMaxParallel();
		scriptSlots = new Semaphore(maxParallelSlots, true);
		scriptExecutor = Executors.newCachedThreadPool(new ScriptThreadFactory("luamade-script"));

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
		cancelForegroundScript(false);
		cancelAllBackgroundJobs(true);
		running = false;
	}

	@LuaMadeCallable
	public void reboot() {
		hardReset();
	}

	@LuaMadeCallable
	public void hardReset() {
		if(!running) {
			running = true;
		}
		cancelForegroundScript(false);
		cancelAllBackgroundJobs(true);
		bootTerminal(true);
	}

	private boolean cancelForegroundScript(boolean printInterruptMarker) {
		boolean visibleBeforeClear = module.getGfxApi().hasVisibleCommands();
		ScriptExecutionContext context = activeForegroundContext;
		Future<Boolean> future = activeForegroundFuture;
		logInterruptDebug("cancelForegroundScript begin: printInterruptMarker=" + printInterruptMarker
				+ ", contextPresent=" + (context != null)
				+ ", futurePresent=" + (future != null)
				+ ", futureDone=" + (future != null && future.isDone())
				+ ", activeScripts=" + activeScripts.get()
				+ ", runningBackgroundJobs=" + countRunningBackgroundJobs()
				+ ", visibleBeforeClear=" + visibleBeforeClear);

		// Clear the graphics buffer so any GUI drawn by the script disappears,
		// and reset the input API to release keyboard/mouse locks the script held.
		module.getGfxApi().forceClear();
		module.getInputApi().reset();
		logInterruptDebug("cancelForegroundScript after clear: visibleAfterClear=" + module.getGfxApi().hasVisibleCommands());
		if(printInterruptMarker) {
			console.print(valueOf("^C"));
		}
		if(context == null) {
			// Foreground may already be cleared by a race; stop background loops too
			// so stale GUI redraws cannot continue after Ctrl+C.
			int canceled = cancelAllBackgroundJobs(false);
			logInterruptDebug("cancelForegroundScript no foreground context; canceledBackgroundJobs=" + canceled);
			return canceled > 0;
		}

		activeForegroundContext = null;
		activeForegroundFuture = null;
		context.requestCancel();
		if(future != null) {
			future.cancel(true);
		}
		int canceledBackground = cancelAllBackgroundJobs(false);
		logInterruptDebug("cancelForegroundScript foreground canceled; canceledBackgroundJobs=" + canceledBackground);
		return true;
	}

	private int cancelAllBackgroundJobs(boolean clearRegistry) {
		refreshBackgroundJobs();

		int canceled = 0;
		for(BackgroundJob job : backgroundJobs.values()) {
			if(job == null || job.getStatus() != JobStatus.RUNNING) {
				continue;
			}

			Future<Boolean> future = job.getFuture();
			if(future != null && !future.isDone()) {
				job.getContext().requestCancel();
				job.setStatus(JobStatus.CANCELED);
				future.cancel(true);
				canceled++;
			}
		}

		if(clearRegistry) {
			backgroundJobs.clear();
		}

		// Ensure no graphics or input locks linger from killed background scripts.
		module.getGfxApi().forceClear();
		module.getInputApi().reset();

		return canceled;
	}

	private int countRunningBackgroundJobs() {
		int runningCount = 0;
		for(BackgroundJob job : backgroundJobs.values()) {
			if(job != null && job.getStatus() == JobStatus.RUNNING) {
				runningCount++;
			}
		}
		return runningCount;
	}

	private void logInterruptDebug(String message) {
		if(!ConfigManager.isDebugMode()) {
			return;
		}
		LuaMade instance = LuaMade.getInstance();
		if(instance != null) {
			instance.logDebug("[INTERRUPT] " + message);
		}
	}

	/**
	 * Handles input from the user
	 */
	@LuaMadeCallable
	public void handleInput(String input) {
		if(!running) {
			return;
		}

		promptDeferredByCommand.set(false);

		String submittedInput = input == null ? "" : input;

		// Commit the typed command into the console transcript before processing.
		console.appendInline(valueOf(submittedInput));

		// Move to a new line after the inline prompt when Enter is pressed.
		console.print(valueOf(""));

		List<QueuedCommand> queuedCommands = splitQueuedCommands(submittedInput);
		boolean previousSucceeded = true;
		for(QueuedCommand queuedCommand : queuedCommands) {
			if(!queuedCommand.shouldRun(previousSucceeded)) {
				continue;
			}
			previousSucceeded = executeQueuedCommand(queuedCommand.commandText);
		}

		if(autoPromptEnabled && running && !promptDeferredByCommand.get()) {
			printPrompt();
		}
	}

	private boolean executeQueuedCommand(String rawCommandLine) {
		String commandLine = rawCommandLine == null ? "" : rawCommandLine.trim();
		if(commandLine.isEmpty()) {
			return true;
		}

		String expandedHistoryCommand = expandHistoryCommand(commandLine);
		if(expandedHistoryCommand == null) {
			return false;
		}

		if(!expandedHistoryCommand.equals(commandLine)) {
			console.print(valueOf(expandedHistoryCommand));
		}
		commandLine = expandedHistoryCommand;

		history.add(commandLine);
		historyIndex = history.size();

		List<String> tokens = parseCommandTokens(commandLine);
		if(tokens.isEmpty()) {
			return true;
		}

		String commandName = tokens.get(0);
		String args = joinTokens(tokens, 1);

		Command command = commands.get(commandName);
		if(command != null) {
			try {
				command.execute(args);
				return true;
			} catch(LuaError error) {
				String message = error.getMessage();
				console.print(valueOf(message == null || message.trim().isEmpty() ? "Command failed" : message));
				return false;
			} catch(Exception exception) {
				console.print(valueOf("Command failed: " + exception.getMessage()));
				return false;
			}
		}

		String resolvedScriptPath = resolveScriptPath(commandName);
		if(resolvedScriptPath != null) {
			return runScriptAtPath(resolvedScriptPath, tokens.subList(1, tokens.size()));
		} else {
			console.print(valueOf("Unknown command: " + commandName));
			return false;
		}
	}

	private List<QueuedCommand> splitQueuedCommands(String submittedInput) {
		List<QueuedCommand> commandsOut = new ArrayList<>();
		if(submittedInput == null || submittedInput.isEmpty()) {
			return commandsOut;
		}

		StringBuilder current = new StringBuilder();
		boolean inQuotes = false;
		char quoteChar = 0;
		ChainCondition nextCondition = ChainCondition.ALWAYS;

		for(int i = 0; i < submittedInput.length(); i++) {
			char c = submittedInput.charAt(i);

			if(c == '\\' && i + 1 < submittedInput.length()) {
				char next = submittedInput.charAt(i + 1);
				if(next == '\\' || next == '"' || next == '\'') {
					current.append('\\').append(next);
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
				current.append(c);
				continue;
			}

			if(!inQuotes && (c == '\n' || c == '\r' || c == ';')) {
				String command = current.toString().trim();
				if(!command.isEmpty()) {
					commandsOut.add(new QueuedCommand(command, nextCondition));
				}
				current.setLength(0);
				nextCondition = ChainCondition.ALWAYS;
				if(c == '\r' && i + 1 < submittedInput.length() && submittedInput.charAt(i + 1) == '\n') {
					i++;
				}
				continue;
			}

			if(!inQuotes && c == '&' && i + 1 < submittedInput.length() && submittedInput.charAt(i + 1) == '&') {
				String command = current.toString().trim();
				if(!command.isEmpty()) {
					commandsOut.add(new QueuedCommand(command, nextCondition));
				}
				current.setLength(0);
				nextCondition = ChainCondition.ON_SUCCESS;
				i++;
				continue;
			}

			if(!inQuotes && c == '|' && i + 1 < submittedInput.length() && submittedInput.charAt(i + 1) == '|') {
				String command = current.toString().trim();
				if(!command.isEmpty()) {
					commandsOut.add(new QueuedCommand(command, nextCondition));
				}
				current.setLength(0);
				nextCondition = ChainCondition.ON_FAILURE;
				i++;
				continue;
			}

			current.append(c);
		}

		String trailing = current.toString().trim();
		if(!trailing.isEmpty()) {
			commandsOut.add(new QueuedCommand(trailing, nextCondition));
		}

		return commandsOut;
	}

	/**
	 * Executes a Lua script with arguments
	 */
	private boolean executeScript(String scriptPath, String script, List<String> args) {
		ScriptExecutionContext context = scriptContextThreadLocal.get();
		if(context != null) {
			context.throwIfCancellationRequested();
		}

		try {
			// Create a sandboxed Lua environment
			Globals globals = createSandboxedGlobals(context);

			// Set up arguments
			LuaTable argsTable = new LuaTable();
			for(int i = 0; i < args.size(); i++) {
				argsTable.set(i + 1, valueOf(args.get(i)));
			}
			globals.set("args", argsTable);

			// Execute the script
			LuaValue chunk = globals.load(script, scriptPath);
			if(context != null) {
				context.throwIfCancellationRequested();
			}
			chunk.call();
			return true;
		} catch(LuaError error) {
			if(context != null && context.isCancellationRequested()) {
				return false;
			}
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

	private Future<Boolean> submitScriptTask(String scriptPath, String script, List<String> args, ScriptExecutionContext context) {
		return scriptExecutor.submit(() -> {
			scriptContextThreadLocal.set(context);
			context.bindToCurrentThread();
			activeScripts.incrementAndGet();
			try {
				return executeScript(scriptPath, script, args);
			} finally {
				scriptContextThreadLocal.remove();
				activeScripts.decrementAndGet();
				scriptSlots.release();
				module.getGfxApi().clear();
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

	private boolean startForegroundScript(String scriptPath, String script, List<String> args) {
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

		ScriptExecutionContext context = new ScriptExecutionContext();
		Future<Boolean> future = submitScriptTask(scriptPath, script, args, context);
		activeForegroundContext = context;
		activeForegroundFuture = future;
		AtomicBoolean promptPrinted = new AtomicBoolean(false);

		scriptExecutor.submit(() -> {
			try {
				future.get();
			} catch(InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
			} catch(CancellationException cancellationException) {
				// Cancellation messages are handled by the callers that requested it.
			} catch(ExecutionException executionException) {
				console.print(valueOf("Error executing script: " + executionException.getMessage()));
			} finally {
				if(activeForegroundFuture == future) {
					activeForegroundContext = null;
					activeForegroundFuture = null;
				}
				if(autoPromptEnabled && running && promptPrinted.compareAndSet(false, true)) {
					printPrompt();
				}
			}
		});

		deferPromptAfterCommand();
		return true;
	}

	private int startBackgroundScript(String scriptPath, String script, List<String> args) {
		refreshBackgroundJobs();
		if(!acquireScriptSlot()) {
			return -1;
		}

		int jobId = nextJobId.getAndIncrement();
		ScriptExecutionContext context = new ScriptExecutionContext();
		BackgroundJob job = new BackgroundJob(jobId, scriptPath, context);
		Future<Boolean> future = submitScriptTask(scriptPath, script, args, context);
		job.setFuture(future);
		backgroundJobs.put(jobId, job);

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
				if(Boolean.TRUE.equals(success)) {
					job.setStatus(JobStatus.COMPLETED);
				} else if(job.getContext().isCancellationRequested()) {
					job.setStatus(JobStatus.CANCELED);
				} else {
					job.setStatus(JobStatus.FAILED);
				}
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

	private boolean runScriptAtPath(String scriptPath, List<String> args) {
		String script = fileSystem.read(scriptPath);
		if(script == null) {
			console.print(valueOf("Error: Could not read script file: " + scriptPath));
			return false;
		}

		console.print(valueOf("Running script: " + scriptPath));
		return startForegroundScript(scriptPath, script, args);
	}

	private void bootTerminal(boolean clearConsole) {
		if(clearConsole) {
			console.clearTextContents();
		}

		resetRuntimeConfiguration();
		boolean startupStarted = runStartupScriptAsync();
		if(!startupStarted) {
			printDefaultStartupBanner();
			if(autoPromptEnabled && running) {
				printPrompt();
			}
		}
	}

	private void resetRuntimeConfiguration() {
		autoPromptEnabled = true;
		promptTemplate = DEFAULT_PROMPT_TEMPLATE;
		fileSystem.resetWorkingDirectory();
	}

	private boolean runStartupScriptAsync() {
		if(!fileSystem.exists(STARTUP_SCRIPT_PATH) || fileSystem.isDir(STARTUP_SCRIPT_PATH)) {
			return false;
		}

		String script = fileSystem.read(STARTUP_SCRIPT_PATH);
		if(script == null) {
			console.print(valueOf("Error: Could not read startup script: " + STARTUP_SCRIPT_PATH));
			return false;
		}

		return startForegroundScript(STARTUP_SCRIPT_PATH, script, new ArrayList<String>());
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
		if(looksLikeLuaScript(trimmed) && isLuaFile(normalized)) {
			return normalized;
		}

		if(!looksLikeLuaScript(trimmed)) {
			String normalizedWithExt = fileSystem.normalizePath(trimmed + ".lua");
			if(isLuaFile(normalizedWithExt)) {
				return normalizedWithExt;
			}
		}

		if(!trimmed.startsWith("/")) {
			if(!looksLikeLuaScript(trimmed)) {
				String binCandidateWithExt = fileSystem.normalizePath("/bin/" + trimmed + ".lua");
				if(isLuaFile(binCandidateWithExt)) {
					return binCandidateWithExt;
				}
			} else {
				String binCandidate = fileSystem.normalizePath("/bin/" + trimmed);
				if(isLuaFile(binCandidate)) {
					return binCandidate;
				}
			}
		}

		return null;
	}

	private boolean isLuaFile(String path) {
		return looksLikeLuaScript(path) && fileSystem.exists(path) && !fileSystem.isDir(path);
	}

	private boolean looksLikeLuaScript(String path) {
		return path != null && path.toLowerCase(Locale.ROOT).endsWith(".lua");
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
	private Globals createSandboxedGlobals(ScriptExecutionContext context) {
		Globals globals = new Globals();

		// Load only safe libraries
		globals.load(new BaseLib());
		// PackageLib must come before CoroutineLib: CoroutineLib registers itself into
		// package.loaded on init, so loading it before PackageLib causes a nil-index crash
		// that prevents the entire sandbox from being constructed.
		globals.load(new PackageLib());
		// String/Table libs register themselves via package.loaded in LuaJ.
		globals.load(new StringLib());
		globals.load(new TableLib());
		globals.load(new JseMathLib());
		globals.load(new Bit32Lib());
		globals.load(new CoroutineLib());
		LuaC.install(globals);

		// Create our own sandboxed versions of these
		globals.set("dofile", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs vargs) {
				if(vargs.narg() < 1 || vargs.arg1().isnil()) {
					return NIL;
				}

				String path = vargs.arg1().tojstring();
				String resolvedPath = resolveScriptPath(path);
				if(resolvedPath == null) {
					throw new LuaError("dofile could not resolve script: " + path);
				}

				String script = fileSystem.read(resolvedPath);
				if(script == null) {
					throw new LuaError("Could not read script file: " + resolvedPath);
				}

				LuaValue chunk = compileLuaChunk(globals, script, resolvedPath, context);
				if(context != null) {
					context.throwIfCancellationRequested();
				}
				return chunk.invoke(vargs.subargs(2));
			}
		});
		globals.set("loadfile", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs vargs) {
				if(vargs.narg() < 1 || vargs.arg1().isnil()) {
					return NIL;
				}

				String path = vargs.arg1().tojstring();
				String resolvedPath = resolveScriptPath(path);
				if(resolvedPath == null) {
					console.print(valueOf("Error: loadfile could not resolve script: " + path));
					return NIL;
				}

				String script = fileSystem.read(resolvedPath);
				if(script == null) {
					throw new LuaError("Could not read script file: " + resolvedPath);
				}

				return compileLuaChunk(globals, script, resolvedPath, context);
			}
		});
		globals.set("load", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs vargs) {
				if(vargs.narg() < 1 || vargs.arg1().isnil()) {
					return NIL;
				}

				String script = vargs.arg1().tojstring();
				String chunkName = vargs.narg() >= 2 && vargs.arg(2).isstring() ? vargs.arg(2).tojstring() : "chunk";
				return compileLuaChunk(globals, script, chunkName, context);
			}
		});
		LuaTable sandboxPackage = createSandboxedPackageTable();
		globals.set("package", sandboxPackage);
		globals.set("require", createStrictRequire(globals, sandboxPackage, context));

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
		globals.set("gfx", module.getGfxApi());
		globals.set("shell", createShellCompatibilityApi());


		LuaTable jsonLibrary = loadBuiltinLibrary(globals, "scripts/lib/json.lua", "json");
		LuaTable utilLibrary = loadBuiltinLibrary(globals, "scripts/lib/util.lua", "util");
		LuaTable vectorLibrary = loadBuiltinLibrary(globals, "scripts/lib/vector.lua", "vector");
		LuaTable gfxLibrary = loadBuiltinLibrary(globals, "scripts/lib/gfxlib.lua", "gfxlib");
		LuaTable loadedModules = sandboxPackage.get("loaded").checktable();
		if(jsonLibrary != null) {
			loadedModules.set("json", jsonLibrary);
		}
		if(utilLibrary != null) {
			loadedModules.set("util", utilLibrary);
		}
		if(vectorLibrary != null) {
			loadedModules.set("vector", vectorLibrary);
		}
		if(gfxLibrary != null) {
			loadedModules.set("gfxlib", gfxLibrary);
		}

		UtilApi nativeUtil = new UtilApi(context == null ? null : context::throwIfCancellationRequested);
		if(utilLibrary != null) {
			utilLibrary.set("now", nativeUtil.get("now"));
			utilLibrary.set("sleep", nativeUtil.get("sleep"));
		} else {
			globals.set("util", nativeUtil);
		}
		return globals;
	}

	private LuaTable createSandboxedPackageTable() {
		LuaTable sandboxPackage = new LuaTable();
		sandboxPackage.set("loaded", new LuaTable());
		sandboxPackage.set("preload", new LuaTable());
		sandboxPackage.set("path", valueOf(""));
		sandboxPackage.set("cpath", valueOf(""));
		return sandboxPackage;
	}

	private LuaFunction createStrictRequire(Globals globals, LuaTable sandboxPackage, ScriptExecutionContext context) {
		return new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue moduleNameValue) {
				String moduleName = normalizeRequireModuleName(moduleNameValue);
				if(moduleName == null) {
					throw new LuaError("require expects a module name like 'mylib.math'");
				}

				LuaTable loaded = sandboxPackage.get("loaded").checktable();
				LuaTable preload = sandboxPackage.get("preload").checktable();

				LuaValue alreadyLoaded = loaded.get(moduleName);
				if(!alreadyLoaded.isnil()) {
					if(alreadyLoaded == REQUIRE_LOADING_SENTINEL) {
						throw new LuaError("recursive require detected for module: " + moduleName);
					}
					return alreadyLoaded;
				}

				LuaValue preloaded = preload.get(moduleName);
				if(preloaded.isfunction()) {
					loaded.set(moduleName, REQUIRE_LOADING_SENTINEL);
					try {
						LuaValue result = preloaded.call(valueOf(moduleName));
						LuaValue stored = result.isnil() ? TRUE : result;
						loaded.set(moduleName, stored);
						return stored;
					} catch(Exception exception) {
						loaded.set(moduleName, NIL);
						throw exception;
					}
				}

				String modulePath = resolveRequireModulePath(moduleName);
				if(modulePath == null) {
					throw new LuaError("module not found: " + moduleName);
				}

				if(context != null) {
					context.throwIfCancellationRequested();
				}

				String source = fileSystem.read(modulePath);
				if(source == null) {
					throw new LuaError("module unreadable: " + moduleName);
				}

				loaded.set(moduleName, REQUIRE_LOADING_SENTINEL);
				try {
					LuaValue chunk = compileLuaChunk(globals, source, modulePath, context);
					LuaValue result = chunk.call();
					LuaValue stored = result.isnil() ? TRUE : result;
					loaded.set(moduleName, stored);
					return stored;
				} catch(Exception exception) {
					loaded.set(moduleName, NIL);
					throw exception;
				}
			}
		};
	}

	private LuaValue compileLuaChunk(Globals globals, String source, String chunkName, ScriptExecutionContext context) {
		if(source == null) {
			throw new LuaError("no source provided");
		}
		if(source.length() > MAX_LOADER_SOURCE_CHARS) {
			throw new LuaError("chunk exceeds maximum size of " + MAX_LOADER_SOURCE_CHARS + " characters");
		}
		if(!source.isEmpty() && source.charAt(0) == '\u001b') {
			throw new LuaError("binary chunks are not supported in sandbox");
		}
		if(context != null) {
			context.throwIfCancellationRequested();
		}

		return globals.load(source, chunkName == null || chunkName.trim().isEmpty() ? "chunk" : chunkName);
	}

	private String normalizeRequireModuleName(LuaValue moduleNameValue) {
		if(moduleNameValue == null || moduleNameValue.isnil() || !moduleNameValue.isstring()) {
			return null;
		}

		String moduleName = moduleNameValue.tojstring().trim().toLowerCase(Locale.ROOT);
		if(moduleName.isEmpty()) {
			return null;
		}

		if(!SAFE_REQUIRE_MODULE_PATTERN.matcher(moduleName).matches()) {
			return null;
		}

		return moduleName;
	}

	private String resolveRequireModulePath(String moduleName) {
		if(moduleName == null) {
			return null;
		}

		String relativePath = moduleName.replace('.', '/');
		for(String root : REQUIRE_LIBRARY_ROOTS) {
			String candidate = fileSystem.normalizePath(root + "/" + relativePath + ".lua");
			if(isLuaFile(candidate)) {
				return candidate;
			}
		}

		if(ConfigManager.isAllowedLuaPackage(moduleName)) {
			String builtinCandidate = fileSystem.normalizePath("/scripts/lib/" + relativePath + ".lua");
			if(isLuaFile(builtinCandidate)) {
				return builtinCandidate;
			}
		}

		return null;
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
		String normalizedName = normalizeCommandName(name);
		if(normalizedName == null || callback == null || !callback.isfunction()) {
			return;
		}

		synchronized(commands) {
			commands.put(normalizedName, new LuaCommand(normalizedName, callback));
		}
	}

	@LuaMadeCallable
	public boolean unregisterCommand(String name) {
		String normalizedName = normalizeCommandName(name);
		if(normalizedName == null) {
			return false;
		}

		synchronized(commands) {
			return commands.remove(normalizedName) != null;
		}
	}

	@LuaMadeCallable
	public boolean hasCommand(String name) {
		String normalizedName = normalizeCommandName(name);
		if(normalizedName == null) {
			return false;
		}

		synchronized(commands) {
			return commands.containsKey(normalizedName);
		}
	}

	/**
	 * Wraps an existing command with a Lua callback.
	 * Callback signature: callback(args, next)
	 * - args: command arguments string
	 * - next: function(nextArgs) to invoke original command behavior
	 */
	@LuaMadeCallable
	public boolean wrapCommand(String name, LuaValue callback) {
		String normalizedName = normalizeCommandName(name);
		if(normalizedName == null || callback == null || !callback.isfunction()) {
			return false;
		}

		synchronized(commands) {
			Command existing = commands.get(normalizedName);
			if(existing == null) {
				return false;
			}

			commands.put(normalizedName, new LuaWrappedCommand(normalizedName, existing, callback));
		}
		return true;
	}

	private String normalizeCommandName(String name) {
		if(name == null) {
			return null;
		}

		String normalized = name.trim();
		if(normalized.isEmpty() || normalized.contains(" ")) {
			return null;
		}

		return normalized;
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

	/**
	 * Returns command suggestions for the first token in the current input.
	 * Suggestions include built-ins and runnable scripts from /bin.
	 */
	@LuaMadeCallable
	public List<String> getCommandSuggestions(String partialInput) {
		String normalizedInput = partialInput == null ? "" : partialInput.trim();
		if(normalizedInput.contains(" ")) {
			return Collections.emptyList();
		}

		String prefix = normalizedInput.toLowerCase(Locale.ROOT);
		List<String> candidates = collectCommandSuggestionCandidates();
		List<String> matches = new ArrayList<>();
		for(String candidate : candidates) {
			if(candidate == null || candidate.isEmpty()) {
				continue;
			}
			if(prefix.isEmpty() || candidate.toLowerCase(Locale.ROOT).startsWith(prefix)) {
				matches.add(candidate);
			}
		}

		matches.sort(String.CASE_INSENSITIVE_ORDER);
		return matches;
	}

	/**
	 * Returns the best completion candidate for the first token in input,
	 * or null if no completion is available.
	 */
	@LuaMadeCallable
	public String getBestCommandCompletion(String partialInput) {
		List<String> suggestions = getCommandSuggestions(partialInput);
		if(suggestions.isEmpty()) {
			return null;
		}

		String normalizedInput = partialInput == null ? "" : partialInput.trim();
		if(normalizedInput.isEmpty()) {
			return suggestions.get(0);
		}

		for(String suggestion : suggestions) {
			if(suggestion.equalsIgnoreCase(normalizedInput)) {
				return suggestion;
			}
		}

		return suggestions.get(0);
	}

	/**
	 * Returns path/filename completions for the last argument token in partialInput.
	 * Returns an empty list when the input contains no space (command-only mode).
	 * Directories are returned with a trailing '/'.
	 */
	@LuaMadeCallable
	public List<String> getPathSuggestions(String partialInput) {
		if(partialInput == null || !partialInput.contains(" ")) {
			return Collections.emptyList();
		}

		// Determine the partial path token at the end of the input.
		boolean endsWithSpace = partialInput.endsWith(" ");
		String pathPrefix;
		if(endsWithSpace) {
			pathPrefix = "";
		} else {
			int lastSpace = partialInput.lastIndexOf(' ');
			pathPrefix = partialInput.substring(lastSpace + 1);
		}

		return collectPathSuggestions(pathPrefix);
	}

	private List<String> collectPathSuggestions(String pathPrefix) {
		String dirPath;
		String namePrefix;
		String displayDirPrefix;

		int lastSlash = pathPrefix.lastIndexOf('/');
		if(lastSlash >= 0) {
			displayDirPrefix = pathPrefix.substring(0, lastSlash + 1);
			namePrefix = pathPrefix.substring(lastSlash + 1);
			String dirPart = pathPrefix.substring(0, lastSlash);
			dirPath = fileSystem.normalizePath(dirPart.isEmpty() ? "/" : dirPart);
		} else {
			displayDirPrefix = "";
			namePrefix = pathPrefix;
			dirPath = fileSystem.getCurrentDir();
		}

		List<String> entries = fileSystem.list(dirPath);
		if(entries == null || entries.isEmpty()) {
			return Collections.emptyList();
		}

		String lowerPrefix = namePrefix.toLowerCase(Locale.ROOT);
		List<String> matches = new ArrayList<>();
		for(String entry : entries) {
			if(entry == null || entry.isEmpty()) continue;
			if(!namePrefix.isEmpty() && !entry.toLowerCase(Locale.ROOT).startsWith(lowerPrefix)) continue;

			String entryPath = "/".equals(dirPath) ? "/" + entry : dirPath + "/" + entry;
			String completion = displayDirPrefix + entry;
			if(fileSystem.isDir(entryPath)) {
				completion += "/";
			}
			matches.add(completion);
		}

		matches.sort(String.CASE_INSENSITIVE_ORDER);
		return matches;
	}

	/**
	 * Interrupts the currently running foreground script (Ctrl+C).
	 * Returns true if a foreground script was active and has been signaled to stop.
	 */
	@LuaMadeCallable
	public boolean interruptForeground() {
		return cancelForegroundScript(true);
	}

	/**
	 * Attempts to parse and import a versioned clipboard text bundle.
	 * Returns false when the clipboard text does not match the bundle header.
	 */
	public boolean importClipboardProtocol(String clipboardText, boolean overwriteExisting) {
		LinkedHashMap<String, String> parsed = parseClipboardBundle(clipboardText);
		if(parsed == null) {
			return false;
		}

		if(parsed.isEmpty()) {
			console.print(valueOf("Clipboard bundle contains no files"));
			return true;
		}

		importClipboardEntries(parsed, overwriteExisting, "clipboard bundle");
		return true;
	}

	/**
	 * Imports host-provided file payloads into the terminal's current directory.
	 */
	public boolean importClipboardFiles(Map<String, String> files, boolean overwriteExisting, String sourceLabel) {
		if(files == null || files.isEmpty()) {
			return false;
		}
		importClipboardEntries(new LinkedHashMap<>(files), overwriteExisting, sourceLabel == null ? "clipboard" : sourceLabel);
		return true;
	}

	private LinkedHashMap<String, String> parseClipboardBundle(String clipboardText) {
		if(clipboardText == null) {
			return null;
		}

		String normalized = clipboardText.replace("\r\n", "\n").replace('\r', '\n');
		String[] lines = normalized.split("\n");
		if(lines.length == 0 || !CLIPBOARD_BUNDLE_HEADER.equals(lines[0].trim())) {
			return null;
		}

		LinkedHashMap<String, String> files = new LinkedHashMap<>();
		for(int i = 1; i < lines.length; i++) {
			String line = lines[i] == null ? "" : lines[i].trim();
			if(line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			if(!line.startsWith("FILE ")) {
				console.print(valueOf("Clipboard bundle parse error at line " + (i + 1) + ": expected FILE entry"));
				return new LinkedHashMap<>();
			}

			String payload = line.substring(5).trim();
			int splitIndex = payload.indexOf(' ');
			if(splitIndex <= 0 || splitIndex >= payload.length() - 1) {
				console.print(valueOf("Clipboard bundle parse error at line " + (i + 1) + ": malformed FILE entry"));
				return new LinkedHashMap<>();
			}

			String encodedPath = payload.substring(0, splitIndex).trim();
			String encodedContent = payload.substring(splitIndex + 1).trim();
			try {
				String path = new String(Base64.getDecoder().decode(encodedPath), StandardCharsets.UTF_8);
				String content = new String(Base64.getDecoder().decode(encodedContent), StandardCharsets.UTF_8);
				files.put(path, content);
			} catch(IllegalArgumentException exception) {
				console.print(valueOf("Clipboard bundle parse error at line " + (i + 1) + ": invalid base64"));
				return new LinkedHashMap<>();
			}
		}

		return files;
	}

	private void importClipboardEntries(LinkedHashMap<String, String> files, boolean overwriteExisting, String sourceLabel) {
		if(files == null || files.isEmpty()) {
			return;
		}

		String baseDir = fileSystem.getCurrentDir();
		int imported = 0;
		int skipped = 0;
		int failed = 0;
		int totalChars = 0;

		if(files.size() > CLIPBOARD_IMPORT_MAX_FILES) {
			console.print(valueOf("Error: Clipboard payload has too many files (max " + CLIPBOARD_IMPORT_MAX_FILES + ")"));
			return;
		}

		for(Map.Entry<String, String> entry : files.entrySet()) {
			String relativePath = sanitizeClipboardRelativePath(entry.getKey());
			if(relativePath == null) {
				failed++;
				continue;
			}

			String content = entry.getValue();
			if(content == null) {
				content = "";
			}

			if(content.length() > CLIPBOARD_IMPORT_MAX_FILE_CHARS) {
				console.print(valueOf("Skipped " + relativePath + " (file too large)"));
				skipped++;
				continue;
			}

			totalChars += content.length();
			if(totalChars > CLIPBOARD_IMPORT_MAX_TOTAL_CHARS) {
				console.print(valueOf("Stopped import: clipboard payload exceeds size limit"));
				break;
			}

			String destination = fileSystem.normalizePath(baseDir + "/" + relativePath);
			if(fileSystem.exists(destination) && fileSystem.isDir(destination)) {
				console.print(valueOf("Skipped " + relativePath + " (destination is a directory)"));
				skipped++;
				continue;
			}

			if(fileSystem.exists(destination) && !overwriteExisting) {
				skipped++;
				continue;
			}

			if(!ensureParentDirectoryExists(destination)) {
				console.print(valueOf(fsErrorOr("Error: Could not create parent directory for " + relativePath)));
				failed++;
				continue;
			}

			if(fileSystem.write(destination, content)) {
				imported++;
			} else {
				console.print(valueOf(fsErrorOr("Error: Could not write " + relativePath)));
				failed++;
			}
		}

		StringBuilder summary = new StringBuilder();
		summary.append("Imported ").append(imported).append(imported == 1 ? " file" : " files").append(" from ").append(sourceLabel == null ? "clipboard" : sourceLabel).append(" into ").append(baseDir);
		if(skipped > 0) {
			summary.append(" (skipped ").append(skipped).append(')');
		}
		if(failed > 0) {
			summary.append(" (failed ").append(failed).append(')');
		}
		console.print(valueOf(summary.toString()));
	}

	private boolean ensureParentDirectoryExists(String normalizedFilePath) {
		if(normalizedFilePath == null || normalizedFilePath.isEmpty() || "/".equals(normalizedFilePath)) {
			return false;
		}

		int slashIndex = normalizedFilePath.lastIndexOf('/');
		if(slashIndex <= 0) {
			return true;
		}

		String parent = normalizedFilePath.substring(0, slashIndex);
		if(parent.isEmpty() || "/".equals(parent)) {
			return true;
		}

		if(fileSystem.exists(parent)) {
			return fileSystem.isDir(parent);
		}

		String[] segments = parent.substring(1).split("/");
		String current = "";
		for(String segment : segments) {
			if(segment == null || segment.isEmpty()) {
				continue;
			}
			current += "/" + segment;
			if(fileSystem.exists(current)) {
				if(!fileSystem.isDir(current)) {
					return false;
				}
				continue;
			}
			if(!fileSystem.makeDir(current)) {
				return false;
			}
		}

		return true;
	}

	private String sanitizeClipboardRelativePath(String rawPath) {
		if(rawPath == null) {
			console.print(valueOf("Skipped clipboard entry with empty path"));
			return null;
		}

		String normalized = rawPath.trim().replace('\\', '/');
		while(normalized.startsWith("/")) {
			normalized = normalized.substring(1);
		}
		if(normalized.isEmpty()) {
			console.print(valueOf("Skipped clipboard entry with empty path"));
			return null;
		}

		String[] segments = normalized.split("/");
		List<String> safeSegments = new ArrayList<>();
		for(String segment : segments) {
			if(segment == null || segment.isEmpty() || ".".equals(segment)) {
				continue;
			}
			if("..".equals(segment)) {
				console.print(valueOf("Skipped " + normalized + " (path traversal is not allowed)"));
				return null;
			}
			safeSegments.add(segment);
		}

		if(safeSegments.isEmpty()) {
			console.print(valueOf("Skipped clipboard entry with empty path"));
			return null;
		}

		return joinTokens(safeSegments, 0).replace(' ', '/');
	}

	private List<String> collectCommandSuggestionCandidates() {
		LinkedHashSet<String> candidates = new LinkedHashSet<>();

		synchronized(commands) {
			candidates.addAll(commands.keySet());
		}

		List<String> binEntries = fileSystem.list("/bin");
		if(binEntries != null) {
			for(String entry : binEntries) {
				if(entry == null || entry.isEmpty()) {
					continue;
				}

				String scriptPath = fileSystem.normalizePath("/bin/" + entry);
				if(!fileSystem.exists(scriptPath) || fileSystem.isDir(scriptPath)) {
					continue;
				}

				if(entry.endsWith(".lua") && entry.length() > 4) {
					candidates.add(entry.substring(0, entry.length() - 4));
				} else {
					candidates.add(entry);
				}
			}
		}

		return new ArrayList<>(candidates);
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
	public String httpPut(String url, String body) {
		return putWebData(url, body == null ? "" : body, null);
	}

	@LuaMadeCallable
	public String httpPut(String url, String body, String contentType) {
		return putWebData(url, body == null ? "" : body, contentType);
	}

	@LuaMadeCallable
	public String getPromptTemplate() {
		return promptTemplate;
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

	@LuaMadeCallable
	public String getScrollMode() {
		return module == null ? "VERTICAL" : module.getScrollModeName();
	}

	@LuaMadeCallable
	public boolean setScrollMode(String mode) {
		return module != null && module.setScrollMode(mode);
	}

	@LuaMadeCallable
	public boolean isMaskedEnterForwardingEnabled() {
		return module != null && module.isMaskedEnterForwardingEnabled();
	}

	@LuaMadeCallable
	public void setMaskedEnterForwardingEnabled(boolean enabled) {
		if(module != null) {
			module.setMaskedEnterForwardingEnabled(enabled);
		}
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

		return template.replace("{name}", module.getPromptComputerName()).replace("{display}", module.getDisplayName()).replace("{hostname}", hostname).replace("{dir}", promptPath);
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
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					printHelpIndex();
					return;
				}

				List<String> tokens = parseCommandTokens(trimmed);
				if(tokens.isEmpty()) {
					printHelpIndex();
					return;
				}

				String targetName = normalizeCommandName(tokens.get(0));
				if(targetName == null) {
					console.print(valueOf("Error: Usage: help [command]"));
					return;
				}

				Command target = commands.get(targetName);
				if(target == null) {
					console.print(valueOf("Error: Unknown command: " + targetName));
					List<String> suggestions = getCommandSuggestions(targetName);
					if(!suggestions.isEmpty()) {
						console.print(valueOf("Try: help " + suggestions.get(0)));
					}
					return;
				}

				printCommandHelp(target);
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
				deferPromptAfterCommand();
				reboot();
			}
		});

		commands.put("scrollmode", new Command("scrollmode", "View or set scrollbar mode (NONE/HORIZONTAL/VERTICAL/BOTH)") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim();
				if(trimmed.isEmpty()) {
					console.print(valueOf("Scroll mode: " + module.getScrollModeName()));
					console.print(valueOf("Usage: scrollmode <NONE|HORIZONTAL|VERTICAL|BOTH>"));
					return;
				}

				if(module.setScrollMode(trimmed)) {
					console.print(valueOf("Scroll mode set to " + module.getScrollModeName()));
				} else {
					console.print(valueOf("Error: Invalid scroll mode. Use NONE, HORIZONTAL, VERTICAL, or BOTH"));
				}
			}
		});

		commands.put("maskenter", new Command("maskenter", "Control Enter key forwarding while gfx input masking is active") {
			@Override
			public void execute(String args) {
				String trimmed = args == null ? "" : args.trim().toLowerCase(Locale.ROOT);
				if(trimmed.isEmpty()) {
					console.print(valueOf("Masked Enter forwarding: " + (module.isMaskedEnterForwardingEnabled() ? "ON" : "OFF")));
					console.print(valueOf("Usage: maskenter <on|off>"));
					return;
				}

				if("on".equals(trimmed) || "true".equals(trimmed) || "1".equals(trimmed)) {
					module.setMaskedEnterForwardingEnabled(true);
					console.print(valueOf("Masked Enter forwarding is ON"));
					return;
				}

				if("off".equals(trimmed) || "false".equals(trimmed) || "0".equals(trimmed)) {
					module.setMaskedEnterForwardingEnabled(false);
					console.print(valueOf("Masked Enter forwarding is OFF"));
					return;
				}

				console.print(valueOf("Error: Usage: maskenter <on|off>"));
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

		// Httpput command
		commands.put("httpput", new Command("httpput", "Sends HTTP/HTTPS PUT data; optionally save response to a file") {
			@Override
			public void execute(String args) {
				HttpPutArgs parsed = parseHttpPutArgs(parseCommandTokens(args == null ? "" : args.trim()));
				if(parsed == null || parsed.url == null || parsed.payloadArg == null) {
					console.print(valueOf("Error: Usage: httpput [--content-type <mime>] <url> <payload|@file> [output-file]"));
					return;
				}

				String payload = parsed.payloadArg;
				if(parsed.payloadArg.startsWith("@") && parsed.payloadArg.length() > 1) {
					String sourcePath = parsed.payloadArg.substring(1);
					String filePayload = fileSystem.read(sourcePath);
					if(filePayload == null) {
						console.print(valueOf("Error: Could not read payload file: " + sourcePath));
						return;
					}
					payload = filePayload;
				}

				String response = putWebData(parsed.url, payload, parsed.contentType);
				if(response == null) {
					return;
				}

				if(parsed.outputPath != null) {
					String outputPath = parsed.outputPath;
					if(fileSystem.write(outputPath, response)) {
						console.print(valueOf("Saved " + response.getBytes(StandardCharsets.UTF_8).length + " bytes to " + fileSystem.normalizePath(outputPath)));
					} else {
						console.print(valueOf("Error: Could not write output file"));
					}
					return;
				}

				if(response.isEmpty()) {
					console.print(valueOf("PUT request completed (empty response body)"));
				} else {
					console.print(valueOf(response));
				}
			}
		});

		commands.put("pkg", new Command("pkg", "Trusted package manager (search/info/fetch/install/list/remove)") {
			@Override
			public void execute(String args) {
				packageManagerService.handleCommand(args);
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
					job.getContext().requestCancel();
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

		applyBuiltInCommandHelp();
	}

	private void printHelpIndex() {
		List<Command> sortedCommands = new ArrayList<>(commands.values());
		sortedCommands.sort(Comparator.comparing(Command::getName, String.CASE_INSENSITIVE_ORDER));
		console.print(valueOf("Available commands (use 'help <command>' for details):"));
		for(Command command : sortedCommands) {
			console.print(valueOf("    " + command.getName() + " - " + command.getDescription()));
		}
	}

	private void printCommandHelp(Command command) {
		if(command == null) {
			return;
		}

		console.print(valueOf(command.getName() + " - " + command.getDescription()));
		console.print(valueOf("Usage: " + command.getUsage()));
		if(command.hasGuidance()) {
			console.print(valueOf("Details: " + command.getGuidance()));
		}
	}

	private void applyBuiltInCommandHelp() {
		setCommandHelp("help", "help [command]", "Without args lists commands. With a command name, shows usage and arguments.");
		setCommandHelp("history", "history", "Shows previously executed command lines with indices for !<n> replay.");
		setCommandHelp("which", "which [-a] <command-or-path>", "Resolve command names or file/script paths. Use -a to print all matches.");
		setCommandHelp("fsauth", "fsauth <password> | fsauth --clear", "Unlock protected filesystem scopes for the current session or clear auth state.");
		setCommandHelp("protect", "protect <path> <password> [read,write,delete,list|copy|move|paste|all]", "Protects a path and limits operations until authenticated.");
		setCommandHelp("unprotect", "unprotect <path> <password>", "Removes password protection rule from a path.");
		setCommandHelp("perms", "perms [path]", "Without args lists all protection rules; with a path prints the effective rule.");
		setCommandHelp("name", "name [--reset|<display-name>]", "Show current display name, set a new one, or reset to default.");
		setCommandHelp("head", "head [-n lines] <file>", "Print first lines from a file. Default line count is 10.");
		setCommandHelp("tail", "tail [-n lines] <file>", "Print last lines from a file. Default line count is 10.");
		setCommandHelp("wc", "wc [-l] [-w] [-c] <file>...", "Count lines, words, and UTF-8 bytes for one or more files.");
		setCommandHelp("echo", "echo [-n] <text>", "Print text. Use -n to suppress trailing newline.");
		setCommandHelp("ls", "ls [-a] [-l] [-R] [path]", "List directory entries. -a shows dotfiles, -l long format, -R recursive.");
		setCommandHelp("find", "find [path] [-name <glob>] [-type f|d] [-maxdepth <n>]", "Traverse directories and filter matches by name, type, and depth.");
		setCommandHelp("grep", "grep [-n] [-i] [-r] <pattern> <path>", "Search text in files or directories. Use -r for recursive directory search.");
		setCommandHelp("stat", "stat <path>...", "Show metadata for each file or directory path.");
		setCommandHelp("tree", "tree [-a] [-L depth] [path]", "Render a directory tree. -a includes dotfiles, -L limits depth.");
		setCommandHelp("cd", "cd <directory>", "Change current working directory. With no args, moves to root '/'.");
		setCommandHelp("pwd", "pwd [-L|-P]", "Print working directory. -P prints normalized physical-style path.");
		setCommandHelp("mkdir", "mkdir [-p] <directory>...", "Create directories. -p creates intermediate directories when needed.");
		setCommandHelp("cat", "cat [-n] [-A] <file>...", "Print file contents. -n adds line numbers, -A renders control characters.");
		setCommandHelp("touch", "touch <file>", "Create an empty file when it does not already exist.");
		setCommandHelp("rm", "rm [-r] [-f] <path>...", "Remove files/dirs. -r recursive delete, -f ignore missing/errors and suppress warnings.");
		setCommandHelp("clear", "clear", "Clear terminal transcript.");
		setCommandHelp("exit", "exit", "Stop the terminal session.");
		setCommandHelp("reboot", "reboot", "Hard reset terminal state and rerun startup flow.");
		setCommandHelp("scrollmode", "scrollmode [NONE|HORIZONTAL|VERTICAL|BOTH]", "Without args shows current scrollbar mode; with arg updates it.");
		setCommandHelp("maskenter", "maskenter [on|off]", "Enable or disable Enter key forwarding to scripts while gfx input masking is active.");
		setCommandHelp("cp", "cp [-r] <source> <destination>", "Copy file or directory. Use -r when source is a directory.");
		setCommandHelp("mv", "mv <source> <destination>", "Move or rename a file path.");
		setCommandHelp("edit", "edit <file> <content>", "Write provided content to a file in one command.");
		setCommandHelp("nano", "nano <file>", "Open file in the in-game editor view.");
		setCommandHelp("run", "run <script> [args...]", "Execute a Lua script in foreground with optional script arguments.");
		setCommandHelp("httpget", "httpget <url> [output-file]", "Fetch HTTP/HTTPS response and print or save it.");
		setCommandHelp("httpput", "httpput [--content-type <mime>] <url> <payload|@file> [output-file]", "Send PUT data and print or save response body.");
		setCommandHelp("pkg", "pkg <search|info|fetch|install|list|remove> ...", "Trusted package manager operations for discovery and install/remove.");
		setCommandHelp("runbg", "runbg <script> [args...]", "Execute a Lua script as a background job.");
		setCommandHelp("jobs", "jobs", "List active and completed background jobs.");
		setCommandHelp("kill", "kill [-TERM|-KILL|-INT|-HUP|-15|-9|-2|-1] <job-id>", "Cancel a background job, optionally with signal semantics.");
	}

	private void setCommandHelp(String name, String usage, String guidance) {
		Command command = commands.get(name);
		if(command != null) {
			command.setHelp(usage, guidance);
		}
	}

	private void deferPromptAfterCommand() {
		promptDeferredByCommand.set(true);
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

	private HttpPutArgs parseHttpPutArgs(List<String> tokens) {
		if(tokens == null) {
			return null;
		}

		HttpPutArgs parsed = new HttpPutArgs();
		List<String> operands = new ArrayList<>();
		for(int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			if(token == null || token.isEmpty()) {
				continue;
			}

			if(operands.isEmpty() && token.startsWith("--content-type")) {
				if("--content-type".equals(token)) {
					if(i + 1 >= tokens.size()) {
						return null;
					}
					++i;
					parsed.contentType = tokens.get(i);
					continue;
				}

				if(token.startsWith("--content-type=")) {
					parsed.contentType = token.substring("--content-type=".length());
					continue;
				}
			}

			operands.add(token);
		}

		if(operands.size() < 2 || operands.size() > 3) {
			return null;
		}

		parsed.url = operands.get(0);
		parsed.payloadArg = operands.get(1);
		parsed.outputPath = operands.size() > 2 ? operands.get(2) : null;
		return parsed;
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

	private String fetchWebData(String rawUrl) {
		if(!ConfigManager.isWebFetchEnabled()) {
			console.print(valueOf("Error: Web fetch is disabled by server config"));
			return null;
		}

		URL url = validateWebUrl(rawUrl, ConfigManager.isWebFetchTrustedDomainsOnly());
		if(url == null) {
			return null;
		}

		return executeWebRequest(url, "GET", null, null, ConfigManager.getWebFetchTimeoutMs(), ConfigManager.getWebFetchMaxBytes(), "web_fetch_max_bytes");
	}

	private String putWebData(String rawUrl, String payload, String contentType) {
		if(!ConfigManager.isWebPutEnabled()) {
			console.print(valueOf("Error: Web PUT is disabled by server config"));
			return null;
		}

		URL url = validateWebUrl(rawUrl, ConfigManager.isWebPutTrustedDomainsOnly());
		if(url == null) {
			return null;
		}

		byte[] requestBytes = (payload == null ? "" : payload).getBytes(StandardCharsets.UTF_8);
		int maxRequestBytes = ConfigManager.getWebPutMaxRequestBytes();
		if(requestBytes.length > maxRequestBytes) {
			console.print(valueOf("Error: Request exceeded web_put_max_request_bytes limit (" + maxRequestBytes + ")"));
			return null;
		}

		return executeWebRequest(url, "PUT", requestBytes, contentType, ConfigManager.getWebPutTimeoutMs(), ConfigManager.getWebPutMaxResponseBytes(), "web_put_max_response_bytes");
	}

	private URL validateWebUrl(String rawUrl, boolean trustedDomainsOnly) {
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

		if(trustedDomainsOnly && !ConfigManager.isTrustedWebDomain(host)) {
			console.print(valueOf("Error: Domain is not in trusted allowlist"));
			return null;
		}

		return url;
	}

	private String executeWebRequest(URL url, String method, byte[] requestBytes, String contentType, int timeoutMs, int maxResponseBytes, String responseLimitConfigName) {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);
			connection.setConnectTimeout(timeoutMs);
			connection.setReadTimeout(timeoutMs);
			connection.setRequestProperty("User-Agent", "LuaMade/1.0");

			if(requestBytes != null) {
				connection.setDoOutput(true);
				String effectiveContentType = (contentType == null || contentType.trim().isEmpty()) ? "text/plain; charset=UTF-8" : contentType.trim();
				connection.setRequestProperty("Content-Type", effectiveContentType);
				connection.setRequestProperty("Content-Length", String.valueOf(requestBytes.length));
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(requestBytes);
				outputStream.flush();
				outputStream.close();
			}

			int status = connection.getResponseCode();
			InputStream input = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
			String responseBody = readResponseWithLimit(input, maxResponseBytes, responseLimitConfigName);
			if(responseBody == null) {
				return null;
			}

			if(status < 200 || status >= 300) {
				if(responseBody.trim().isEmpty()) {
					console.print(valueOf("Error: HTTP status " + status));
				} else {
					console.print(valueOf("Error: HTTP status " + status + " - " + responseBody));
				}
				return null;
			}

			return responseBody;
		} catch(IOException ioException) {
			console.print(valueOf("Error fetching URL: " + ioException.getMessage()));
			return null;
		} finally {
			if(connection != null) {
				connection.disconnect();
			}
		}
	}

	private String readResponseWithLimit(InputStream input, int maxBytes, String configName) throws IOException {
		if(input == null) {
			return "";
		}

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[4096];
		int total = 0;
		while(true) {
			int read = input.read(buffer);
			if(read < 0) {
				break;
			}

			total += read;
			if(total > maxBytes) {
				console.print(valueOf("Error: Response exceeded " + configName + " limit (" + maxBytes + ")"));
				return null;
			}

			output.write(buffer, 0, read);
		}

		return output.toString(StandardCharsets.UTF_8.name());
	}

	private enum ChainCondition {
		ALWAYS,
		ON_SUCCESS,
		ON_FAILURE
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
		CANCELED
	}

	private static final class QueuedCommand {
		private final String commandText;
		private final ChainCondition condition;

		private QueuedCommand(String commandText, ChainCondition condition) {
			this.commandText = commandText;
			this.condition = condition == null ? ChainCondition.ALWAYS : condition;
		}

		private boolean shouldRun(boolean previousSucceeded) {
			switch(condition) {
				case ON_SUCCESS:
					return previousSucceeded;
				case ON_FAILURE:
					return !previousSucceeded;
				case ALWAYS:
				default:
					return true;
			}
		}
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
		private final List<String> filePaths = new ArrayList<>();
		private boolean showLineNumbers;
		private boolean showAllChars;
	}

	private static final class WcArgs {
		private final List<String> filePaths = new ArrayList<>();
		private boolean showLines;
		private boolean showWords;
		private boolean showBytes;
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

	private static final class HttpPutArgs {
		private String url;
		private String payloadArg;
		private String outputPath;
		private String contentType;
	}

	/**
	 * Command interface
	 */
	private abstract static class Command {
		private final String name;
		private final String description;
		private String usage;
		private String guidance;

		protected Command(String name, String description) {
			this(name, description, name + " [args]", "");
		}

		protected Command(String name, String description, String usage, String guidance) {
			this.name = name;
			this.description = description;
			this.usage = usage == null || usage.trim().isEmpty() ? name + " [args]" : usage.trim();
			this.guidance = guidance == null ? "" : guidance.trim();
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public String getUsage() {
			return usage;
		}

		public String getGuidance() {
			return guidance;
		}

		public boolean hasGuidance() {
			return guidance != null && !guidance.isEmpty();
		}

		public void setHelp(String usage, String guidance) {
			if(usage != null && !usage.trim().isEmpty()) {
				this.usage = usage.trim();
			}
			this.guidance = guidance == null ? "" : guidance.trim();
		}

		public abstract void execute(String args);
	}

	/**
	 * Lua command implementation
	 */
	private static class LuaCommand extends Command {
		private final LuaValue callback;

		public LuaCommand(String name, LuaValue callback) {
			super(name, "User-defined command", name + " [args]", "Registered at runtime via term.registerCommand(name, callback).");
			this.callback = callback;
		}

		@Override
		public void execute(String args) {
			callback.call(valueOf(args));
		}
	}

	private static final class BackgroundJob {
		private final int id;
		private final String scriptPath;
		private final ScriptExecutionContext context;
		private volatile JobStatus status = JobStatus.RUNNING;
		private volatile Future<Boolean> future;

		private BackgroundJob(int id, String scriptPath, ScriptExecutionContext context) {
			this.id = id;
			this.scriptPath = scriptPath;
			this.context = context;
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

		private ScriptExecutionContext getContext() {
			return context;
		}

		private Future<Boolean> getFuture() {
			return future;
		}

		private void setFuture(Future<Boolean> future) {
			this.future = future;
		}
	}

	private static final class ScriptExecutionContext {
		private final AtomicBoolean cancellationRequested = new AtomicBoolean(false);
		private volatile Thread workerThread;

		private void bindToCurrentThread() {
			workerThread = Thread.currentThread();
		}

		private void requestCancel() {
			cancellationRequested.set(true);
			Thread runningThread = workerThread;
			if(runningThread != null) {
				runningThread.interrupt();
			}
		}

		private boolean isCancellationRequested() {
			return cancellationRequested.get();
		}

		private void throwIfCancellationRequested() {
			if(cancellationRequested.get()) {
				throw new LuaError("Script canceled");
			}
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

	private class LuaWrappedCommand extends Command {
		private final Command original;
		private final LuaValue wrapper;

		private LuaWrappedCommand(String name, Command original, LuaValue wrapper) {
			super(name, "Wrapped command", original.getUsage(), original.hasGuidance() ? original.getGuidance() : "Wrapped command; behavior may be extended by Lua callback.");
			this.original = original;
			this.wrapper = wrapper;
		}

		@Override
		public void execute(String args) {
			LuaValue next = new OneArgFunction() {
				@Override
				public LuaValue call(LuaValue nextArgs) {
					String effectiveArgs = nextArgs.isnil() ? args : nextArgs.tojstring();
					original.execute(effectiveArgs);
					return TRUE;
				}
			};

			try {
				wrapper.call(valueOf(args), next);
			} catch(LuaError error) {
				String message = error.getMessage();
				if(message == null || message.trim().isEmpty()) {
					message = error.toString();
				}
				console.print(valueOf("Lua error in wrapped command '" + getName() + "': " + message));
			}
		}
	}
}