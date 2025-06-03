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
		// Initialize with some basic directories
//		files.put("/", "directory");
//		files.put("/home", "directory");
//		files.put("/bin", "directory");
//		files.put("/usr", "directory");
//		files.put("/etc", "directory");
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
		currentDirectory = this.rootDirectory;
	}

	/**
	 * Lists files in the specified directory
	 */
	@LuaMadeCallable
	public List<String> list(String path, String... args) {
		if(path == null || path.isEmpty()) path = currentDirectory.getPath();

		// Normalize path
		path = normalizePath(path);

		boolean includeDirectories = false;

		// Check args
		for(String arg : args) {
			if(arg.equals("-d")) {
				includeDirectories = true;
				break;
			}
		}

		List<String> result = new ArrayList<>();
		for(VirtualFile file : currentDirectory.listFiles())

//		if(!files.containsKey(path) || !files.get(path).isDirectory()) return new ArrayList<>();
//
//		// Get all files in the directory
//		List<String> result = new ArrayList<>();
//		for(String file : files.keySet()) {
//			if(file.startsWith(path) && !file.equals(path)) {
//				String relativePath = file.substring(path.length());
//				if(relativePath.startsWith("/")) relativePath = relativePath.substring(1);
//
//				// Only include files directly in this directory (not in subdirectories)
//				if(!relativePath.contains("/")) result.add(relativePath);
//			}
//		}

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
		if(files.containsKey(path)) {
			return false;
		}

		// Check if parent directory exists
		String parentDir = getParentDirectory(path);
		if(!files.containsKey(parentDir) || !files.get(parentDir).isDirectory()) {
			return false;
		}

		// Create directory
		VirtualFile dir = new VirtualFile(path);
		return dir.mkdirs();
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

		// Check if file exists and is not a directory
		if(!files.containsKey(path) || files.get(path).getName().equals("directory")) {
			return null;
		}

		return files.get(path);
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

		// Check if parent directory exists
		String parentDir = getParentDirectory(path);
		if(!files.containsKey(parentDir) || !files.get(parentDir).equals("directory")) {
			return false;
		}

		// Write to file
		files.put(path, content);
		return true;
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

		// Check if file exists
		if(!files.containsKey(path)) {
			return false;
		}

		// If it's a directory, check if it's empty
		if(files.get(path).equals("directory")) {
			for(String file : files.keySet()) {
				if(file.startsWith(path + "/")) {
					return false; // Directory not empty
				}
			}
		}

		// Delete file
		files.remove(path);
		return true;
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

		// Check if directory exists
		if(!files.containsKey(path) || !files.get(path).equals("directory")) {
			return false;
		}

		currentDirectory = path;
		return true;
	}

	/**
	 * Gets the current working directory
	 */
	@LuaMadeCallable
	public String getCurrentDir() {
		return currentDirectory;
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

		return files.containsKey(path);
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

		return files.containsKey(path) && files.get(path).equals("directory");
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
			path = currentDirectory + (currentDirectory.getPath().endsWith("/") ? "" : "/") + path;
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

	public VirtualFile getFile(String filePath) {

	}

	public String readFile(VirtualFile virtualFile) {

	}

	public VirtualFile getRootDirectory() {
		return rootDirectory;
	}
}
