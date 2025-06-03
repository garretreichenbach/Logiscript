package luamade.lua.fs;

import luamade.data.DataCompressionInterface;
import luamade.data.SerializationInterface;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.io.File;

/**
 * Wrapper for a file in the file system. Prevents access to the underlying file system outside the sandbox.
 */
public final class VirtualFile extends LuaMadeUserdata implements SerializationInterface, DataCompressionInterface {

	private final FileSystem fileSystem;
	private File internalFile;

	public VirtualFile(FileSystem fileSystem, File internalFile) {
		this.fileSystem = fileSystem;
		this.internalFile = internalFile;
		checkSanity(this);
	}

	/**
	 * Checks if the file is within the sandboxed file system.
	 * @param file The file to check.
	 * @throws SecurityException if the file is outside the sandboxed file system.
	 */
	public static void checkSanity(VirtualFile file) {
		String path = file.getAbsolutePath();

	}

	@LuaMadeCallable
	public VirtualFile[] listFiles() {
		if(!isDirectory()) return new VirtualFile[0];
		File[] files = internalFile.listFiles();
		if(files == null) return new VirtualFile[0];
		VirtualFile[] virtualFiles = new VirtualFile[files.length];
		for(int i = 0; i < files.length; i++) virtualFiles[i] = new VirtualFile(fileSystem, files[i]);
		return virtualFiles;
	}

	@LuaMadeCallable
	public VirtualFile getParentFile() {
		if(internalFile.getParentFile() == null) return null;
		return new VirtualFile(fileSystem, internalFile.getParentFile());
	}

	@LuaMadeCallable
	public String getPath() {
		//Only include the path relative to the file system root
		String path = internalFile.getPath();
		if(path.startsWith(fileSystem.getRootDirectory().getPath())) path = path.substring(fileSystem.getRootDirectory().getPath().length());
		if(path.startsWith(File.separator)) path = path.substring(1);
		return path;
	}

	@LuaMadeCallable
	public String getAbsolutePath() {
		//Only include the path relative to the file system root
		String path = internalFile.getAbsolutePath();
		if(path.startsWith(fileSystem.getRootDirectory().getAbsolutePath())) path = path.substring(fileSystem.getRootDirectory().getAbsolutePath().length());
		if(path.startsWith(File.separator)) path = path.substring(1);
		return path;
	}

	@LuaMadeCallable
	public boolean isDirectory() {
		return internalFile.isDirectory();
	}

	public String getTextContents() {
		if(isDirectory()) return null;
		else return fileSystem.readFile(this);
	}

	public File getInternalFile() {
		return internalFile;
	}
}
