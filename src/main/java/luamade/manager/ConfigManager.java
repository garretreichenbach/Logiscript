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
import java.util.*;
import java.util.regex.Pattern;

public final class ConfigManager {

	private static SimpleConfigContainer config;

	private static SimpleConfigBool debugMode;
	private static SimpleConfigInt consoleCharacterLimit;
	private static SimpleConfigInt consoleLineLimit;
	private static SimpleConfigInt scriptMaxParallel;
	private static SimpleConfigInt scriptQueueWaitMs;
	private static SimpleConfigInt scriptOverloadMode;
	private static SimpleConfigBool webFetchEnabled;
	private static SimpleConfigBool webFetchTrustedDomainsOnly;
	private static SimpleConfigInt webFetchTimeoutMs;
	private static SimpleConfigInt webFetchMaxBytes;
	private static SimpleConfigBool webPutEnabled;
	private static SimpleConfigBool webPutTrustedDomainsOnly;
	private static SimpleConfigInt webPutTimeoutMs;
	private static SimpleConfigInt webPutMaxRequestBytes;
	private static SimpleConfigInt webPutMaxResponseBytes;
	private static SimpleConfigBool packageManagerEnabled;
	private static SimpleConfigBool packageManagerTrustedDomainsOnly;
	private static SimpleConfigInt packageManagerTimeoutMs;
	private static SimpleConfigInt packageManagerMaxBytes;
	private static SimpleConfigInt gfxMaxCommandsPerLayer;
	private static SimpleConfigInt gfxMaxLayers;
	private static SimpleConfigBool dockingRequirePermissions;
	private static SimpleConfigBool dockingAllowFriendFactions;
	private static SimpleConfigInt dockingSnapRadius;

	private static final List<String> DEFAULT_TRUSTED_WEB_DOMAINS = Arrays.asList(
		"raw.githubusercontent.com",
		"gist.githubusercontent.com",
			"github.com",
		"pastebin.com",
		"hastebin.com"
	);
	private static final Path TRUSTED_WEB_DOMAINS_PATH = Paths.get("config", "luamade", "trusted_domains.txt");
	private static volatile Set<String> trustedWebDomainsCache = new LinkedHashSet<>(DEFAULT_TRUSTED_WEB_DOMAINS);
	private static final List<String> DEFAULT_ALLOWED_LUA_PACKAGES = Arrays.asList(
			"json",
			"util",
			"vector"
	);
	private static final Path ALLOWED_LUA_PACKAGES_PATH = Paths.get("config", "luamade", "allowed_lua_packages.txt");
	private static final String DEFAULT_PACKAGE_MANAGER_BASE_URL = "https://packages.luamade.net";
	private static final Path PACKAGE_MANAGER_BASE_URL_PATH = Paths.get("config", "luamade", "package_manager_base_url.txt");
	private static final Pattern SAFE_LUA_MODULE_PATTERN = Pattern.compile("[a-z0-9_]+(?:\\.[a-z0-9_]+)*");
	private static volatile Set<String> allowedLuaPackagesCache = new LinkedHashSet<>(DEFAULT_ALLOWED_LUA_PACKAGES);
	private static volatile String packageManagerBaseUrlCache = DEFAULT_PACKAGE_MANAGER_BASE_URL;
	private static final String DEFAULT_EDITOR_THEME = "dark";
	private static final Path EDITOR_THEME_PATH = Paths.get("config", "luamade", "editor_theme.txt");
	private static volatile String editorThemeCache = DEFAULT_EDITOR_THEME;
	private static final int DEFAULT_EDITOR_FONT_SIZE = 14;
	private static final int MIN_EDITOR_FONT_SIZE = 8;
	private static final int MAX_EDITOR_FONT_SIZE = 48;
	private static final Path EDITOR_FONT_SIZE_PATH = Paths.get("config", "luamade", "editor_font_size.txt");
	private static volatile int editorFontSizeCache = DEFAULT_EDITOR_FONT_SIZE;

	private ConfigManager() {
	}

