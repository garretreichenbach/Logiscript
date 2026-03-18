package luamade.manager;

import api.utils.simpleconfig.SimpleConfigBool;
import api.utils.simpleconfig.SimpleConfigContainer;
import api.utils.simpleconfig.SimpleConfigDouble;
import api.utils.simpleconfig.SimpleConfigInt;
import luamade.LuaMade;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
	private static SimpleConfigBool webFetchEnabled;
	private static SimpleConfigBool webFetchTrustedDomainsOnly;
	private static SimpleConfigInt webFetchTimeoutMs;
	private static SimpleConfigInt webFetchMaxBytes;
	private static final List<String> DEFAULT_TRUSTED_WEB_DOMAINS = Arrays.asList(
		"raw.githubusercontent.com",
		"gist.githubusercontent.com",
		"pastebin.com",
		"hastebin.com"
	);
	private static final Path TRUSTED_WEB_DOMAINS_PATH = Paths.get("config", "luamade", "trusted_domains.txt");
	private static volatile Set<String> trustedWebDomainsCache = new LinkedHashSet<>(DEFAULT_TRUSTED_WEB_DOMAINS);

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
		webFetchEnabled = new SimpleConfigBool(config, "web_fetch_enabled", false, "If true, allows terminal/scripts to fetch HTTP(S) data from the web.");
		webFetchTrustedDomainsOnly = new SimpleConfigBool(config, "web_fetch_trusted_domains_only", true, "If true, web fetch is limited to a built-in trusted domain allowlist.");
		webFetchTimeoutMs = new SimpleConfigInt(config, "web_fetch_timeout_ms", 4000, "Web fetch connect/read timeout in milliseconds.");
		webFetchMaxBytes = new SimpleConfigInt(config, "web_fetch_max_bytes", 131072, "Maximum response payload size (bytes) accepted by web fetch.");

		config.readWriteFields();
		ensureTrustedDomainsFileExists(instance);
		reloadTrustedWebDomains(instance);
		if(isDebugMode()) {
			String mode = config.isServer() ? "server" : (config.local ? "client-local" : "client-synced");
			instance.logInfo("Config initialized via SimpleConfigContainer (mode=" + mode + ")");
		}
	}

	public static void reload() {
		if(config != null) {
			config.readFields();
		}
		reloadTrustedWebDomains(LuaMade.getInstance());
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

	public static boolean isWebFetchEnabled() {
		return boolOrDefault(webFetchEnabled, false);
	}

	public static boolean isWebFetchTrustedDomainsOnly() {
		return boolOrDefault(webFetchTrustedDomainsOnly, true);
	}

	public static int getWebFetchTimeoutMs() {
		return clampInt(intOrDefault(webFetchTimeoutMs, 4000), 250, 30000);
	}

	public static int getWebFetchMaxBytes() {
		return clampInt(intOrDefault(webFetchMaxBytes, 131072), 1024, 1048576);
	}

	public static Set<String> getTrustedWebDomains() {
		return new LinkedHashSet<>(trustedWebDomainsCache);
	}

	public static boolean isTrustedWebDomain(String host) {
		if(host == null || host.trim().isEmpty()) {
			return false;
		}

		String normalizedHost = host.toLowerCase(Locale.ROOT).trim();
		for(String domain : trustedWebDomainsCache) {
			if(normalizedHost.equals(domain) || normalizedHost.endsWith("." + domain)) {
				return true;
			}
		}
		return false;
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

	private static void ensureTrustedDomainsFileExists(LuaMade instance) {
		if(Files.exists(TRUSTED_WEB_DOMAINS_PATH)) {
			return;
		}

		try {
			if(TRUSTED_WEB_DOMAINS_PATH.getParent() != null) {
				Files.createDirectories(TRUSTED_WEB_DOMAINS_PATH.getParent());
			}

			StringBuilder builder = new StringBuilder();
			builder.append("# LuaMade trusted web domains\n");
			builder.append("# One domain per line. Subdomains are allowed automatically.\n");
			builder.append("# Example: raw.githubusercontent.com\n\n");
			for(String domain : DEFAULT_TRUSTED_WEB_DOMAINS) {
				builder.append(domain).append('\n');
			}

			Files.write(
				TRUSTED_WEB_DOMAINS_PATH,
				builder.toString().getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			);
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to create trusted domains file: " + TRUSTED_WEB_DOMAINS_PATH, exception);
			}
		}
	}

	private static void reloadTrustedWebDomains(LuaMade instance) {
		Set<String> loaded = new LinkedHashSet<>();
		try {
			if(Files.exists(TRUSTED_WEB_DOMAINS_PATH)) {
				for(String line : Files.readAllLines(TRUSTED_WEB_DOMAINS_PATH, StandardCharsets.UTF_8)) {
					String domain = line.trim().toLowerCase(Locale.ROOT);
					if(domain.isEmpty() || domain.startsWith("#")) {
						continue;
					}
					loaded.add(domain);
				}
			}
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to read trusted domains file: " + TRUSTED_WEB_DOMAINS_PATH, exception);
			}
		}

		if(loaded.isEmpty()) {
			loaded.addAll(DEFAULT_TRUSTED_WEB_DOMAINS);
		}

		trustedWebDomainsCache = loaded;
	}
}
