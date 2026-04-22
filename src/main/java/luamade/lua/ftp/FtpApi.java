package luamade.lua.ftp;

import luamade.lua.fs.FileSystem;
import luamade.lua.networking.NetworkInterface;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Built-in FTP utility for transferring files directly between computers.
 * Transfers happen in-memory by sharing FileSystem references, making them far
 * more efficient than line-by-line script-driven messaging.
 *
 * <p><b>Server side:</b>
 * <pre>
 * local nm = peripheral.wrapRelative("front", "networkmodule")
 * local ftp = nm.getFtp()
 * ftp.listen("secret")          -- start accepting connections
 * ftp.listen("secret", true)    -- read-only: clients can only download
 * ftp.stop()                    -- stop serving and disconnect any client
 * </pre>
 *
 * <p><b>Client side:</b>
 * <pre>
 * local nm = peripheral.wrapRelative("front", "networkmodule")
 * local ftp = nm.getFtp()
 * if ftp.connect("server-host", "secret") then
 *     ftp.download("/remote/prog.lua", "/local/prog.lua")
 *     ftp.upload("/local/data.txt", "/remote/data.txt")
 *     ftp.downloadDir("/remote/libs", "/local/libs")
 *     ftp.uploadDir("/local/assets", "/remote/assets")
 *     ftp.disconnect()
 * end
 * </pre>
 *
 * <p>The server's filesystem permission rules are always enforced — protected paths
 * require the server to call {@code fs.auth()} before starting the FTP session.
 */
public class FtpApi extends LuaMadeUserdata {

	/** hostname → server state for all actively listening computers. */
	private static final Map<String, FtpServer> listeningServers = new ConcurrentHashMap<>();

	/** client hostname → server hostname for all active connections. */
	private static final Map<String, String> clientConnections = new ConcurrentHashMap<>();

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final NetworkInterface networkInterface;
	private final FileSystem fileSystem;

	public FtpApi(NetworkInterface networkInterface, FileSystem fileSystem) {
		this.networkInterface = networkInterface;
		this.fileSystem = fileSystem;
	}

	// ─── Identity ────────────────────────────────────────────────────────────────

	private String hostname() {
		return networkInterface.getHostname();
	}

	private FileSystem localFs() {
		return fileSystem;
	}

	// ─── Server API ──────────────────────────────────────────────────────────────

	/**
	 * Start serving files from this computer's filesystem.
	 * Only one client may connect at a time.
	 *
	 * @param password the password clients must supply to connect
	 * @return true if listening started, false if already listening or password is blank
	 */
	@LuaMadeCallable
	public Boolean listen(String password) {
		return listenInternal(password, false);
	}

	/**
	 * Start serving files, optionally restricting clients to read-only access
	 * (downloads/list only; uploads, mkdir, and delete are rejected).
	 *
	 * @param password the password clients must supply to connect
	 * @param readOnly if true, clients cannot upload, mkdir, or delete
	 * @return true if listening started
	 */
	@LuaMadeCallable
	public Boolean listen(String password, Boolean readOnly) {
		return listenInternal(password, readOnly != null && readOnly);
	}

	private boolean listenInternal(String password, boolean readOnly) {
		if(password == null || password.isEmpty()) return false;
		String host = hostname();
		if(listeningServers.containsKey(host)) return false;

		byte[] salt = new byte[16];
		SECURE_RANDOM.nextBytes(salt);
		byte[] hash = hashPassword(password, salt);
		if(hash == null) return false;

		listeningServers.put(host, new FtpServer(localFs(), toHex(salt), toHex(hash), readOnly));
		return true;
	}

	/**
	 * Stop listening and disconnect any connected client.
	 */
	@LuaMadeCallable
	public void stop() {
		String host = hostname();
		FtpServer server = listeningServers.remove(host);
		if(server != null && server.connectedClient != null) {
			clientConnections.remove(server.connectedClient);
		}
	}

	/**
	 * Returns true if this computer is currently listening for FTP connections.
	 */
	@LuaMadeCallable
	public Boolean isListening() {
		return listeningServers.containsKey(hostname());
	}

	/**
	 * Returns the hostname of the currently connected client, or null if no client is connected.
	 */
	@LuaMadeCallable
	public String getConnectedClient() {
		FtpServer server = listeningServers.get(hostname());
		return server != null ? server.connectedClient : null;
	}

