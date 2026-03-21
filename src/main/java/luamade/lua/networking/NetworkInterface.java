package luamade.lua.networking;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.schema.common.util.linAlg.Vector3i;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implements networking capabilities for computers in the game.
 * Supports direct messages, galaxy-wide channels, local same-sector channels,
 * and explicit long-range 1-to-1 modem links.
 */
public class NetworkInterface extends LuaMadeUserdata {

	private static final String MAILBOX_DIRECT_PREFIX = "direct:";
	private static final String MAILBOX_CHANNEL_PREFIX = "channel:";
	private static final String MAILBOX_LOCAL_PREFIX = "local:";
	private static final String MAILBOX_ENTITY_PREFIX = "entity:";
	private static final String MAILBOX_COMPUTER_PREFIX = "computer:";
	private static final String MAILBOX_MODEM = "modem";

	private final ComputerModule module;
	private String hostname;
	private static final Map<String, NetworkInterface> networkInterfaces = new ConcurrentHashMap<>();
	private static final Map<String, Map<String, MessageQueue>> messageQueues = new ConcurrentHashMap<>();
	private static final Map<String, Channel> globalChannels = new ConcurrentHashMap<>();
	private static final Map<String, Channel> localChannels = new ConcurrentHashMap<>();
	private static final Map<String, ModemEndpoint> modemEndpoints = new ConcurrentHashMap<>();
	private static final Map<String, ModemConnection> modemConnections = new ConcurrentHashMap<>();

	public NetworkInterface(ComputerModule module) {
		this.module = module;
		hostname = "computer-" + module.getUUID();
		networkInterfaces.put(hostname, this);
		messageQueues.put(hostname, new ConcurrentHashMap<String, MessageQueue>());
	}

	/**
	 * Sets the hostname of this computer
	 */
	@LuaMadeCallable
	public boolean setHostname(String hostname) {
		if(hostname == null || hostname.isEmpty() || hostname.contains(" ")) return false;

		// Check if hostname is already taken
		if(networkInterfaces.containsKey(hostname) && !hostname.equals(this.hostname)) return false;

		String oldHostname = this.hostname;
		disconnectModemInternal(oldHostname);
		migrateChannelSubscriptions(oldHostname, hostname, globalChannels);
		migrateChannelSubscriptions(oldHostname, hostname, localChannels);
		ModemEndpoint endpoint = modemEndpoints.remove(oldHostname);
		if(endpoint != null) {
			endpoint.setHostname(hostname);
			modemEndpoints.put(hostname, endpoint);
		}

		// Update hostname
		networkInterfaces.remove(oldHostname);
		Map<String, MessageQueue> queues = messageQueues.remove(oldHostname);
		this.hostname = hostname;
		networkInterfaces.put(hostname, this);
		messageQueues.put(hostname, queues == null ? new ConcurrentHashMap<>() : queues);

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
		MessageQueue queue = getOrCreateMailbox(targetHostname, directMailbox(protocol));
		queue.addMessage(new Message(hostname, message, protocol, "direct"));

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
		return getOrCreateMailbox(hostname, directMailbox(protocol)).getMessage();
	}