	public static void initialize(LuaMade instance) {
		config = new SimpleConfigContainer(instance, "config", false);

		debugMode = new SimpleConfigBool(config, "debug_mode", false, "If true, enables debug logging and features.");
		consoleCharacterLimit = new SimpleConfigInt(config, "console_character_limit", 30000, "Maximum number of characters retained in the computer terminal UI.");
		consoleLineLimit = new SimpleConfigInt(config, "console_line_limit", 1000, "Maximum number of lines retained in the computer terminal UI.");
		scriptMaxParallel = new SimpleConfigInt(config, "script_max_parallel", 2, "Maximum number of Lua scripts that may run at once per computer.");
		scriptQueueWaitMs = new SimpleConfigInt(config, "script_queue_wait_ms", 250, "Queue wait budget in milliseconds used by hybrid overload mode.");
		scriptOverloadMode = new SimpleConfigInt(config, "script_overload_mode", 2, "Script overload policy: 0=hard-stop, 1=stall, 2=hybrid.");
		webFetchEnabled = new SimpleConfigBool(config, "web_fetch_enabled", true, "If true, allows terminal/scripts to fetch HTTP(S) data from the web.");
		webFetchTrustedDomainsOnly = new SimpleConfigBool(config, "web_fetch_trusted_domains_only", true, "If true, web fetch is limited to a built-in trusted domain allowlist.");
		webFetchTimeoutMs = new SimpleConfigInt(config, "web_fetch_timeout_ms", 4000, "Web fetch connect/read timeout in milliseconds.");
		webFetchMaxBytes = new SimpleConfigInt(config, "web_fetch_max_bytes", 1048576, "Maximum response payload size (bytes) accepted by web fetch.");
		webPutEnabled = new SimpleConfigBool(config, "web_put_enabled", true, "If true, allows terminal/scripts to send HTTP(S) PUT requests.");
		webPutTrustedDomainsOnly = new SimpleConfigBool(config, "web_put_trusted_domains_only", true, "If true, HTTP PUT is limited to domains in trusted_domains.txt.");
		webPutTimeoutMs = new SimpleConfigInt(config, "web_put_timeout_ms", 4000, "HTTP PUT connect/read timeout in milliseconds.");
		webPutMaxRequestBytes = new SimpleConfigInt(config, "web_put_max_request_bytes", 32768, "Maximum UTF-8 request payload size (bytes) for HTTP PUT.");
		webPutMaxResponseBytes = new SimpleConfigInt(config, "web_put_max_response_bytes", 1048576, "Maximum response payload size (bytes) accepted by HTTP PUT.");
		packageManagerEnabled = new SimpleConfigBool(config, "package_manager_enabled", false, "If true, enables trusted package manager commands.");
		packageManagerTrustedDomainsOnly = new SimpleConfigBool(config, "package_manager_trusted_domains_only", true, "If true, package artifacts are restricted to trusted domains.");
		packageManagerTimeoutMs = new SimpleConfigInt(config, "package_manager_timeout_ms", 5000, "Package manager connect/read timeout in milliseconds.");
		packageManagerMaxBytes = new SimpleConfigInt(config, "package_manager_max_bytes", 2097152, "Maximum package response payload size in bytes.");
		gfxMaxCommandsPerLayer = new SimpleConfigInt(config, "gfx_max_commands_per_layer", 4096, "Maximum number of queued draw commands in a single gfx2d layer.");
		gfxMaxLayers = new SimpleConfigInt(config, "gfx_max_layers", 32, "Maximum number of gfx2d layers per computer.");
		dockingRequirePermissions = new SimpleConfigBool(config, "docking_require_permissions", true, "If true, Lua docking helpers require same-faction or friend-faction permissions.");
		dockingAllowFriendFactions = new SimpleConfigBool(config, "docking_allow_friend_factions", true, "If true, docking permission checks allow faction friends in addition to same faction.");
		dockingSnapRadius = new SimpleConfigInt(config, "docking_snap_radius", 5, "Maximum block distance used by Lua docking helpers when snapping to rail targets.");

		config.readWriteFields();
		ensureTrustedDomainsFileExists(instance);
		ensureAllowedLuaPackagesFileExists(instance);
		ensurePackageManagerBaseUrlFileExists(instance);
		ensureEditorThemeFileExists(instance);
		ensureEditorFontSizeFileExists(instance);
		reloadTrustedWebDomains(instance);
		reloadAllowedLuaPackages(instance);
		reloadPackageManagerBaseUrl(instance);
		reloadEditorTheme(instance);
		reloadEditorFontSize(instance);
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
		reloadAllowedLuaPackages(LuaMade.getInstance());
		reloadPackageManagerBaseUrl(LuaMade.getInstance());
		reloadEditorTheme(LuaMade.getInstance());
		reloadEditorFontSize(LuaMade.getInstance());
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


	public static int getScriptQueueWaitMs() {
		return clampInt(intOrDefault(scriptQueueWaitMs, 250), 0, 60000);
	}

	public static int getScriptOverloadMode() {
		return clampInt(intOrDefault(scriptOverloadMode, 2), 0, 2);
	}

	public static boolean isWebFetchEnabled() {
		return boolOrDefault(webFetchEnabled, true);
	}

	public static boolean isWebFetchTrustedDomainsOnly() {
		return boolOrDefault(webFetchTrustedDomainsOnly, true);
	}

	public static int getWebFetchTimeoutMs() {
		return clampInt(intOrDefault(webFetchTimeoutMs, 4000), 250, 30000);
	}

	public static int getWebFetchMaxBytes() {
		return clampInt(intOrDefault(webFetchMaxBytes, 1048576), 4096, 1048576);
	}

	public static boolean isWebPutEnabled() {
		return boolOrDefault(webPutEnabled, true);
	}

	public static boolean isWebPutTrustedDomainsOnly() {
		return boolOrDefault(webPutTrustedDomainsOnly, true);
	}

	public static int getWebPutTimeoutMs() {
		return clampInt(intOrDefault(webPutTimeoutMs, 4000), 250, 30000);
	}

	public static int getWebPutMaxRequestBytes() {
		return clampInt(intOrDefault(webPutMaxRequestBytes, 32768), 256, 1048576);
	}

	public static int getWebPutMaxResponseBytes() {
		return clampInt(intOrDefault(webPutMaxResponseBytes, 1048576), 4096, 1048576);
	}

	public static boolean isPackageManagerEnabled() {
		return boolOrDefault(packageManagerEnabled, false);
	}

	public static boolean isPackageManagerTrustedDomainsOnly() {
		return boolOrDefault(packageManagerTrustedDomainsOnly, true);
	}

	public static int getPackageManagerTimeoutMs() {
		return clampInt(intOrDefault(packageManagerTimeoutMs, 5000), 250, 30000);
	}

	public static int getPackageManagerMaxBytes() {
		return clampInt(intOrDefault(packageManagerMaxBytes, 2097152), 4096, 8 * 1024 * 1024);
	}

	public static String getPackageManagerBaseUrl() {
		return packageManagerBaseUrlCache;
	}

	public static int getGfxMaxCommandsPerLayer() {
		return clampInt(intOrDefault(gfxMaxCommandsPerLayer, 4096), 64, 32768);
	}

	public static int getGfxMaxLayers() {
		return clampInt(intOrDefault(gfxMaxLayers, 32), 1, 256);
	}

	public static boolean isDockingPermissionRequired() {
		return boolOrDefault(dockingRequirePermissions, true);
	}

	public static boolean isDockingFriendFactionsAllowed() {
		return boolOrDefault(dockingAllowFriendFactions, true);
	}

	public static int getDockingSnapRadius() {
		return clampInt(intOrDefault(dockingSnapRadius, 5), 1, 50);
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

	public static Set<String> getAllowedLuaPackages() {
		return new LinkedHashSet<>(allowedLuaPackagesCache);
	}

	public static boolean isAllowedLuaPackage(String moduleName) {
		String normalizedModuleName = normalizeLuaModuleName(moduleName);
		return normalizedModuleName != null && allowedLuaPackagesCache.contains(normalizedModuleName);
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

	private static void ensureAllowedLuaPackagesFileExists(LuaMade instance) {
		if(Files.exists(ALLOWED_LUA_PACKAGES_PATH)) {
			return;
		}

		try {
			if(ALLOWED_LUA_PACKAGES_PATH.getParent() != null) {
				Files.createDirectories(ALLOWED_LUA_PACKAGES_PATH.getParent());
			}

			StringBuilder builder = new StringBuilder();
			builder.append("# LuaMade allowed Lua package names\n");
			builder.append("# One module per line using dot notation (for example: util or mylib.math).\n");
			builder.append("# Module names are lowercase and limited to [a-z0-9_] segments.\n\n");
			for(String module : DEFAULT_ALLOWED_LUA_PACKAGES) {
				builder.append(module).append('\n');
			}

			Files.write(
					ALLOWED_LUA_PACKAGES_PATH,
					builder.toString().getBytes(StandardCharsets.UTF_8),
					StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING,
					StandardOpenOption.WRITE
			);
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to create allowed Lua packages file: " + ALLOWED_LUA_PACKAGES_PATH, exception);
			}
		}
	}

	private static void ensurePackageManagerBaseUrlFileExists(LuaMade instance) {
		if(Files.exists(PACKAGE_MANAGER_BASE_URL_PATH)) {
			return;
		}

		try {
			if(PACKAGE_MANAGER_BASE_URL_PATH.getParent() != null) {
				Files.createDirectories(PACKAGE_MANAGER_BASE_URL_PATH.getParent());
			}

			String builder = "# LuaMade package registry base URL\n" +
					"# Example: https://packages.luamade.net\n" +
					"# This is used by the terminal pkg command.\n\n" +
					DEFAULT_PACKAGE_MANAGER_BASE_URL + '\n';

			Files.write(
				PACKAGE_MANAGER_BASE_URL_PATH,
					builder.getBytes(StandardCharsets.UTF_8),
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			);
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to create package manager base URL file: " + PACKAGE_MANAGER_BASE_URL_PATH, exception);
			}
		}
	}

	private static void reloadPackageManagerBaseUrl(LuaMade instance) {
		String loadedUrl = DEFAULT_PACKAGE_MANAGER_BASE_URL;
		try {
			if(Files.exists(PACKAGE_MANAGER_BASE_URL_PATH)) {
				for(String line : Files.readAllLines(PACKAGE_MANAGER_BASE_URL_PATH, StandardCharsets.UTF_8)) {
					String value = line == null ? "" : line.trim();
					if(value.isEmpty() || value.startsWith("#")) {
						continue;
					}
					loadedUrl = value;
					break;
				}
			}
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to read package manager base URL file: " + PACKAGE_MANAGER_BASE_URL_PATH, exception);
			}
		}

		packageManagerBaseUrlCache = loadedUrl;
	}

