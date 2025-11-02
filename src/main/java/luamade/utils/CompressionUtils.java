package luamade.utils;

import luamade.LuaMade;
import luamade.lua.fs.FileSystem;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Objects;

/**
 * Utilities for compressing and decompressing file systems for computer data storage.
 *
 * @author Garret Reichenbach
 */
public class CompressionUtils {

	private static final byte END_OF_STREAM = -1;
	private static final byte DIRECTORY = -2;
	private static final byte FILE = -3;

	private static ByteBuffer readBuffer = ByteBuffer.allocate(FileSystem.MAX_FS_SIZE);
	private static ByteBuffer writeBuffer = ByteBuffer.allocate(FileSystem.MAX_FS_SIZE);

	/**
	 * Decompresses a file system from a compressed file.
	 * @param source The compressed file to read from
	 * @param destination The folder to write the decompressed data to
	 * @throws Exception If an error occurs during decompression
	 */
	public static void decompressFS(File source, File destination) throws Exception {
		if(!source.exists() || source.length() == 0) {
			// Empty file, nothing to decompress
			destination.mkdirs();
			return;
		}

		// Read compressed data from file
		FileInputStream fis = new FileInputStream(source);
		byte[] compressedData = new byte[(int) source.length()];
		fis.read(compressedData);
		fis.close();

		// Decompress the data
		readBuffer.clear();
		writeBuffer.clear();
		readBuffer.put(compressedData);
		readBuffer.flip();

		LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
		decompressor.decompress(readBuffer, writeBuffer);
		writeBuffer.flip();

		// Read the decompressed data and recreate the file system
		readFromBufferRecursive(destination, writeBuffer);
	}

	/**
	 * Combines the contents of a folder into a single file and then compresses it.
	 * @param source The folder to compress
	 * @param destination The file to write the compressed data to
	 * @throws Exception If an error occurs during compression
	 */
	public static void compressFS(File source, File destination) throws Exception {
		//First, we combine all the files in the folder into a single buffer
		writeBuffer.clear();
		writeToBufferRecursive(source);
		writeBuffer.put(END_OF_STREAM);
		
		// Get the uncompressed data size
		int uncompressedSize = writeBuffer.position();
		writeBuffer.flip();

		//Now compress the data
		readBuffer.clear();
		LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
		int compressedSize = compressor.compress(writeBuffer, readBuffer);

		// Write compressed data to file
		destination.getParentFile().mkdirs();
		FileOutputStream fos = new FileOutputStream(destination);
		fos.write(readBuffer.array(), 0, compressedSize);
		fos.close();
	}

	/**
	 * Recursively writes files and directories to the buffer.
	 * @param file The file or directory to write
	 */
	private static void writeToBufferRecursive(File file) {
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			if(children != null) {
				for(File child : children) {
					if(child.isDirectory()) {
						//Write the directory marker and name
						writeBuffer.put(DIRECTORY);
						writeString(child.getName());
						writeToBufferRecursive(child);
					} else {
						//Write the file marker and name
						writeBuffer.put(FILE);
						writeString(child.getName());
						//Read the file data and write it
						try {
							byte[] data = Files.readAllBytes(child.toPath());
							writeBuffer.putInt(data.length);
							writeBuffer.put(data);
						} catch(Exception exception) {
							LuaMade.getInstance().logException("Error writing file to buffer", exception);
						}
					}
				}
			}
		}
	}

	/**
	 * Recursively reads files and directories from the buffer.
	 * @param currentDir The current directory being processed
	 * @param buffer The buffer to read from
	 * @throws Exception If an error occurs during reading
	 */
	private static void readFromBufferRecursive(File currentDir, ByteBuffer buffer) throws Exception {
		currentDir.mkdirs();
		
		while(buffer.hasRemaining()) {
			byte marker = buffer.get();
			
			if(marker == END_OF_STREAM) {
				break;
			} else if(marker == DIRECTORY) {
				String name = readString(buffer);
				File dir = new File(currentDir, name);
				readFromBufferRecursive(dir, buffer);
			} else if(marker == FILE) {
				String name = readString(buffer);
				int length = buffer.getInt();
				byte[] data = new byte[length];
				buffer.get(data);
				
				File file = new File(currentDir, name);
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(data);
				fos.close();
			} else {
				// Unknown marker, could be end of directory
				buffer.position(buffer.position() - 1);
				break;
			}
		}
	}

	/**
	 * Writes a string to the buffer with a length prefix.
	 * @param str The string to write
	 */
	private static void writeString(String str) {
		byte[] bytes = str.getBytes();
		writeBuffer.putInt(bytes.length);
		writeBuffer.put(bytes);
	}

	/**
	 * Reads a string from the buffer with a length prefix.
	 * @param buffer The buffer to read from
	 * @return The string read from the buffer
	 */
	private static String readString(ByteBuffer buffer) {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return new String(bytes);
	}
}
