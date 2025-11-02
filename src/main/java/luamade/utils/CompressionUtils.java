package luamade.utils;

import luamade.LuaMade;
import luamade.lua.fs.FileSystem;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Objects;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class CompressionUtils {

	private static final byte END_OF_STREAM = -1;
	private static final byte DIRECTORY = -2;
	private static final byte FILE = -3;

	private static ByteBuffer readBuffer = ByteBuffer.allocate(FileSystem.MAX_FS_SIZE);
	private static ByteBuffer writeBuffer = ByteBuffer.allocate(FileSystem.MAX_FS_SIZE);

	public static void decompressFS(File source, File destination) throws Exception {

	}

	/**
	 * Combines the contents of a folder into a single file and then compresses it.
	 * @param source The folder to compress
	 * @param destination The file to write the compressed data to
	 * @throws Exception If an error occurs during compression
	 */
	public static void compressFS(File source, File destination) throws Exception {
		//First, we combine all the files in the folder into a single file
		readBuffer.clear();
		writeBuffer.clear();
		writeToBufferRecursive(source);
		writeBuffer.flip();

		//Now compress the file
		LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
		compressor.compress(readBuffer, writeBuffer);
	}

	private static void writeToBufferRecursive(File file) {
		if(file.isDirectory()) {
			for(File child : Objects.requireNonNull(file.listFiles())) {
				if(child.isDirectory()) {
					//Write the directory to the buffer
					writeBuffer.put(DIRECTORY);
					//Write the name of the directory to the buffer
					writeBuffer.put(child.getName().getBytes());
					writeToBufferRecursive(child);
				} else {
					//Write the file to the buffer
					writeBuffer.put(FILE);
					writeBuffer.put(child.getName().getBytes());
					//Read the file into the buffer
					try {
						byte[] data = Files.readAllBytes(child.toPath());
						writeBuffer.put(data);
					} catch(Exception exception) {
						LuaMade.getInstance().logException("Error writing file to buffer", exception);
					}
				}
			}
		} else {
			//Read the file into the buffer
			try {

			} catch(Exception exception) {
				LuaMade.getInstance().logException("Error writing file to buffer", exception);
			}
		}
	}
}
