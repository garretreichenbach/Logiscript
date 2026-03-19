package luamade.lua.fs;

import luamade.data.DataCompressionInterface;
import luamade.data.SerializationInterface;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.io.File;
import java.io.IOException;

/**
 * Wrapper for a file in the file system. Prevents access to the underlying file system outside the sandbox.
 */
public final class VirtualFile extends LuaMadeUserdata implements SerializationInterface, DataCompressionInterface {

	private final FileSystem fileSystem;
	private final File internalFile;

	public VirtualFile(FileSystem fileSystem, File internalFile) {
		this.fileSystem = fileSystem;
		this.internalFile = internalFile;
	}

	/**
	 * Checks if the file is within the sandboxed file system.
	 * @param file The file to check.
	 * @throws SecurityException if the file is outside the sandboxed file system.
	 */
	public static void checkSanity(VirtualFile file) {
		if(file == null) {
			throw new IllegalArgumentException("VirtualFile is null in checkSanity");
		}
		if(file.fileSystem.getRootDirectory() == null) {
			throw new IllegalStateException("FileSystem rootDirectory is not initialized for VirtualFile: " + (file.internalFile != null ? file.internalFile.getAbsolutePath() : "null"));
		}
		String path = file.getAbsolutePath();
		if(path.contains("..") || path.startsWith(File.separator)) {
			throw new SecurityException("Access to file outside sandboxed file system is not allowed: " + path);
		}
	}

	@LuaMadeCallable
	public VirtualFile[] listFiles() {
		if(!isDirectory()) {
			return new VirtualFile[0];
		}
		File[] files = internalFile.listFiles();
		if(files == null) {
			return new VirtualFile[0];
		}
		VirtualFile[] virtualFiles = new VirtualFile[files.length];
		for(int i = 0; i < files.length; i++) {
			virtualFiles[i] = new VirtualFile(fileSystem, files[i]);
		}
		return virtualFiles;
	}

	@LuaMadeCallable
	public VirtualFile getParentFile() {
		if(internalFile.getParentFile() == null) {
			return null;
		}
		return new VirtualFile(fileSystem, internalFile.getParentFile());
	}

	@LuaMadeCallable
	public String getPath() {
		return getRelativePathFromRoot(false);
	}

	@LuaMadeCallable
	public String getAbsolutePath() {
		if(fileSystem == null || fileSystem.getRootDirectory() == null) {
			throw new IllegalStateException("FileSystem or rootDirectory is not initialized for VirtualFile: " + (internalFile != null ? internalFile.getAbsolutePath() : "null"));
		}
		return getRelativePathFromRoot(true);
	}

	private String getRelativePathFromRoot(boolean absolute) {
		File rootFile = fileSystem.getRootDirectory() != null ? fileSystem.getRootDirectory().internalFile : null;
		if(rootFile == null) {
			return absolute ? internalFile.getAbsolutePath() : internalFile.getPath();
		}

		String path = absolute ? internalFile.getAbsolutePath() : internalFile.getPath();
		String rootPath = absolute ? rootFile.getAbsolutePath() : rootFile.getPath();

		if(!path.startsWith(rootPath)) {
			try {
				String canonicalPath = internalFile.getCanonicalPath();
				String canonicalRootPath = rootFile.getCanonicalPath();
				if(canonicalPath.startsWith(canonicalRootPath)) {
					path = canonicalPath;
					rootPath = canonicalRootPath;
				}
			} catch(IOException ignored) {
				// Fall back to the original path strings if canonical resolution fails.
			}
		}

		if(path.startsWith(rootPath)) {
			path = path.substring(rootPath.length());
		}
		if(path.startsWith(File.separator)) {
			path = path.substring(1);
		}

		return path.replace('\\', '/');
	}

	@LuaMadeCallable
	public boolean isDirectory() {
		return internalFile.isDirectory();
	}

	@LuaMadeCallable
	public String getName() {
		return internalFile.getName();
	}

	public String getTextContents() {
		if(isDirectory()) {
			return null;
		} else {
			return fileSystem.readFile(this);
		}
	}

	public File getInternalFile() {
		return internalFile;
	}
}
