package luamade.lua.fs;

import luamade.LuaMade;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import luamade.utils.CompressionUtils;
import luamade.utils.DataUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

/**
 * Implements a Unix-like file system for computers in the game. This allows scripts to create, read, write, and delete files.
 * <br/>Each FileSystem is compressed and serialized to a single file on disk named "computer_<UUID>.fs.smdat".
 * <br/>This ensures that the data takes up minimal space and is easy to manage.
 * <br/>File Systems are cached in memory for quick access, but are saved to disk when the computer is off or has been idle for a while.
 * <br/>When a computer is in this state, it is marked as IDLE and has to be loaded back into memory when a player interacts with it again.
 */
public class FileSystem extends LuaMadeUserdata {

	//Todo: This might be too generous idk
	public static int MAX_FS_SIZE = 2 * 1024 * 1024; //2MB

	private static final File computerStorage = resolveComputerStorage();
	private static volatile boolean storagePathLogged;
	private static final String STARTUP_SCRIPT_PATH = "/etc/startup.lua";
	private static final String STARTUP_BACKUP_PATH = "/etc/startup.lua.bak";
	private static final String PERMISSIONS_FILE_PATH = "/etc/.fs_permissions";
	private static final String STARTUP_MANAGED_HEADER = "-- /etc/startup.lua";
	private static final String STARTUP_BANNER_LINE = "print(\"LuaMade Terminal v1.0\")";
	private static final String STARTUP_HELP_LINE = "print(\"Type 'help' for a list of commands\")";
	private static final String LEGACY_STARTUP_SCRIPT_V1 =
		"-- /etc/startup.lua\n" +
		"-- Executed whenever the terminal boots or when you run: reboot\n" +
		"-- Prompt placeholders: {name}, {display}, {hostname}, {dir}\n" +
		"\n" +
		"term.setAutoPrompt(true)\n" +
		"term.setPromptTemplate(\"{name}:{dir} $ \")\n" +
		"\n" +
		"print(\"LuaMade Terminal v1.0\")\n" +
		"print(\"Type 'help' for a list of commands\")";
	private VirtualFile rootDirectory;
	private VirtualFile currentDirectory;
	private final String computerUUID;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	private final List<PermissionRule> permissionRules = new ArrayList<>();
	private final Map<String, Boolean> unlockedScopes = new HashMap<>();
	private String lastError = "";

	public FileSystem(ComputerModule module) {
		computerUUID = module.getUUID();
		if(!computerStorage.exists()) {
			computerStorage.mkdirs();
		}
		if(!readFilesFromDisk(module)) {
			initializeDefaultDirectories();
		}
		ensureDefaultStructure();
		loadPermissionRules();
	}

	private void clearLastError() {
		lastError = "";
	}

	public static FileSystem initNewFileSystem(ComputerModule module) {
		logStoragePathOnce();
		if(!computerStorage.exists()) {
			computerStorage.mkdirs();
		}
		return new FileSystem(module);
	}

	private static File resolveComputerStorage() {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath != null && !worldDataPath.trim().isEmpty()) {
			return new File(worldDataPath, "computers");
		}

