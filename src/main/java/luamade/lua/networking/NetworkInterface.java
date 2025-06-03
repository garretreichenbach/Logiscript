package luamade.lua.networking;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements networking capabilities for computers in the game.
 * This allows computers to communicate with each other using various protocols.
 */
public class NetworkInterface extends LuaMadeUserdata {

	private final SegmentPiece segmentPiece;
	private String hostname;
	private static final Map<String, NetworkInterface> networkInterfaces = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, MessageQueue>> messageQueues = new ConcurrentHashMap<>();

	public NetworkInterface(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
		long absIndex = segmentPiece.getAbsoluteIndex();
		hostname = "computer-" + ComputerModule.generateComputerUUID(absIndex);
		networkInterfaces.put(hostname, this);
		messageQueues.put(hostname, new HashMap<String, MessageQueue>());
	}

	/**
	 * Sets the hostname of this computer
	 */
	@LuaMadeCallable
	public boolean setHostname(String hostname) {
		if(hostname == null || hostname.isEmpty() || hostname.contains(" ")) return false;

		// Check if hostname is already taken
		if(networkInterfaces.containsKey(hostname) && !hostname.equals(this.hostname)) return false;

		// Update hostname
		networkInterfaces.remove(this.hostname);
		Map<String, MessageQueue> queues = messageQueues.remove(this.hostname);
		this.hostname = hostname;
		networkInterfaces.put(hostname, this);
		messageQueues.put(hostname, queues);

		return true;
	}

	/**
	 * Gets the hostname of this computer
	 */
	@LuaMadeCallable
	public String getHostname() {
		return hostname;
	}

	/**
	 * Sends a message to another computer
	 */
	@LuaMadeCallable
	public boolean send(String targetHostname, String protocol, String message) {
		if(targetHostname == null || targetHostname.isEmpty() || protocol == null || protocol.isEmpty() || message == null) {
			return false;
		}

		// Check if target exists
		if(!networkInterfaces.containsKey(targetHostname)) {
			return false;
		}

		// Get or create message queue for the protocol
		Map<String, MessageQueue> targetQueues = messageQueues.get(targetHostname);
		if(!targetQueues.containsKey(protocol)) {
			targetQueues.put(protocol, new MessageQueue());
		}

		// Add message to queue
		MessageQueue queue = targetQueues.get(protocol);
		queue.addMessage(new Message(hostname, message));

		return true;
	}

	/**
	 * Broadcasts a message to all computers
	 */
	@LuaMadeCallable
	public void broadcast(String protocol, String message) {
		if(protocol == null || protocol.isEmpty() || message == null) {
			return;
		}

		for(String targetHostname : networkInterfaces.keySet()) {
			if(!targetHostname.equals(hostname)) {
				send(targetHostname, protocol, message);
			}
		}
	}

	/**
	 * Receives a message from the specified protocol queue
	 */
	@LuaMadeCallable
	public Message receive(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return null;
		}

		// Check if queue exists
		Map<String, MessageQueue> queues = messageQueues.get(hostname);
		if(!queues.containsKey(protocol)) {
			queues.put(protocol, new MessageQueue());
			return null;
		}

		// Get message from queue
		MessageQueue queue = queues.get(protocol);
		return queue.getMessage();
	}

	/**
	 * Checks if there are any messages in the specified protocol queue
	 */
	@LuaMadeCallable
	public boolean hasMessage(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return false;
		}

		// Check if queue exists
		Map<String, MessageQueue> queues = messageQueues.get(hostname);
		if(!queues.containsKey(protocol)) {
			queues.put(protocol, new MessageQueue());
			return false;
		}

		// Check if queue has messages
		MessageQueue queue = queues.get(protocol);
		return !queue.isEmpty();
	}

	/**
	 * Gets a list of all hostnames on the network
	 */
	@LuaMadeCallable
	public String[] getHostnames() {
		return networkInterfaces.keySet().toArray(new String[0]);
	}

	/**
	 * Checks if a hostname exists on the network
	 */
	@LuaMadeCallable
	public boolean isHostnameAvailable(String hostname) {
		return !networkInterfaces.containsKey(hostname);
	}

	/**
	 * Pings another computer to check if it's reachable
	 */
	@LuaMadeCallable
	public boolean ping(String targetHostname) {
		return networkInterfaces.containsKey(targetHostname);
	}

	/**
	 * Message class for network communication
	 */
	public static class Message extends LuaMadeUserdata {
		private final String sender;
		private final String content;
		private final long timestamp;

		public Message(String sender, String content) {
			this.sender = sender;
			this.content = content;
			timestamp = System.currentTimeMillis();
		}

		@LuaMadeCallable
		public String getSender() {
			return sender;
		}

		@LuaMadeCallable
		public String getContent() {
			return content;
		}

		@LuaMadeCallable
		public long getTimestamp() {
			return timestamp;
		}
	}

	/**
	 * Message queue for storing messages
	 */
	private static class MessageQueue {
		private final java.util.Queue<Message> queue = new LinkedList<>();

		public void addMessage(Message message) {
			queue.add(message);
		}

		public Message getMessage() {
			return queue.poll();
		}

		public boolean isEmpty() {
			return queue.isEmpty();
		}
	}
}
