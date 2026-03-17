package luamade.lua.fs;

import luamade.LuaMade;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import luamade.utils.CompressionUtils;
import luamade.utils.DataUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

	private static final File computerStorage = new File(DataUtils.getWorldDataPath(), "computers");
	private VirtualFile rootDirectory;
	private VirtualFile currentDirectory;
	private final String computerUUID;

	public static FileSystem initNewFileSystem(ComputerModule module) {
		if(!computerStorage.exists()) {
			computerStorage.mkdirs();
		}
		return new FileSystem(module);
	}

	public FileSystem(ComputerModule module) {
		computerUUID = module.getUUID();
		if(!computerStorage.exists()) {
			computerStorage.mkdirs();
		}
		if(!readFilesFromDisk(module)) {
			initializeDefaultDirectories();
		}
		ensureDefaultStructure();
	}

	/**
	 * Reads the file system from disk, decompressing if a saved state exists.
	 * Note: module.getUUID() returns a standard UUID string (hex + hyphens), which is safe for file names.
	 */
	private boolean readFilesFromDisk(ComputerModule module) {
		// Each computer file system is stored in a compressed file
		File compressedFile = new File(computerStorage, "computer_" + module.getUUID() + "_fs.smdat");
		File rootDirectory = new File(computerStorage, "computer_" + module.getUUID() + "_fs");

		if(rootDirectory.exists()) {
			// If the uncompressed root is left behind, keep it only if it has content.
			File[] children = rootDirectory.listFiles();
			if(children != null && children.length > 0) {
				LuaMade.getInstance().logWarning("File system for computer " + module.getUUID() + " was not cleaned up properly on server shutdown!");
				this.rootDirectory = new VirtualFile(this, rootDirectory);
				currentDirectory = this.rootDirectory;
				return true;
			}
			// Stale empty dir: remove it and continue normal load/bootstrap path.
			deleteRecursive(rootDirectory);
		}

		boolean exists = compressedFile.exists() && compressedFile.length() > 0;
		if(!exists) {
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
		installDefaultScriptFromResource("scripts/etc/startup.lua", "/etc/startup.lua");
		installDefaultScriptFromResource("scripts/bin/shell.lua", "/bin/shell.lua");
		installDefaultScriptFromResource("scripts/bin/hello.lua", "/bin/hello.lua");
		installDefaultScriptFromResource("scripts/bin/chat.lua", "/bin/chat.lua");
		installDefaultScriptFromResource("scripts/bin/listall.lua", "/bin/listall.lua");
		installDefaultScriptFromResource("scripts/bin/channel_chat.lua", "/bin/channel_chat.lua");
		installDefaultScriptFromResource("scripts/bin/modem.lua", "/bin/modem.lua");
		
		// Create a README file
		String readme = 
			"LuaMade Unix-like Terminal\n" +
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
			"\n" +
			"Try: run /bin/hello.lua YourName\n";
		
		if(!exists("/home/README.txt")) {
			write("/home/README.txt", readme);
		}
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
	 * Lists files in the specified directory
	 */
	@LuaMadeCallable
	public List<String> list(String path, String... args) {
		if(path == null || path.isEmpty()) path = currentDirectory.getPath();

		// Normalize path
		path = normalizePath(path);

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
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		// Check if path already exists
		VirtualFile existing = getFile(path);
		if(existing != null) {
			return false;
		}

		File safeFile = resolveSandboxPath(path);
		if(safeFile == null) {
			return false;
		}

		return safeFile.mkdirs();
	}

	/**
	 * Reads the content of a file
	 */
	@LuaMadeCallable
	public String read(String path) {
		if(path == null || path.isEmpty()) {
			return null;
		}

		// Normalize path
		path = normalizePath(path);

		// Get the file
		VirtualFile file = getFile(path);
		if(file == null || file.isDirectory()) {
			return null;
		}

		return readFile(file);
	}

	/**
	 * Writes content to a file
	 */
	@LuaMadeCallable
	public boolean write(String path, String content) {
		if(path == null || path.isEmpty()) {
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		try {
			File safeFile = resolveSandboxPath(path);
			if(safeFile == null) {
				return false;
			}

			VirtualFile file = new VirtualFile(this, safeFile);
			
			// Make sure parent directory exists
			file.getInternalFile().getParentFile().mkdirs();
			
			// Write content to file
			OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file.getInternalFile()), StandardCharsets.UTF_8);
			writer.write(content);
			writer.close();
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Deletes a file or directory
	 */
	@LuaMadeCallable
	public boolean delete(String path) {
		if(path == null || path.isEmpty() || "/".equals(path)) {
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		// Get the file
		VirtualFile file = getFile(path);
		if(file == null) {
			return false;
		}

		// If it's a directory, check if it's empty
		if(file.isDirectory()) {
			VirtualFile[] files = file.listFiles();
			if(files != null && files.length > 0) {
				return false; // Directory not empty
			}
		}

		// Delete file or directory
		return file.getInternalFile().delete();
	}

	/**
	 * Changes the current working directory
	 */
	@LuaMadeCallable
	public boolean changeDir(String path) {
		if(path == null || path.isEmpty()) {
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		// Get the directory
		VirtualFile dir = getFile(path);
		if(dir == null || !dir.isDirectory()) {
			return false;
		}

		currentDirectory = dir;
		return true;
	}

	/**
	 * Gets the current working directory
	 */
	@LuaMadeCallable
	public String getCurrentDir() {
		String path = currentDirectory.getPath();
		return path.isEmpty() ? "/" : "/" + path;
	}

	/**
	 * Checks if a file exists
	 */
	@LuaMadeCallable
	public boolean exists(String path) {
		if(path == null || path.isEmpty()) {
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		VirtualFile file = getFile(path);
		return file != null && file.getInternalFile().exists();
	}

	/**
	 * Checks if a path is a directory
	 */
	@LuaMadeCallable
	public boolean isDir(String path) {
		if(path == null || path.isEmpty()) {
			return false;
		}

		// Normalize path
		path = normalizePath(path);

		VirtualFile file = getFile(path);
		return file != null && file.isDirectory();
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

	/**
	 * Normalizes a path (resolves '.', '..', and ensures it starts with '/')
	 */
	@LuaMadeCallable
	public String normalizePath(String path) {
		// If path is relative, prepend current directory
		if(!path.startsWith("/")) {
			String currentPath = currentDirectory.getPath();
			if(currentPath.isEmpty()) {
				path = "/" + path;
			} else {
				path = "/" + currentPath + "/" + path;
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

	/**
	 * Gets a file by its path
	 */
	public VirtualFile getFile(String filePath) {
		if(filePath == null || filePath.isEmpty()) {
			return null;
		}
		
		// Normalize the path
		filePath = normalizePath(filePath);
		
		File internalFile = resolveSandboxPath(filePath);
		if(internalFile == null) {
			return null;
		}

		if(!internalFile.exists()) {
			return null;
		}
		
		return new VirtualFile(this, internalFile);
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