	private static void reloadAllowedLuaPackages(LuaMade instance) {
		Set<String> loaded = new LinkedHashSet<>();
		try {
			if(Files.exists(ALLOWED_LUA_PACKAGES_PATH)) {
				for(String line : Files.readAllLines(ALLOWED_LUA_PACKAGES_PATH, StandardCharsets.UTF_8)) {
					String moduleName = normalizeLuaModuleName(line);
					if(moduleName == null) {
						continue;
					}
					loaded.add(moduleName);
				}
			}
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to read allowed Lua packages file: " + ALLOWED_LUA_PACKAGES_PATH, exception);
			}
		}

		if(loaded.isEmpty()) {
			loaded.addAll(DEFAULT_ALLOWED_LUA_PACKAGES);
		}

		allowedLuaPackagesCache = loaded;
	}

	public static String getEditorTheme() {
		return editorThemeCache;
	}

	public static boolean isEditorDarkTheme() {
		return "dark".equalsIgnoreCase(editorThemeCache);
	}

	public static void setEditorTheme(String theme) {
		String normalized = theme == null ? DEFAULT_EDITOR_THEME : theme.trim().toLowerCase(Locale.ROOT);
		if(!"dark".equals(normalized) && !"light".equals(normalized)) {
			normalized = DEFAULT_EDITOR_THEME;
		}
		editorThemeCache = normalized;
		saveEditorTheme();
	}

	private static void saveEditorTheme() {
		try {
			if(EDITOR_THEME_PATH.getParent() != null) {
				Files.createDirectories(EDITOR_THEME_PATH.getParent());
			}
			String content = "# LuaMade Swing editor theme\n# Options: dark, light\n\n" + editorThemeCache + '\n';
			Files.write(EDITOR_THEME_PATH, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to save editor theme file: " + EDITOR_THEME_PATH, exception);
		}
	}

	private static void ensureEditorThemeFileExists(LuaMade instance) {
		if(Files.exists(EDITOR_THEME_PATH)) {
			return;
		}
		try {
			if(EDITOR_THEME_PATH.getParent() != null) {
				Files.createDirectories(EDITOR_THEME_PATH.getParent());
			}
			String content = "# LuaMade Swing editor theme\n# Options: dark, light\n\n" + DEFAULT_EDITOR_THEME + '\n';
			Files.write(EDITOR_THEME_PATH, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to create editor theme file: " + EDITOR_THEME_PATH, exception);
			}
		}
	}

	private static void reloadEditorTheme(LuaMade instance) {
		String loaded = DEFAULT_EDITOR_THEME;
		try {
			if(Files.exists(EDITOR_THEME_PATH)) {
				for(String line : Files.readAllLines(EDITOR_THEME_PATH, StandardCharsets.UTF_8)) {
					String value = line == null ? "" : line.trim().toLowerCase(Locale.ROOT);
					if(value.isEmpty() || value.startsWith("#")) {
						continue;
					}
					if("dark".equals(value) || "light".equals(value)) {
						loaded = value;
					}
					break;
				}
			}
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to read editor theme file: " + EDITOR_THEME_PATH, exception);
			}
		}
		editorThemeCache = loaded;
	}

	public static int getEditorFontSize() {
		return editorFontSizeCache;
	}

	public static void setEditorFontSize(int size) {
		editorFontSizeCache = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, size));
		saveEditorFontSize();
	}

	private static void saveEditorFontSize() {
		try {
			if(EDITOR_FONT_SIZE_PATH.getParent() != null) {
				Files.createDirectories(EDITOR_FONT_SIZE_PATH.getParent());
			}
			String content = "# LuaMade Swing editor font size\n# Range: " + MIN_EDITOR_FONT_SIZE + "-" + MAX_EDITOR_FONT_SIZE + "\n\n" + editorFontSizeCache + '\n';
			Files.write(EDITOR_FONT_SIZE_PATH, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to save editor font size file: " + EDITOR_FONT_SIZE_PATH, exception);
		}
	}

	private static void ensureEditorFontSizeFileExists(LuaMade instance) {
		if(Files.exists(EDITOR_FONT_SIZE_PATH)) {
			return;
		}
		try {
			if(EDITOR_FONT_SIZE_PATH.getParent() != null) {
				Files.createDirectories(EDITOR_FONT_SIZE_PATH.getParent());
			}
			String content = "# LuaMade Swing editor font size\n# Range: " + MIN_EDITOR_FONT_SIZE + "-" + MAX_EDITOR_FONT_SIZE + "\n\n" + DEFAULT_EDITOR_FONT_SIZE + '\n';
			Files.write(EDITOR_FONT_SIZE_PATH, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to create editor font size file: " + EDITOR_FONT_SIZE_PATH, exception);
			}
		}
	}

	private static void reloadEditorFontSize(LuaMade instance) {
		int loaded = DEFAULT_EDITOR_FONT_SIZE;
		try {
			if(Files.exists(EDITOR_FONT_SIZE_PATH)) {
				for(String line : Files.readAllLines(EDITOR_FONT_SIZE_PATH, StandardCharsets.UTF_8)) {
					String value = line == null ? "" : line.trim();
					if(value.isEmpty() || value.startsWith("#")) {
						continue;
					}
					try {
						int parsed = Integer.parseInt(value);
						loaded = Math.max(MIN_EDITOR_FONT_SIZE, Math.min(MAX_EDITOR_FONT_SIZE, parsed));
					} catch(NumberFormatException ignored) {
					}
					break;
				}
			}
		} catch(IOException exception) {
			if(instance != null) {
				instance.logException("Failed to read editor font size file: " + EDITOR_FONT_SIZE_PATH, exception);
			}
		}
		editorFontSizeCache = loaded;
	}

	private static String normalizeLuaModuleName(String rawModuleName) {
		if(rawModuleName == null) {
			return null;
		}

		String trimmed = rawModuleName.trim().toLowerCase(Locale.ROOT);
		if(trimmed.isEmpty() || trimmed.startsWith("#")) {
			return null;
		}

		if(!SAFE_LUA_MODULE_PATTERN.matcher(trimmed).matches()) {
			return null;
		}

		return trimmed;
	}
}