	/**
	 * Checks if there are any messages in the specified protocol queue
	 */
	@LuaMadeCallable
	public boolean hasMessage(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return false;
		}
		return !getOrCreateMailbox(hostname, directMailbox(protocol)).isEmpty();
	}

	@LuaMadeCallable
	public boolean openChannel(String channelName, String password) {
		return subscribeToChannel(globalChannels, channelName, password);
	}

	@LuaMadeCallable
	public boolean closeChannel(String channelName) {
		return unsubscribeFromChannel(globalChannels, channelName);
	}

	@LuaMadeCallable
	public boolean sendChannel(String channelName, String password, String message) {
		Channel channel = globalChannels.get(normalizeChannelName(channelName));
		if(channel == null || !channel.matchesPassword(password)) {
			return false;
		}

		int delivered = 0;
		for(String subscriber : channel.getSubscribers()) {
			if(subscriber.equals(hostname)) {
				continue;
			}
			if(networkInterfaces.containsKey(subscriber)) {
				getOrCreateMailbox(subscriber, channelMailbox(channel.getName())).addMessage(new Message(hostname, message, channel.getName(), "channel"));
				delivered++;
			}
		}
		return delivered > 0;
	}

	@LuaMadeCallable
	public Message receiveChannel(String channelName) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return null;
		}
		return getOrCreateMailbox(hostname, channelMailbox(normalized)).getMessage();
	}

	@LuaMadeCallable
	public boolean hasChannelMessage(String channelName) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return false;
		}
		return !getOrCreateMailbox(hostname, channelMailbox(normalized)).isEmpty();
	}

	@LuaMadeCallable
	public boolean openLocalChannel(String channelName, String password) {
		return subscribeToChannel(localChannels, channelName, password);
	}

	@LuaMadeCallable
	public boolean closeLocalChannel(String channelName) {
		return unsubscribeFromChannel(localChannels, channelName);
	}

	@LuaMadeCallable
	public boolean sendLocal(String channelName, String password, String message) {
		Channel channel = localChannels.get(normalizeChannelName(channelName));
		if(channel == null || !channel.matchesPassword(password)) {
			return false;
		}

		String senderSector = getSectorKey();
		int delivered = 0;
		for(String subscriber : channel.getSubscribers()) {
			if(subscriber.equals(hostname)) {
				continue;
			}
			NetworkInterface target = networkInterfaces.get(subscriber);
			if(target != null && senderSector.equals(target.getSectorKey())) {
				getOrCreateMailbox(subscriber, localMailbox(channel.getName())).addMessage(new Message(hostname, message, channel.getName(), "local"));
				delivered++;
			}
		}
		return delivered > 0;
	}

	@LuaMadeCallable
	public Message receiveLocal(String channelName) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return null;
		}
		return getOrCreateMailbox(hostname, localMailbox(normalized)).getMessage();
	}

	@LuaMadeCallable
	public boolean hasLocalMessage(String channelName) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return false;
		}
		return !getOrCreateMailbox(hostname, localMailbox(normalized)).isEmpty();
	}

	/**
	 * Sends a protocol message to all other computers on the same entity (ship/station).
	 */
	@LuaMadeCallable
	public boolean sendEntity(String protocol, String message) {
		if(protocol == null || protocol.isEmpty() || message == null) {
			return false;
		}

		String senderEntity = getEntityKey();
		int delivered = 0;
		for(NetworkInterface target : networkInterfaces.values()) {
			if(target == null || target == this) {
				continue;
			}
			if(!senderEntity.equals(target.getEntityKey())) {
				continue;
			}

			getOrCreateMailbox(target.hostname, entityMailbox(protocol)).addMessage(new Message(hostname, message, protocol, "entity"));
			delivered++;
		}
		return delivered > 0;
	}

	@LuaMadeCallable
	public Message receiveEntity(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return null;
		}
		return getOrCreateMailbox(hostname, entityMailbox(protocol)).getMessage();
	}

	@LuaMadeCallable
	public boolean hasEntityMessage(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return false;
		}
		return !getOrCreateMailbox(hostname, entityMailbox(protocol)).isEmpty();
	}

	/**
	 * Sends a protocol message to this computer only.
	 * Useful for foreground/background script coordination.
	 */
	@LuaMadeCallable
	public boolean sendComputer(String protocol, String message) {
		if(protocol == null || protocol.isEmpty() || message == null) {
			return false;
		}

		getOrCreateMailbox(hostname, computerMailbox(protocol)).addMessage(new Message(hostname, message, protocol, "computer"));
		return true;
	}

	@LuaMadeCallable
	public Message receiveComputer(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return null;
		}
		return getOrCreateMailbox(hostname, computerMailbox(protocol)).getMessage();
	}

	@LuaMadeCallable
	public boolean hasComputerMessage(String protocol) {
		if(protocol == null || protocol.isEmpty()) {
			return false;
		}
		return !getOrCreateMailbox(hostname, computerMailbox(protocol)).isEmpty();
	}

	@LuaMadeCallable
	public boolean openModem(String password) {
		ModemConnection activeConnection = modemConnections.get(hostname);
		if(activeConnection != null) {
			return false;
		}

		modemEndpoints.put(hostname, new ModemEndpoint(hostname, sanitizePassword(password)));
		return true;
	}

	@LuaMadeCallable
	public boolean closeModem() {
		modemEndpoints.remove(hostname);
		disconnectModemInternal(hostname);
		return true;
	}

	@LuaMadeCallable
	public boolean connectModem(String targetHostname, String password) {
		if(targetHostname == null || targetHostname.isEmpty() || targetHostname.equals(hostname)) {
			return false;
		}
		if(!networkInterfaces.containsKey(targetHostname)) {
			return false;
		}
		if(modemConnections.containsKey(hostname) || modemConnections.containsKey(targetHostname)) {
			return false;
		}

		ModemEndpoint endpoint = modemEndpoints.get(targetHostname);
		if(endpoint == null || !endpoint.matchesPassword(password)) {
			return false;
		}

		ModemConnection connection = new ModemConnection(hostname, targetHostname);
		modemConnections.put(hostname, connection);
		modemConnections.put(targetHostname, connection);
		return true;
	}

	@LuaMadeCallable
	public boolean disconnectModem() {
		return disconnectModemInternal(hostname);
	}

	@LuaMadeCallable
	public boolean isModemConnected() {
		return modemConnections.containsKey(hostname);
	}

	@LuaMadeCallable
	public String getModemPeer() {
		ModemConnection connection = modemConnections.get(hostname);
		if(connection == null) {
			return null;
		}
		return connection.getPeer(hostname);
	}

	@LuaMadeCallable
	public boolean sendModem(String message) {
		if(message == null) {
			return false;
		}
		ModemConnection connection = modemConnections.get(hostname);
		if(connection == null) {
			return false;
		}
		String peer = connection.getPeer(hostname);
		if(peer == null || !networkInterfaces.containsKey(peer)) {
			return false;
		}
		getOrCreateMailbox(peer, MAILBOX_MODEM).addMessage(new Message(hostname, message, peer, "modem"));
		return true;
	}

	@LuaMadeCallable
	public Message receiveModem() {
		return getOrCreateMailbox(hostname, MAILBOX_MODEM).getMessage();
	}

	@LuaMadeCallable
	public boolean hasModemMessage() {
		return !getOrCreateMailbox(hostname, MAILBOX_MODEM).isEmpty();
	}

	/**
	 * Gets a list of all hostnames on the network
	 */
	@LuaMadeCallable
	public String[] getHostnames() {
		return networkInterfaces.keySet().toArray(new String[0]);
	}

	@LuaMadeCallable
	public String getCurrentSector() {
		return getSectorKey();
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

	private boolean subscribeToChannel(Map<String, Channel> registry, String channelName, String password) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return false;
		}

		String normalizedPassword = sanitizePassword(password);
		Channel created = new Channel(normalized, normalizedPassword);
		Channel channel = registry.putIfAbsent(normalized, created);
		if(channel == null) {
			channel = created;
		}

		if(!channel.matchesPassword(normalizedPassword)) {
			return false;
		}

		channel.getSubscribers().add(hostname);
		return true;
	}

	private boolean unsubscribeFromChannel(Map<String, Channel> registry, String channelName) {
		String normalized = normalizeChannelName(channelName);
		if(normalized == null) {
			return false;
		}

		Channel channel = registry.get(normalized);
		if(channel == null) {
			return false;
		}

		boolean removed = channel.getSubscribers().remove(hostname);
		if(channel.getSubscribers().isEmpty()) {
			registry.remove(normalized);
		}
		return removed;
	}

	private void migrateChannelSubscriptions(String oldHostname, String newHostname, Map<String, Channel> registry) {
		for(Channel channel : registry.values()) {
			if(channel.getSubscribers().remove(oldHostname)) {
				channel.getSubscribers().add(newHostname);
			}
		}
	}

	private boolean disconnectModemInternal(String sourceHostname) {
		ModemConnection connection = modemConnections.remove(sourceHostname);
		if(connection == null) {
			return false;
		}

		String peer = connection.getPeer(sourceHostname);
		if(peer != null) {
			modemConnections.remove(peer);
		}
		return true;
	}

	private MessageQueue getOrCreateMailbox(String targetHostname, String mailbox) {
		Map<String, MessageQueue> queues = messageQueues.computeIfAbsent(targetHostname, key -> new ConcurrentHashMap<String, MessageQueue>());
		return queues.computeIfAbsent(mailbox, key -> new MessageQueue());
	}

	private String directMailbox(String protocol) {
		return MAILBOX_DIRECT_PREFIX + protocol;
	}

	private String channelMailbox(String channelName) {
		return MAILBOX_CHANNEL_PREFIX + channelName;
	}

	private String localMailbox(String channelName) {
		return MAILBOX_LOCAL_PREFIX + channelName;
	}

	private String entityMailbox(String protocol) {
		return MAILBOX_ENTITY_PREFIX + protocol;
	}

	private String computerMailbox(String protocol) {
		return MAILBOX_COMPUTER_PREFIX + protocol;
	}

	private String normalizeChannelName(String channelName) {
		if(channelName == null) {
			return null;
		}
		String normalized = channelName.trim().toLowerCase();
		if(normalized.isEmpty() || normalized.contains(" ")) {
			return null;
		}
		return normalized;
	}

	private String sanitizePassword(String password) {
		return password == null ? "" : password;
	}

	private String getSectorKey() {
		Vector3i sector = module.getSegmentPiece().getSegmentController().getSector(new Vector3i());
		return sector.x + ":" + sector.y + ":" + sector.z;
	}

	private String getEntityKey() {
		return String.valueOf(module.getSegmentPiece().getSegmentController().getId());
	}

	/**
	 * Message class for network communication
	 */
	public static class Message extends LuaMadeUserdata {
		private final String sender;
		private final String content;
		private final String route;
		private final String transport;
		private final long timestamp;

		public Message(String sender, String content, String route, String transport) {
			this.sender = sender;
			this.content = content;
			this.route = route;
			this.transport = transport;
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
		public String getRoute() {
			return route;
		}

		@LuaMadeCallable
		public String getTransport() {
			return transport;
		}

		@LuaMadeCallable
		public long getTimestamp() {
			return timestamp;
		}
	}

	private static final class Channel {
		private final String name;
		private final String password;
		private final Set<String> subscribers = ConcurrentHashMap.newKeySet();

		private Channel(String name, String password) {
			this.name = name;
			this.password = password;
		}

		private String getName() {
			return name;
		}

		private Set<String> getSubscribers() {
			return subscribers;
		}

		private boolean matchesPassword(String suppliedPassword) {
			String normalized = suppliedPassword == null ? "" : suppliedPassword;
			return password.equals(normalized);
		}
	}

	private static final class ModemEndpoint {
		private volatile String hostname;
		private final String password;

		private ModemEndpoint(String hostname, String password) {
			this.hostname = hostname;
			this.password = password;
		}

		private boolean matchesPassword(String suppliedPassword) {
			String normalized = suppliedPassword == null ? "" : suppliedPassword;
			return password.equals(normalized);
		}

		private void setHostname(String hostname) {
			this.hostname = hostname;
		}
	}

	private static final class ModemConnection {
		private final String a;
		private final String b;

		private ModemConnection(String a, String b) {
			this.a = a;
			this.b = b;
		}

		private String getPeer(String hostname) {
			if(a.equals(hostname)) {
				return b;
			}
			if(b.equals(hostname)) {
				return a;
			}
			return null;
		}
	}

	/**
	 * Message queue for storing messages
	 */
	private static class MessageQueue {
		private final java.util.Queue<Message> queue = new ConcurrentLinkedQueue<>();

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