	// ─── Client API ──────────────────────────────────────────────────────────────

	/**
	 * Connect to a listening FTP server on another computer.
	 *
	 * @param serverHostname the hostname of the server
	 * @param password       the server's FTP password
	 * @return true if the connection was established
	 */
	@LuaMadeCallable
	public Boolean connect(String serverHostname, String password) {
		if(serverHostname == null || password == null) return false;
		String myHost = hostname();
		if(clientConnections.containsKey(myHost)) return false;

		FtpServer server = listeningServers.get(serverHostname);
		if(server == null || server.connectedClient != null) return false;
		if(!verifyPassword(password, server.saltHex, server.hashHex)) return false;

		server.connectedClient = myHost;
		clientConnections.put(myHost, serverHostname);
		return true;
	}

	/**
	 * Disconnect from the current FTP server.
	 */
	@LuaMadeCallable
	public void disconnect() {
		String myHost = hostname();
		String serverHostname = clientConnections.remove(myHost);
		if(serverHostname != null) {
			FtpServer server = listeningServers.get(serverHostname);
			if(server != null && myHost.equals(server.connectedClient)) {
				server.connectedClient = null;
			}
		}
	}

	/**
	 * Returns true if this computer has an active FTP connection.
	 */
	@LuaMadeCallable
	public Boolean isConnected() {
		return clientConnections.containsKey(hostname());
	}

	/**
	 * Returns the hostname of the server this computer is connected to, or null.
	 */
	@LuaMadeCallable
	public String getServer() {
		return clientConnections.get(hostname());
	}

	// ─── Remote file inspection ───────────────────────────────────────────────────

	/**
	 * Returns true if the remote path exists.
	 */
	@LuaMadeCallable
	public Boolean exists(String remotePath) {
		FtpServer server = connectedServer();
		return server != null && server.fileSystem.exists(remotePath);
	}

	/**
	 * Returns true if the remote path is a directory.
	 */
	@LuaMadeCallable
	public Boolean isDir(String remotePath) {
		FtpServer server = connectedServer();
		return server != null && server.fileSystem.isDir(remotePath);
	}

	/**
	 * Lists the names of files and directories at the remote path.
	 * Returns null if not connected or the path is not a directory.
	 */
	@LuaMadeCallable
	public String[] list(String remotePath) {
		FtpServer server = connectedServer();
		if(server == null) return null;
		List<String> names = server.fileSystem.list(remotePath);
		return names.toArray(new String[0]);
	}

	// ─── Remote mutations ─────────────────────────────────────────────────────────

	/**
	 * Creates a directory at the remote path.
	 * Fails if the server is read-only or not connected.
	 */
	@LuaMadeCallable
	public Boolean mkdir(String remotePath) {
		FtpServer server = connectedServer();
		if(server == null || server.readOnly) return false;
		return server.fileSystem.makeDir(remotePath);
	}

	/**
	 * Deletes a remote file or directory.
	 * Fails if the server is read-only or not connected.
	 */
	@LuaMadeCallable
	public Boolean delete(String remotePath) {
		FtpServer server = connectedServer();
		if(server == null || server.readOnly) return false;
		return server.fileSystem.delete(remotePath);
	}

	// ─── File transfer ────────────────────────────────────────────────────────────

	/**
	 * Downloads a single file from the server to the local filesystem.
	 *
	 * @param remotePath path of the file on the server
	 * @param localPath  destination path on this computer
	 * @return true if the transfer succeeded
	 */
	@LuaMadeCallable
	public Boolean download(String remotePath, String localPath) {
		FtpServer server = connectedServer();
		if(server == null) return false;
		String content = server.fileSystem.read(remotePath);
		if(content == null) return false;
		return localFs().write(localPath, content);
	}

	/**
	 * Uploads a single file from the local filesystem to the server.
	 *
	 * @param localPath  path of the file on this computer
	 * @param remotePath destination path on the server
	 * @return true if the transfer succeeded
	 */
	@LuaMadeCallable
	public Boolean upload(String localPath, String remotePath) {
		FtpServer server = connectedServer();
		if(server == null || server.readOnly) return false;
		String content = localFs().read(localPath);
		if(content == null) return false;
		return server.fileSystem.write(remotePath, content);
	}

