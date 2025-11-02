package luamade.lua.fs;

import luamade.LuaMade;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import luamade.utils.CompressionUtils;
import luamade.utils.DataUtils;

import java.io.File;
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
	public static int MAX_FS_SIZE = 10 * 1024 * 1024; //10MB

	private static final File computerStorage = new File(DataUtils.getWorldDataPath(), "computers");
	private VirtualFile rootDirectory;
	private VirtualFile currentDirectory;

	public static FileSystem initNewFileSystem(ComputerModule module) {
		if(!computerStorage.exists()) computerStorage.mkdirs();
		FileSystem fileSystem = new FileSystem(module);
		//Todo: Create default directories and files

		return fileSystem;
	}

	public FileSystem(ComputerModule module) {
		readFilesFromDisk(module);
		// Initialize with some basic Unix directories
		initializeDefaultDirectories();
	}

	private void readFilesFromDisk(ComputerModule module) {
		// Read the file system from disk
		// This is where you would implement the logic to read the file system from a file
		// For now, we'll just create a root directory
		//Todo: Each computer file system is stored in a compressed file
		File compressedFile = new File(computerStorage, "computer_" + module.getUUID() + "_fs.smdat");
		File rootDirectory = new File(computerStorage, "computer_" + module.getUUID() + "_fs");
		if(rootDirectory.exists()) {
			//Indicator that the file system wasn't cleaned up properly on server shutdown
			LuaMade.getInstance().logWarning("File system for computer " + module.getUUID() + " was not cleaned up properly on server shutdown!");
			this.rootDirectory = new VirtualFile(this, rootDirectory);
			currentDirectory = this.rootDirectory;
			return;
		}
		boolean exists = compressedFile.exists();
		if(!exists) {
			compressedFile.getParentFile().mkdirs();
			try {
				compressedFile.createNewFile();
				rootDirectory.mkdirs();
				this.rootDirectory = new VirtualFile(this, rootDirectory);
				currentDirectory = this.rootDirectory;
				return;
			} catch(Exception exception) {
				throw new RuntimeException(exception);
			}
		} else {
			try {
				rootDirectory.mkdirs();
				CompressionUtils.decompressFS(compressedFile, rootDirectory);
			} catch(Exception exception) {
				throw new RuntimeException(exception);
			}
		}
		this.rootDirectory = new VirtualFile(this, rootDirectory);
		currentDirectory = this.rootDirectory;
	}

	/**
	 * Initializes default Unix-like directory structure
	 */
	private void initializeDefaultDirectories() {
		// Create standard Unix directories if they don't exist
		String[] defaultDirs = {"/home", "/bin", "/usr", "/etc", "/tmp"};
		for(String dir : defaultDirs) {
			makeDir(dir);
		}
		
		// Create default startup files
		createDefaultFiles();
	}

	/**
	 * Creates default files for a new file system
	 */
	private void createDefaultFiles() {
		// Create a simple shell script as an example
		String shellScript = 
			"-- LuaMade Shell\n" +
			"-- This is an example Lua script for the terminal\n" +
			"\n" +
			"print(\"Hello from shell.lua!\")\n" +
			"print(\"You can create your own scripts in /bin or /home\")\n" +
			"print(\"Available globals: console, fs, term, args\")\n";
		
		if(!exists("/bin/shell.lua")) {
			write("/bin/shell.lua", shellScript);
		}
		
		// Create a simple hello world example
		String helloScript = 
			"-- Hello World example\n" +
			"print(\"Hello, World!\")\n" +
			"if args[1] then\n" +
			"    print(\"Hello, \" .. args[1] .. \"!\")\n" +
			"end\n";
		
		if(!exists("/bin/hello.lua")) {
			write("/bin/hello.lua", helloScript);
		}
		
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
			"    - args: Table of command-line arguments\n" +
			"\n" +
			"Example scripts are located in /bin/\n";
		
		if(!exists("/home/README.txt")) {
			write("/home/README.txt", readme);
		}
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

		// Create the directory
		VirtualFile dir = new VirtualFile(this, new File(rootDirectory.getInternalFile(), path.substring(1)));
		return dir.getInternalFile().mkdirs();
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
			// Create the file
			VirtualFile file = new VirtualFile(this, new File(rootDirectory.getInternalFile(), path.substring(1)));
			
			// Make sure parent directory exists
			file.getInternalFile().getParentFile().mkdirs();
			
			// Write content to file
			java.io.FileWriter writer = new java.io.FileWriter(file.getInternalFile());
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
		if(path == null || path.isEmpty() || path.equals("/")) {
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
			if(component.isEmpty() || component.equals(".")) {
				continue;
			} else if(component.equals("..")) {
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
		
		// Root directory
		if(filePath.equals("/")) {
			return rootDirectory;
		}
		
		// Remove leading slash
		if(filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		
		File internalFile = new File(rootDirectory.getInternalFile(), filePath);
		if(!internalFile.exists()) {
			return null;
		}
		
		return new VirtualFile(this, internalFile);
	}

	/**
	 * Reads the content of a virtual file
	 */
	public String readFile(VirtualFile virtualFile) {
		if(virtualFile == null || virtualFile.isDirectory()) {
			return null;
		}
		
		try {
			java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(virtualFile.getInternalFile()));
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
}
