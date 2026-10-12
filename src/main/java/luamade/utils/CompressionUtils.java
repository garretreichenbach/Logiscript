package luamade.utils;

import luamade.LuaMade;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Utilities for compressing and decompressing file systems for computer data storage.
 *
 * @author Garret Reichenbach
 */
public class CompressionUtils {

	private static final byte END_OF_STREAM = -1;
	private static final byte END_DIRECTORY = -2;
	private static final byte DIRECTORY = -3;
	private static final byte FILE = -4;

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

		// Read compressed data from file - use Files.readAllBytes for complete read
		byte[] compressedData = Files.readAllBytes(source.toPath());

		// Read the uncompressed size from the first 4 bytes
		ByteBuffer sizeBuffer = ByteBuffer.wrap(compressedData, 0, 4);
		int uncompressedSize = sizeBuffer.getInt();

		// Decompress the data
		LZ4FastDecompressor decompressor = LZ4Factory.fastestInstance().fastDecompressor();
		byte[] decompressedData = new byte[uncompressedSize];
		decompressor.decompress(compressedData, 4, decompressedData, 0, uncompressedSize);

		// Read the decompressed data and recreate the file system
		readFromBufferRecursive(destination, ByteBuffer.wrap(decompressedData));
	}

	/**
	 * Combines the contents of a folder into a single file and then compresses it.
	 * @param source The folder to compress
	 * @param destination The file to write the compressed data to
	 * @throws Exception If an error occurs during compression
	 */
	public static void compressFS(File source, File destination) throws Exception {
		//First, we combine all the files in the folder into a single growable buffer
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bytes);
		writeToBufferRecursive(source, out);
		out.writeByte(END_OF_STREAM);
		out.flush();

		byte[] uncompressedData = bytes.toByteArray();
		int uncompressedSize = uncompressedData.length;

		//Now compress the data
		LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
		int maxCompressedLength = compressor.maxCompressedLength(uncompressedSize);
		byte[] compressedData = new byte[maxCompressedLength];
		int compressedSize = compressor.compress(uncompressedData, 0, uncompressedSize, compressedData, 0);

		// Write compressed data to file (with uncompressed size prepended)
		destination.getParentFile().mkdirs();
		try(FileOutputStream fos = new FileOutputStream(destination)) {
			// Write uncompressed size first (4 bytes)
			fos.write((uncompressedSize >> 24) & 0xFF);
			fos.write((uncompressedSize >> 16) & 0xFF);
			fos.write((uncompressedSize >> 8) & 0xFF);
			fos.write(uncompressedSize & 0xFF);
			// Write compressed data
			fos.write(compressedData, 0, compressedSize);
		}
	}

	/**
	 * Recursively writes files and directories to the buffer.
	 * @param file The file or directory to write
	 */
	private static void writeToBufferRecursive(File file, DataOutputStream out) throws IOException {
		if(file.isDirectory()) {
			File[] children = file.listFiles();
			if(children != null) {
				for(File child : children) {
					if(child.isDirectory()) {
						//Write the directory marker and name
						out.writeByte(DIRECTORY);
						writeString(out, child.getName());
						writeToBufferRecursive(child, out);
						out.writeByte(END_DIRECTORY);
					} else {
						//Write the file marker and name
						//Read the file first so a failed read never leaves a half-written entry
						byte[] data;
						try {
							data = Files.readAllBytes(child.toPath());
						} catch(Exception exception) {
							LuaMade.getInstance().logException("Error writing file to buffer", exception);
							continue;
						}
						out.writeByte(FILE);
						writeString(out, child.getName());
						out.writeInt(data.length);
						out.write(data);
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
			
			if(marker == END_OF_STREAM || marker == END_DIRECTORY) {
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
				try(FileOutputStream fos = new FileOutputStream(file)) {
					fos.write(data);
				}
			} else {
				// Unknown marker
				LuaMade.getInstance().logWarning("Unknown marker in compressed file system: " + marker);
				break;
			}
		}
	}

	/**
	 * Writes a string to the buffer with a length prefix.
	 * @param str The string to write
	 */
	private static void writeString(DataOutputStream out, String str) throws IOException {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		out.writeInt(bytes.length);
		out.write(bytes);
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
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
