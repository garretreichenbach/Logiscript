package luamade.manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigDouble;
import api.utils.simpleconfig.SimpleConfigInt;
import luamade.LuaMade;

public final class ConfigManager {

	private static SimpleConfigContainer config;

	private static SimpleConfigBool debugMode;
	private static SimpleConfigInt consoleCharacterLimit;
	private static SimpleConfigInt consoleLineLimit;
	private static SimpleConfigInt scriptMaxParallel;
	private static SimpleConfigInt scriptTimeoutMs;
	private static SimpleConfigInt startupScriptTimeoutMs;
	private static SimpleConfigInt scriptQueueWaitMs;
	private static SimpleConfigInt scriptOverloadMode;

	private ConfigManager() {
	}

	public static void initialize(LuaMade instance) {
		config = new SimpleConfigContainer(instance, "config", false);

		debugMode = new SimpleConfigBool(config, "debug_mode", false, "If true, enables debug logging and features.");
		consoleCharacterLimit = new SimpleConfigInt(config, "console_character_limit", 30000, "Maximum number of characters retained in the computer terminal UI.");
		consoleLineLimit = new SimpleConfigInt(config, "console_line_limit", 1000, "Maximum number of lines retained in the computer terminal UI.");
		scriptMaxParallel = new SimpleConfigInt(config, "script_max_parallel", 2, "Maximum number of Lua scripts that may run at once per computer.");
		scriptTimeoutMs = new SimpleConfigInt(config, "script_timeout_ms", 5000, "Foreground/background script timeout in milliseconds.");
		startupScriptTimeoutMs = new SimpleConfigInt(config, "startup_script_timeout_ms", 2000, "Startup script timeout in milliseconds.");
		scriptQueueWaitMs = new SimpleConfigInt(config, "script_queue_wait_ms", 250, "Queue wait budget in milliseconds used by hybrid overload mode.");
		scriptOverloadMode = new SimpleConfigInt(config, "script_overload_mode", 2, "Script overload policy: 0=hard-stop, 1=stall, 2=hybrid.");

		config.readWriteFields();
		if(isDebugMode()) {
			String mode = config.isServer() ? "server" : (config.local ? "client-local" : "client-synced");
			instance.logInfo("Config initialized via SimpleConfigContainer (mode=" + mode + ")");
		}
	}

	public static void reload() {
		if(config != null) {
			config.readFields();
		}
	}

	public static boolean isDebugMode() {
		return boolOrDefault(debugMode, false);
	}

	public static int getConsoleCharacterLimit() {
		return clampInt(intOrDefault(consoleCharacterLimit, 30000), 1000, 200000);
	}

	public static int getConsoleLineLimit() {
		return clampInt(intOrDefault(consoleLineLimit, 1000), 100, 10000);
	}

	public static int getScriptMaxParallel() {
		return clampInt(intOrDefault(scriptMaxParallel, 2), 1, 8);
	}

	public static int getScriptTimeoutMs() {
		return clampInt(intOrDefault(scriptTimeoutMs, 5000), 100, 120000);
	}

	public static int getStartupScriptTimeoutMs() {
		return clampInt(intOrDefault(startupScriptTimeoutMs, 2000), 100, 60000);
	}

	public static int getScriptQueueWaitMs() {
		return clampInt(intOrDefault(scriptQueueWaitMs, 250), 0, 60000);
	}

	public static int getScriptOverloadMode() {
		return clampInt(intOrDefault(scriptOverloadMode, 2), 0, 2);
	}


	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static boolean boolOrDefault(SimpleConfigBool entry, boolean defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}

	private static int intOrDefault(SimpleConfigInt entry, int defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}

	private static double doubleOrDefault(SimpleConfigDouble entry, double defaultValue) {
		if(entry == null || entry.getValue() == null) {
			return defaultValue;
		}
		return entry.getValue();
	}
}