	/**
	 * Recursively downloads an entire directory from the server.
	 *
	 * @param remotePath source directory on the server
	 * @param localPath  destination directory on this computer (created if absent)
	 * @return number of files transferred, or -1 if not connected / source is not a directory
	 */
	@LuaMadeCallable
	public Integer downloadDir(String remotePath, String localPath) {
		FtpServer server = connectedServer();
		if(server == null) return -1;
		if(!server.fileSystem.isDir(remotePath)) return -1;
		int[] count = {0};
		transferDir(server.fileSystem, remotePath, localFs(), localPath, count);
		return count[0];
	}

	/**
	 * Recursively uploads an entire local directory to the server.
	 *
	 * @param localPath  source directory on this computer
	 * @param remotePath destination directory on the server (created if absent)
	 * @return number of files transferred, or -1 if not connected / read-only / source is not a directory
	 */
	@LuaMadeCallable
	public Integer uploadDir(String localPath, String remotePath) {
		FtpServer server = connectedServer();
		if(server == null || server.readOnly) return -1;
		if(!localFs().isDir(localPath)) return -1;
		int[] count = {0};
		transferDir(localFs(), localPath, server.fileSystem, remotePath, count);
		return count[0];
	}

	// ─── Static lifecycle ─────────────────────────────────────────────────────────

	/**
	 * Clean up all FTP state for a hostname that is being removed.
	 * Called when a Network Module block is destroyed or its NetworkInterface
	 * is cleaned up.
	 *
	 * @param hostname the hostname being removed
	 */
	public static void onHostnameRemoved(String hostname) {
		// Remove server registration and disconnect any connected client
		FtpServer server = listeningServers.remove(hostname);
		if(server != null && server.connectedClient != null) {
			clientConnections.remove(server.connectedClient);
		}
		// Remove client connection and notify the server
		String serverHostname = clientConnections.remove(hostname);
		if(serverHostname != null) {
			FtpServer serverEntry = listeningServers.get(serverHostname);
			if(serverEntry != null && hostname.equals(serverEntry.connectedClient)) {
				serverEntry.connectedClient = null;
			}
		}
	}

	// ─── Internal helpers ─────────────────────────────────────────────────────────

	private FtpServer connectedServer() {
		String serverHostname = clientConnections.get(hostname());
		if(serverHostname == null) return null;
		return listeningServers.get(serverHostname);
	}

	private static void transferDir(FileSystem src, String srcPath, FileSystem dst, String dstPath, int[] count) {
		dst.makeDir(dstPath);
		List<String> names = src.list(srcPath);
		if(names == null) return;
		for(String name : names) {
			String srcEntry = joinPath(srcPath, name);
			String dstEntry = joinPath(dstPath, name);
			if(src.isDir(srcEntry)) {
				transferDir(src, srcEntry, dst, dstEntry, count);
			} else {
				String content = src.read(srcEntry);
				if(content != null && dst.write(dstEntry, content)) {
					count[0]++;
				}
			}
		}
	}

	private static String joinPath(String base, String name) {
		return base.endsWith("/") ? base + name : base + "/" + name;
	}

	private static byte[] hashPassword(String password, byte[] salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			return md.digest(password.getBytes(StandardCharsets.UTF_8));
		} catch(NoSuchAlgorithmException ignored) {
			return null;
		}
	}

	private static boolean verifyPassword(String password, String saltHex, String hashHex) {
		byte[] salt = fromHex(saltHex);
		byte[] hash = hashPassword(password, salt);
		return hash != null && toHex(hash).equals(hashHex);
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for(byte b : bytes) {
			sb.append(String.format("%02x", b & 0xff));
		}
		return sb.toString();
	}

	private static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for(int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}

	// ─── Inner class ─────────────────────────────────────────────────────────────

	private static final class FtpServer {
		final FileSystem fileSystem;
		final String saltHex;
		final String hashHex;
		final boolean readOnly;
		volatile String connectedClient;

		FtpServer(FileSystem fileSystem, String saltHex, String hashHex, boolean readOnly) {
			this.fileSystem = fileSystem;
			this.saltHex = saltHex;
			this.hashHex = hashHex;
			this.readOnly = readOnly;
		}
	}
}