		// Client contexts may not expose server world paths. Use a stable mod-local fallback.
		String fallbackRoot = DataUtils.getResourcesPath() + "/data/luamade-local";
		return new File(fallbackRoot, "computers");
	}

	private static void logStoragePathOnce() {
		if(storagePathLogged) {
			return;
		}

		synchronized(FileSystem.class) {
			if(storagePathLogged) {
				return;
			}
			storagePathLogged = true;

			String worldDataPath = DataUtils.getWorldDataPath();
			if(worldDataPath == null || worldDataPath.trim().isEmpty()) {
				LuaMade.getInstance().logWarning("World data path unavailable; using fallback computer storage: " + computerStorage.getAbsolutePath());
			} else {
				LuaMade.getInstance().logInfo("Using computer storage: " + computerStorage.getAbsolutePath());
			}
		}
	}

	@LuaMadeCallable
	public String getLastError() {
		return lastError;
	}

	private void setLastError(String message) {
		lastError = message == null ? "" : message;
	}

	@LuaMadeCallable
	public void clearLastErrorState() {
		clearLastError();
	}

	@LuaMadeCallable
	public boolean protect(String path, String password) {
		return protect(path, password, "all");
	}

	@LuaMadeCallable
	public boolean protect(String path, String password, String operationsText) {
		if(path == null || path.trim().isEmpty()) {
			setLastError("Usage: protect(path, password, [operations])");
			return false;
		}
		if(password == null || password.isEmpty()) {
			setLastError("Password must not be empty");
			return false;
		}

		EnumSet<PermissionOperation> operations = parseOperations(operationsText);
		if(operations == null || operations.isEmpty()) {
			setLastError("Invalid operations. Use read,write,delete,list,copy,move,paste,all");
			return false;
		}

		String normalizedPath = normalizePath(path);
		PermissionRule existing = findRuleExact(normalizedPath);
		if(existing != null && !verifyPassword(password, existing.saltHex, existing.hashHex)) {
			setLastError("Incorrect password for existing protected scope");
			return false;
		}

		byte[] salt = new byte[16];
		SECURE_RANDOM.nextBytes(salt);
		String saltHex = toHex(salt);
		String hashHex = hashPassword(password, salt);
		if(hashHex == null) {
			setLastError("Failed to hash password");
			return false;
		}

		if(existing != null) {
			permissionRules.remove(existing);
		}
		permissionRules.add(new PermissionRule(normalizedPath, saltHex, hashHex, operations));
		unlockedScopes.remove(normalizedPath);

		persistPermissionRules();
		clearLastError();
		return true;
	}

	@LuaMadeCallable
	public boolean unprotect(String path, String password) {
		if(path == null || path.trim().isEmpty()) {
			setLastError("Usage: unprotect(path, password)");
			return false;
		}
		if(password == null || password.isEmpty()) {
			setLastError("Password must not be empty");
			return false;
		}

		String normalizedPath = normalizePath(path);
		PermissionRule existing = findRuleExact(normalizedPath);
		if(existing == null) {
			setLastError("No protection rule found for " + normalizedPath);
			return false;
		}
		if(!verifyPassword(password, existing.saltHex, existing.hashHex)) {
			setLastError("Incorrect password");
			return false;
		}

		permissionRules.remove(existing);
		unlockedScopes.remove(normalizedPath);
		persistPermissionRules();
		clearLastError();
		return true;
	}

	@LuaMadeCallable
	public boolean auth(String password) {
		if(password == null || password.isEmpty()) {
			setLastError("Usage: auth(password)");
			return false;
		}

		int unlocked = 0;
		for(PermissionRule rule : permissionRules) {
			if(verifyPassword(password, rule.saltHex, rule.hashHex) && !unlockedScopes.containsKey(rule.path)) {
				unlockedScopes.put(rule.path, true);
				unlocked++;
			}
		}

		if(unlocked == 0) {
			setLastError("No protected scopes unlocked");
			return false;
		}

		clearLastError();
		return true;
	}

	@LuaMadeCallable
	public void clearAuth() {
		unlockedScopes.clear();
		clearLastError();
	}

	@LuaMadeCallable
	public List<String> listPermissions() {
		List<String> lines = new ArrayList<>();
		for(PermissionRule rule : permissionRules) {
			String state = unlockedScopes.containsKey(rule.path) ? "unlocked" : "locked";
			lines.add(rule.path + " [" + joinOperations(rule.operations) + "] (" + state + ")");
		}
		return lines;
	}

	@LuaMadeCallable
	public String getPermissions(String path) {
		if(path == null || path.trim().isEmpty()) {
			path = getCurrentDir();
		}

		String normalizedPath = normalizePath(path);
		PermissionRule matching = findMatchingRule(normalizedPath);
		if(matching == null) {
			return "none";
		}

		String state = unlockedScopes.containsKey(matching.path) ? "unlocked" : "locked";
		return matching.path + " [" + joinOperations(matching.operations) + "] (" + state + ")";
	}

	private EnumSet<PermissionOperation> parseOperations(String operationsText) {
		EnumSet<PermissionOperation> out = EnumSet.noneOf(PermissionOperation.class);
		String normalized = operationsText == null ? "all" : operationsText.trim();
		if(normalized.isEmpty()) {
			normalized = "all";
		}

		String[] tokens = normalized.split(",");
		for(String rawToken : tokens) {
			String token = rawToken.trim().toLowerCase(Locale.ROOT);
			if(token.isEmpty()) {
				continue;
			}
			if("all".equals(token) || "*".equals(token)) {
				out.addAll(EnumSet.allOf(PermissionOperation.class));
				continue;
			}
			if("copy".equals(token)) {
				out.add(PermissionOperation.READ);
				out.add(PermissionOperation.WRITE);
				continue;
			}
			if("move".equals(token)) {
				out.add(PermissionOperation.READ);
				out.add(PermissionOperation.WRITE);
				out.add(PermissionOperation.DELETE);
				continue;
			}
			if("paste".equals(token)) {
				out.add(PermissionOperation.WRITE);
				continue;
			}
			if("rw".equals(token)) {
				out.add(PermissionOperation.READ);
				out.add(PermissionOperation.WRITE);
				continue;
			}

			boolean matched = false;
			for(PermissionOperation operation : PermissionOperation.values()) {
				if(operation.token().equals(token)) {
					out.add(operation);
					matched = true;
					break;
				}
			}
			if(!matched) {
				return null;
			}
		}

		return out;
	}

	private String joinOperations(EnumSet<PermissionOperation> operations) {
		StringBuilder out = new StringBuilder();
		for(PermissionOperation operation : PermissionOperation.values()) {
			if(!operations.contains(operation)) {
				continue;
			}
			if(out.length() > 0) {
				out.append(',');
			}
			out.append(operation.token());
		}
		return out.toString();
	}

	private PermissionRule findRuleExact(String normalizedPath) {
		for(PermissionRule rule : permissionRules) {
			if(rule.path.equals(normalizedPath)) {
				return rule;
			}
		}
		return null;
	}

	private PermissionRule findMatchingRule(String normalizedPath) {
		PermissionRule bestMatch = null;
		for(PermissionRule rule : permissionRules) {
			boolean matches = normalizedPath.equals(rule.path)
					|| "/".equals(rule.path)
					|| normalizedPath.startsWith(rule.path + "/");
			if(!matches) {
				continue;
			}

			if(bestMatch == null || rule.path.length() > bestMatch.path.length()) {
				bestMatch = rule;
			}
		}
		return bestMatch;
	}

	private boolean hasPermission(String path, PermissionOperation operation, String actionName) {
		String normalizedPath = normalizePath(path);
		PermissionRule rule = findMatchingRule(normalizedPath);
		if(rule == null || !rule.operations.contains(operation) || unlockedScopes.containsKey(rule.path)) {
			clearLastError();
			return true;
		}

		setLastError("Permission denied: " + actionName + " requires password for " + rule.path);
		return false;
	}

	private void loadPermissionRules() {
		permissionRules.clear();
		unlockedScopes.clear();

		String content = readInternalConfig(PERMISSIONS_FILE_PATH);
		if(content == null || content.trim().isEmpty()) {
			return;
		}

		String[] lines = content.split("\\n");
		for(String line : lines) {
			String trimmed = line.trim();
			if(trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}

			String[] parts = trimmed.split("\\|", 4);
			if(parts.length != 4) {
				continue;
			}

			EnumSet<PermissionOperation> operations = parseOperations(parts[1]);
			if(operations == null || operations.isEmpty()) {
				continue;
			}

			String normalizedPath = normalizePath(parts[0]);
			permissionRules.add(new PermissionRule(normalizedPath, parts[2], parts[3], operations));
		}
	}

	private void persistPermissionRules() {
		StringBuilder content = new StringBuilder();
		for(PermissionRule rule : permissionRules) {
			content.append(rule.path)
					.append('|')
					.append(joinOperations(rule.operations))
					.append('|')
					.append(rule.saltHex)
					.append('|')
					.append(rule.hashHex)
					.append('\n');
		}

		if(!writeInternalConfig(PERMISSIONS_FILE_PATH, content.toString())) {
			LuaMade.getInstance().logWarning("Failed to persist filesystem permissions for computer " + computerUUID + ".");
		}
	}

	private String readInternalConfig(String path) {
		File target = resolveSandboxPath(path);
		if(target == null || !target.exists() || target.isDirectory()) {
			return null;
		}

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(target), StandardCharsets.UTF_8))) {
			StringBuilder content = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				if(content.length() > 0) {
					content.append('\n');
				}
				content.append(line);
			}
			return content.toString();
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to read internal config at " + path, exception);
			return null;
		}
	}

	private boolean writeInternalConfig(String path, String content) {
		File target = resolveSandboxPath(path);
		if(target == null) {
			return false;
		}

		File parent = target.getParentFile();
		if(parent != null && !parent.exists() && !parent.mkdirs()) {
			return false;
		}

		try(OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(target), StandardCharsets.UTF_8)) {
			writer.write(content == null ? "" : content);
			return true;
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to write internal config at " + path, exception);
			return false;
		}
	}

	private String hashPassword(String password, byte[] salt) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			digest.update(salt);
			digest.update(password.getBytes(StandardCharsets.UTF_8));
			return toHex(digest.digest());
		} catch(NoSuchAlgorithmException exception) {
			LuaMade.getInstance().logException("SHA-256 unavailable for permission hashing", exception);
			return null;
		}
	}

	private boolean verifyPassword(String password, String saltHex, String expectedHashHex) {
		byte[] salt = fromHex(saltHex);
		if(salt == null) {
			return false;
		}

		String computed = hashPassword(password, salt);
		return computed != null && computed.equalsIgnoreCase(expectedHashHex);
	}

	private String toHex(byte[] bytes) {
		StringBuilder out = new StringBuilder(bytes.length * 2);
		for(byte b : bytes) {
			out.append(String.format(Locale.ROOT, "%02x", b));
		}
		return out.toString();
	}

	private byte[] fromHex(String hex) {
		if(hex == null || (hex.length() % 2) != 0) {
			return null;
		}

		byte[] out = new byte[hex.length() / 2];
		for(int i = 0; i < hex.length(); i += 2) {
			int hi = Character.digit(hex.charAt(i), 16);
			int lo = Character.digit(hex.charAt(i + 1), 16);
			if(hi < 0 || lo < 0) {
				return null;
			}
			out[i / 2] = (byte) ((hi << 4) + lo);
		}
		return out;
	}

	/**
	 * Lists files in the specified directory
	 */
	@LuaMadeCallable
	public List<String> list(String path, String... args) {
		if(path == null || path.isEmpty()) path = getCurrentDir();

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.LIST, "list")) {
			return new ArrayList<>();
		}

		// Get the virtual file for the path
		VirtualFile dir = getFile(path);
		if(dir == null || !dir.isDirectory()) {
			return new ArrayList<>();
		}

		List<String> result = new ArrayList<>();
		VirtualFile[] files = dir.listFiles();
		if(files != null) {
			for(VirtualFile file : files) {
				result.add(file.getName());
			}
		}

		return result;
	}

	/**
	 * Creates a directory at the specified path
	 */
	@LuaMadeCallable
	public boolean makeDir(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No directory path specified");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.WRITE, "write")) {
			return false;
		}

		// Check if path already exists
		VirtualFile existing = getFile(path);
		if(existing != null) {
			setLastError("Path already exists");
			return false;
		}

		File safeFile = resolveSandboxPath(path);
		if(safeFile == null) {
			setLastError("Path is outside sandbox");
			return false;
		}

		clearLastError();
		return safeFile.mkdirs();
	}

	/**
	 * Reads the file system from disk, decompressing if a saved state exists.
	 * Note: module.getUUID() returns a standard UUID string (hex + hyphens), which is safe for file names.
	 */
	private boolean readFilesFromDisk(ComputerModule module) {
		// Each computer file system is stored in a compressed file
		File compressedFile = new File(computerStorage, "computer_" + module.getUUID() + "_fs.smdat");
		File rootDirectory = new File(computerStorage, "computer_" + module.getUUID() + "_fs");
		boolean hasCompressedArchive = compressedFile.exists() && compressedFile.length() > 0;

		if(rootDirectory.exists()) {
			// If an unpacked filesystem already exists, reuse it. This is normal before
			// the first archive save; only warn when an archive is also present.
			File[] children = rootDirectory.listFiles();
			if(children != null && children.length > 0) {
				if(hasCompressedArchive) {
					LuaMade.getInstance().logWarning("File system for computer " + module.getUUID() + " was not cleaned up properly on server shutdown!");
				}
				this.rootDirectory = new VirtualFile(this, rootDirectory);
				currentDirectory = this.rootDirectory;
				return true;
			}
			// Stale empty dir: remove it and continue normal load/bootstrap path.
			deleteRecursive(rootDirectory);
		}

		if(!hasCompressedArchive) {
			return false;
		}

		try {
			rootDirectory.mkdirs();
			CompressionUtils.decompressFS(compressedFile, rootDirectory);
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to decompress file system for computer " + module.getUUID(), exception);
			deleteRecursive(rootDirectory);
			return false;
		}

		this.rootDirectory = new VirtualFile(this, rootDirectory);
		currentDirectory = this.rootDirectory;
		return true;
	}

	/**
	 * Initializes default Unix-like directory structure
	 */
	public void initializeDefaultDirectories() {
		File rootDirFile = new File(computerStorage, "computer_" + computerUUID + "_fs");
		if(!rootDirFile.exists()) {
			rootDirFile.mkdirs();
		}
		rootDirectory = new VirtualFile(this, rootDirFile);
		currentDirectory = rootDirectory;

		// Create standard Unix directories if they don't exist
		String[] defaultDirs = {"/home", "/bin", "/usr", "/etc", "/tmp"};
		for(String dir : defaultDirs) {
			if(!exists(dir)) {
				makeDir(dir);
			}
		}

		// Create default startup files
		createDefaultFiles();
	}

	/**
	 * Creates default files for a new file system
	 */
	private void createDefaultFiles() {
		installDefaultScriptFromResource("scripts/etc/startup.lua", STARTUP_SCRIPT_PATH);
		migrateLegacyStartupScript();
		installDefaultScriptFromResource("scripts/bin/shell.lua", "/bin/shell.lua");
		installDefaultScriptFromResource("scripts/bin/hello.lua", "/bin/hello.lua");
		installDefaultScriptFromResource("scripts/bin/chat.lua", "/bin/chat.lua");
		installDefaultScriptFromResource("scripts/bin/listall.lua", "/bin/listall.lua");
		installDefaultScriptFromResource("scripts/bin/channel_chat.lua", "/bin/channel_chat.lua");
		installDefaultScriptFromResource("scripts/bin/modem.lua", "/bin/modem.lua");
		installDefaultScriptFromResource("scripts/bin/gfx_demo.lua", "/bin/gfx_demo.lua");
		installDefaultScriptFromResource("scripts/bin/gfx_interactive.lua", "/bin/gfx_interactive.lua");

		// Create a README file
		String readme = 
			"LuaMade Terminal\n" +
			"==========================\n" +
			"\n" +
			"Welcome to your LuaMade computer!\n" +
			"\n" +
			"Available Commands:\n" +
			"  ls [dir]         - List files\n" +
			"  cd <dir>         - Change directory\n" +
			"  pwd              - Print working directory\n" +
			"  cat <file>       - Display file contents\n" +
			"  mkdir <dir>      - Create directory\n" +
			"  touch <file>     - Create empty file\n" +
			"  rm <file>        - Delete file\n" +
			"  cp <src> <dst>   - Copy file\n" +
			"  mv <src> <dst>   - Move/rename file\n" +
			"  edit <file> <text> - Write text to file\n" +
			"  run <script> [args] - Run Lua script\n" +
			"  echo <text>      - Print text\n" +
			"  clear            - Clear terminal\n" +
			"  help             - Show available commands\n" +
			"  exit             - Exit terminal\n" +
			"\n" +
			"Lua Scripts:\n" +
			"  You can write Lua scripts and run them from the terminal.\n" +
			"  Scripts have access to:\n" +
			"    - console: Console API for printing\n" +
			"    - fs: File system API\n" +
			"    - term: Terminal API\n" +
			"    - net: Network API\n" +
			"    - util: Utility API\n" +
			"    - args: Table of command-line arguments\n" +
			"\n" +
			"Example scripts are located in /bin/:\n" +
			"  - hello.lua: Simple hello world\n" +
			"  - shell.lua: Shell information\n" +
			"  - chat.lua: Send messages to other computers\n" +
			"  - channel_chat.lua: Galaxy/local channel messaging\n" +
			"  - modem.lua: Long-range 1-to-1 modem helper\n" +
			"  - listall.lua: Recursively list all files\n" +
					"  - gfx_demo.lua: 2D text graphics API demo\n" +
					"  - gfx_interactive.lua: Interactive gfx showcase\n" +
			"\n" +
			"Try: run /bin/hello.lua YourName\n";
		
		if(!exists("/home/README.txt")) {
			write("/home/README.txt", readme);
		}
	}

	private void migrateManagedScriptIfMissingMarker(String destinationPath, String resourcePath, String requiredMarker) {
		if(destinationPath == null || resourcePath == null || requiredMarker == null) {
			return;
		}
		if(!exists(destinationPath) || isDir(destinationPath)) {
			return;
		}

		String currentScript = read(destinationPath);
		if(currentScript == null) {
			return;
		}

		String normalizedCurrent = normalizeLineEndings(currentScript);
		// Only auto-upgrade bundled scripts; do not overwrite user-authored files.
		if(!normalizedCurrent.startsWith("-- /bin/")) {
			return;
		}
		if(normalizedCurrent.contains(requiredMarker)) {
			return;
		}

		String latestScript = readResourceText(resourcePath);
		if(latestScript == null) {
			return;
		}

		String normalizedLatest = normalizeLineEndings(latestScript);
		if(!normalizedLatest.contains(requiredMarker)) {
			return;
		}

		if(write(destinationPath, latestScript)) {
			LuaMade.getInstance().logInfo("Updated managed script " + destinationPath + " for computer " + computerUUID + ".");
		}
	}

	private void migrateLegacyStartupScript() {
		if(!exists(STARTUP_SCRIPT_PATH) || isDir(STARTUP_SCRIPT_PATH)) {
			return;
		}

		String currentStartup = read(STARTUP_SCRIPT_PATH);
		if(currentStartup == null || !shouldUpgradeManagedStartupScript(currentStartup)) {
			return;
		}

		String upgradedStartup = readResourceText("scripts/etc/startup.lua");
		if(upgradedStartup == null) {
			return;
		}

		if(!exists(STARTUP_BACKUP_PATH)) {
			write(STARTUP_BACKUP_PATH, currentStartup);
		}

		if(write(STARTUP_SCRIPT_PATH, upgradedStartup)) {
			LuaMade.getInstance().logInfo("Updated legacy startup script for computer " + computerUUID + ".");
		}
	}

	private boolean shouldUpgradeManagedStartupScript(String startupScript) {
		String normalized = normalizeLineEndings(startupScript);
		if(normalized.isEmpty()) {
			return false;
		}

		if(normalized.equals(LEGACY_STARTUP_SCRIPT_V1)) {
			return true;
		}

		// Only migrate scripts that still look like LuaMade-managed defaults.
		boolean managedDefaultLike = normalized.startsWith(STARTUP_MANAGED_HEADER)
			&& normalized.contains(STARTUP_BANNER_LINE)
			&& normalized.contains(STARTUP_HELP_LINE);
		if(!managedDefaultLike) {
			return false;
		}

		// Known fragile startup variants from older builds.
		return normalized.contains("term.setAutoPrompt(")
			|| normalized.contains("term.setPromptTemplate(")
			|| normalized.contains("terminalApi.setAutoPrompt ~= nil")
			|| normalized.contains("terminalApi.setPromptTemplate ~= nil");
	}

	private String normalizeLineEndings(String text) {
		return text.replace("\r\n", "\n").trim();
	}

	private void installDefaultScriptFromResource(String resourcePath, String destinationPath) {
		if(exists(destinationPath)) {
			return;
		}

		String script = readResourceText(resourcePath);
		if(script == null) {
			LuaMade.getInstance().logWarning("Default script resource not found: " + resourcePath);
			return;
		}

		if(!write(destinationPath, script)) {
			LuaMade.getInstance().logWarning("Failed writing default script to " + destinationPath + " from resource " + resourcePath);
		}
	}

	private String readResourceText(String resourcePath) {
		InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
		if(in == null) {
			in = LuaMade.class.getClassLoader().getResourceAsStream(resourcePath);
		}
		if(in == null) {
			return null;
		}

		try(BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			StringBuilder content = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				if(content.length() > 0) {
					content.append('\n');
				}
				content.append(line);
			}
			return content.toString();
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed reading default script resource: " + resourcePath, exception);
			return null;
		}
	}

	private void ensureDefaultStructure() {
		if(rootDirectory == null || rootDirectory.getInternalFile() == null) {
			initializeDefaultDirectories();
			return;
		}
		if(currentDirectory == null) {
			currentDirectory = rootDirectory;
		}

		String[] defaultDirs = {"/home", "/bin", "/usr", "/etc", "/tmp"};
		for(String dir : defaultDirs) {
			if(!exists(dir)) {
				makeDir(dir);
			}
		}

		createDefaultFiles();
	}

	/**
	 * Reads the content of a file
	 */
	@LuaMadeCallable
	public String read(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No file path specified");
			return null;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.READ, "read")) {
			return null;
		}

		// Get the file
		VirtualFile file = getFile(path);
		if(file == null || file.isDirectory()) {
			setLastError("File not found or is a directory");
			return null;
		}

		clearLastError();
		return readFile(file);
	}

	/**
	 * Writes content to a file
	 */
	@LuaMadeCallable
	public boolean write(String path, String content) {
		if(path == null || path.isEmpty()) {
			setLastError("No file path specified");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.WRITE, "write")) {
			return false;
		}

		try {
			File safeFile = resolveSandboxPath(path);
			if(safeFile == null) {
				setLastError("Path is outside sandbox");
				return false;
			}

			VirtualFile file = new VirtualFile(this, safeFile);

			// Make sure parent directory exists
			file.getInternalFile().getParentFile().mkdirs();

			// Write content to file
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file.getInternalFile()), StandardCharsets.UTF_8);
			writer.write(content);
			writer.close();
			clearLastError();
			return true;
		} catch(Exception e) {
			setLastError("Failed writing file: " + e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Creates a file if missing, but never truncates existing files.
	 */
	@LuaMadeCallable
	public boolean touch(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No file path specified");
			return false;
		}

		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.WRITE, "write")) {
			return false;
		}

		File safeFile = resolveSandboxPath(path);
		if(safeFile == null) {
			setLastError("Path is outside sandbox");
			return false;
		}

		if(safeFile.exists()) {
			if(safeFile.isDirectory()) {
				setLastError("Path is a directory");
				return false;
			}
			clearLastError();
			return true;
		}

		File parent = safeFile.getParentFile();
		if(parent != null && !parent.exists() && !parent.mkdirs()) {
			setLastError("Could not create parent directory");
			return false;
		}

		try {
			boolean created = safeFile.createNewFile();
			if(!created) {
				setLastError("Could not create file");
				return false;
			}
			clearLastError();
			return true;
		} catch(IOException exception) {
			setLastError("Failed touching file: " + exception.getMessage());
			return false;
		}
	}

	/**
	 * Deletes a file or directory
	 */
	@LuaMadeCallable
	public boolean delete(String path) {
		if(path == null || path.isEmpty() || "/".equals(path)) {
			setLastError("Cannot delete root or empty path");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.DELETE, "delete")) {
			return false;
		}

		// Get the file
		VirtualFile file = getFile(path);
		if(file == null) {
			setLastError("Path not found");
			return false;
		}

		// If it's a directory, check if it's empty
		if(file.isDirectory()) {
			VirtualFile[] files = file.listFiles();
			if(files != null && files.length > 0) {
				setLastError("Directory is not empty");
				return false; // Directory not empty
			}
		}

		// Delete file or directory
		clearLastError();
		return file.getInternalFile().delete();
	}

	/**
	 * Changes the current working directory
	 */
	@LuaMadeCallable
	public boolean changeDir(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No directory path specified");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.LIST, "enter directory")) {
			return false;
		}

		// Get the directory
		VirtualFile dir = getFile(path);
		if(dir == null || !dir.isDirectory()) {
			setLastError("Directory not found");
			return false;
		}

		currentDirectory = dir;
		clearLastError();
		return true;
	}

	/**
	 * Gets the current working directory
	 */
	@LuaMadeCallable
	public String getCurrentDir() {
		if(currentDirectory == null) {
			currentDirectory = rootDirectory;
		}
		if(currentDirectory == null) {
			return "/";
		}

		return sanitizeDisplayPath(currentDirectory.getInternalFile());
	}

	/**
	 * Checks if a file exists
	 */
	@LuaMadeCallable
	public boolean exists(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No path specified");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.READ, "inspect")) {
			return false;
		}

		VirtualFile file = getFile(path);
		clearLastError();
		return file != null && file.getInternalFile().exists();
	}

	/**
	 * Checks if a path is a directory
	 */
	@LuaMadeCallable
	public boolean isDir(String path) {
		if(path == null || path.isEmpty()) {
			setLastError("No path specified");
			return false;
		}

		// Normalize path
		path = normalizePath(path);
		if(!hasPermission(path, PermissionOperation.READ, "inspect")) {
			return false;
		}

		VirtualFile file = getFile(path);
		clearLastError();
		return file != null && file.isDirectory();
	}

	/**
	 * Normalizes a path (resolves '.', '..', and ensures it starts with '/')
	 */
	@LuaMadeCallable
	public String normalizePath(String path) {
		if(path == null || path.isEmpty()) {
			return getCurrentDir();
		}

		path = sanitizeInputPath(path);

		// If path is relative, prepend current directory
		if(!path.startsWith("/")) {
			String currentPath = getCurrentDir();
			if("/".equals(currentPath)) {
				path = "/" + path;
			} else {
				path = currentPath + "/" + path;
			}
		}

		// Split path into components
		String[] components = path.split("/");
		List<String> normalizedComponents = new ArrayList<>();

		for(String component : components) {
			if(component.isEmpty() || ".".equals(component)) {
				continue;
			} else if("..".equals(component)) {
				if(!normalizedComponents.isEmpty()) {
					normalizedComponents.remove(normalizedComponents.size() - 1);
				}
			} else {
				normalizedComponents.add(component);
			}
		}

		// Reconstruct path
		StringBuilder normalizedPath = new StringBuilder();
		for(String component : normalizedComponents) {
			normalizedPath.append("/").append(component);
		}

		// Handle special case for root directory
		if(normalizedPath.length() == 0) {
			return "/";
		}

		return normalizedPath.toString();
	}

	private String sanitizeDisplayPath(File target) {
		if(target == null || rootDirectory == null || rootDirectory.getInternalFile() == null) {
			return "/";
		}

		try {
			File canonicalRoot = rootDirectory.getInternalFile().getCanonicalFile();
			File canonicalTarget = target.getCanonicalFile();
			String rootPath = canonicalRoot.getPath();
			String targetPath = canonicalTarget.getPath();

			if(targetPath.equals(rootPath)) {
				return "/";
			}
			if(!targetPath.startsWith(rootPath + File.separator)) {
				LuaMade.getInstance().logWarning("Current directory escaped sandbox; resetting to root for computer " + computerUUID + ".");
				currentDirectory = rootDirectory;
				return "/";
			}

			String relative = targetPath.substring(rootPath.length()).replace('\\', '/');
			if(relative.isEmpty()) {
				return "/";
			}
			return relative.startsWith("/") ? relative : "/" + relative;
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to sanitize display path for computer " + computerUUID, exception);
			currentDirectory = rootDirectory;
			return "/";
		}
	}

	/**
	 * Gets the parent directory of a path
	 */
	private String getParentDirectory(String path) {
		int lastSlash = path.lastIndexOf('/');
		if(lastSlash <= 0) {
			return "/";
		}
		return path.substring(0, lastSlash);
	}

	private String sanitizeInputPath(String rawPath) {
		String normalizedInput = rawPath.replace('\\', '/');
		if(rootDirectory == null || rootDirectory.getInternalFile() == null) {
			return normalizedInput;
		}

		try {
			String canonicalRootPath = rootDirectory.getInternalFile().getCanonicalPath().replace('\\', '/');
			if(normalizedInput.equals(canonicalRootPath)) {
				return "/";
			}
			if(normalizedInput.startsWith(canonicalRootPath + "/")) {
				String relative = normalizedInput.substring(canonicalRootPath.length());
				return relative.isEmpty() ? "/" : relative;
			}
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to sanitize input path for computer " + computerUUID, exception);
		}

		return normalizedInput;
	}

	/**
	 * Gets a file by its path
	 */
	public VirtualFile getFile(String filePath) {
		if(filePath == null || filePath.isEmpty()) {
			setLastError("No path specified");
			return null;
		}

		// Normalize the path
		filePath = normalizePath(filePath);
		if(!hasPermission(filePath, PermissionOperation.READ, "read")) {
			return null;
		}

		File internalFile = resolveSandboxPath(filePath);
		if(internalFile == null) {
			setLastError("Path is outside sandbox");
			return null;
		}

		if(!internalFile.exists()) {
			setLastError("Path not found");
			return null;
		}

		clearLastError();
		return new VirtualFile(this, internalFile);
	}

	private enum PermissionOperation {
		READ("read"),
		WRITE("write"),
		DELETE("delete"),
		LIST("list");

		private final String token;

		PermissionOperation(String token) {
			this.token = token;
		}

		public String token() {
			return token;
		}
	}

	private static final class PermissionRule {
		private final String path;
		private final String saltHex;
		private final String hashHex;
		private final EnumSet<PermissionOperation> operations;

		private PermissionRule(String path, String saltHex, String hashHex, EnumSet<PermissionOperation> operations) {
			this.path = path;
			this.saltHex = saltHex;
			this.hashHex = hashHex;
			this.operations = EnumSet.copyOf(operations);
		}
	}

	private File resolveSandboxPath(String normalizedPath) {
		if(normalizedPath == null || rootDirectory == null || rootDirectory.getInternalFile() == null) {
			return null;
		}

		String path = normalizePath(normalizedPath);
		String relativePath = "/".equals(path) ? "" : (path.startsWith("/") ? path.substring(1) : path);
		File root = rootDirectory.getInternalFile();
		File candidate = relativePath.isEmpty() ? root : new File(root, relativePath);

		try {
			File canonicalRoot = root.getCanonicalFile();
			File canonicalCandidate = candidate.getCanonicalFile();
			if(canonicalCandidate.equals(canonicalRoot)) {
				return canonicalCandidate;
			}

			String rootPath = canonicalRoot.getPath();
			String candidatePath = canonicalCandidate.getPath();
			if(!candidatePath.startsWith(rootPath + File.separator)) {
				LuaMade.getInstance().logWarning("Blocked sandbox escape attempt: " + path);
				return null;
			}

			return canonicalCandidate;
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to resolve sandbox path: " + path, exception);
			return null;
		}
	}

	/**
	 * Reads the content of a virtual file
	 */
	public String readFile(VirtualFile virtualFile) {
		if(virtualFile == null || virtualFile.isDirectory()) {
			return null;
		}
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(virtualFile.getInternalFile()), StandardCharsets.UTF_8));
			StringBuilder content = new StringBuilder();
			String line;
			while((line = reader.readLine()) != null) {
				content.append(line).append("\n");
			}
			reader.close();
			
			// Remove trailing newline if present
			if(content.length() > 0 && content.charAt(content.length() - 1) == '\n') {
				content.setLength(content.length() - 1);
			}
			
			return content.toString();
		} catch(Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public VirtualFile getRootDirectory() {
		return rootDirectory;
	}

	/**
	 * Saves the file system to disk by compressing it to a .smdat file.
	 * This method should be called when the computer is going idle or shutting down.
	 * Note: computerUUID is generated by UUID.nameUUIDFromBytes().toString(), which produces
	 * a safe file name format (hexadecimal + hyphens only).
	 */
	public boolean saveToDisk() {
		File compressedFile = new File(computerStorage, "computer_" + computerUUID + "_fs.smdat");
		File rootDir = rootDirectory.getInternalFile();

		try {
			CompressionUtils.compressFS(rootDir, compressedFile);
			LuaMade.getInstance().logInfo("Saved file system for computer " + computerUUID);
			return true;
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Error saving file system for computer " + computerUUID, exception);
			return false;
		}
	}

	/**
	 * Cleans up the temporary uncompressed file system directory.
	 * This should be called after saving to disk to free up space.
	 */
	public void cleanupTempFiles() {
		File rootDir = rootDirectory.getInternalFile();
		if(rootDir.exists()) {
			deleteRecursive(rootDir);
			LuaMade.getInstance().logInfo("Cleaned up temporary files for computer " + computerUUID);
		}
	}

	/**
	 * Recursively deletes a directory and all its contents.
	 * @param file The file or directory to delete
	 */
	private void deleteRecursive(File file) {
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			if(children != null) {
				for(File child : children) {
					deleteRecursive(child);
				}
			}
		}
		if(!file.delete()) {
			LuaMade.getInstance().logWarning("Failed to delete file: " + file.getAbsolutePath());
		}
	}
}
