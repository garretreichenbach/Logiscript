package luamade.gui;

import luamade.network.PacketSCComputerConnectAck;
import luamade.network.PacketSCConsoleSnapshot;
import luamade.network.PacketSCGfxSnapshot;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side registry of {@link ComputerSessionView}s, keyed by the computer
 * entity+block-index they represent. Network packet handlers route here;
 * {@link ComputerDialog} reads/writes through the returned view instead of a
 * local {@code ComputerModule}.
 */
public final class ComputerSessionRegistry {

	private static final ConcurrentHashMap<String, ComputerSessionView> sessions = new ConcurrentHashMap<>();

	private ComputerSessionRegistry() {
	}

	private static String key(int entityId, long absIndex) {
		return entityId + ":" + absIndex;
	}

	public static ComputerSessionView getOrCreate(int entityId, long absIndex) {
		return sessions.computeIfAbsent(key(entityId, absIndex), k -> new ComputerSessionView(entityId, absIndex));
	}

	/** Drops the cached view once a dialog closes — the next open starts fresh with a new CONNECT. */
	public static void forget(int entityId, long absIndex) {
		sessions.remove(key(entityId, absIndex));
	}

	public static void handleConnectAck(PacketSCComputerConnectAck ack) {
		ComputerSessionView view = getOrCreate(ack.getEntityId(), ack.getAbsIndex());
		if(ack.isSuccess()) {
			view.applyConnectSuccess(ack.getConsoleText(), ack.getGfxSnapshot(), ack.isKeyboardConsumed(), ack.isMouseConsumed(), ack.getModeOrdinal(), ack.getLastOpenFile(), ack.isPasswordInputMode());
		} else {
			view.applyConnectFailure(ack.getMessage());
		}
	}

	public static void handleConsoleSnapshot(PacketSCConsoleSnapshot packet) {
		ComputerSessionView view = sessions.get(key(packet.getEntityId(), packet.getAbsIndex()));
		if(view != null) {
			view.applyConsoleSnapshot(packet.getText(), packet.isKeyboardConsumed(), packet.isMouseConsumed(), packet.getModeOrdinal(), packet.getLastOpenFile(), packet.isPasswordInputMode());
		}
	}

	public static void handleGfxSnapshot(PacketSCGfxSnapshot packet) {
		ComputerSessionView view = sessions.get(key(packet.getEntityId(), packet.getAbsIndex()));
		if(view != null) {
			view.applyGfxSnapshot(packet.getSnapshot());
		}
	}
}
