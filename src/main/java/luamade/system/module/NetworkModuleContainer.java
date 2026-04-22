package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import luamade.lua.networking.NetworkInterface;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.UUID;

/**
 * Per-entity module that assigns a persistent UUID and hostname to each
 * Network Module block. On entity load the container restores
 * {@link NetworkInterface} instances so that hostnames are discoverable
 * on the network even before a computer wraps the block as a peripheral.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) uuid(String) hostname(String)] * count}
 */
public class NetworkModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	private final Long2ObjectOpenHashMap<String> blockUuids = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<String> pendingHostnames = new Long2ObjectOpenHashMap<>();
	private boolean hasPending = false;

	public NetworkModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.NETWORK_MODULE.getId());
	}

	public static NetworkModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.NETWORK_MODULE.getId()) instanceof NetworkModuleContainer) {
			return (NetworkModuleContainer) managerContainer.getModMCModule(ElementRegistry.NETWORK_MODULE.getId());
		}
		return null;
	}

	/**
	 * Returns the persistent UUID for the Network Module block at {@code absIndex},
	 * creating and storing one if it does not yet exist.
	 */
	public String getOrAssignUuid(long absIndex) {
		String uuid = blockUuids.get(absIndex);
		if(uuid == null) {
			uuid = UUID.randomUUID().toString();
			blockUuids.put(absIndex, uuid);
			flagUpdatedData();
		}
		return uuid;
	}

	public String getUuid(long absIndex) {
		return blockUuids.get(absIndex);
	}

	// -------------------------------------------------------------------------
	// SystemModule lifecycle
	// -------------------------------------------------------------------------

	@Override
	public void handlePlace(long abs, byte orientation) {
		getOrAssignUuid(abs);
	}

	@Override
	public void handleRemove(long abs) {
		String uuid = blockUuids.remove(abs);
		pendingHostnames.remove(abs);
		if(uuid != null) {
			NetworkInterface.remove(uuid);
			flagUpdatedData();
		}
	}

	@Override
	public void handle(Timer timer) {
		if(hasPending) {
			restorePendingModules();
		}
	}

	private void restorePendingModules() {
		long[] keys = pendingHostnames.keySet().toLongArray();
		for(long abs : keys) {
			SegmentPiece piece;
			try {
				piece = segmentController.getSegmentBuffer().getPointUnsave(abs);
			} catch(Exception e) {
				continue;
			}

			if(piece == null || piece.getType() != ElementRegistry.NETWORK_MODULE.getId()) {
				continue;
			}

			String hostname = pendingHostnames.remove(abs);
			String uuid = blockUuids.get(abs);
			if(uuid == null) continue;

			NetworkInterface net = NetworkInterface.getOrCreate(piece, uuid);
			if(hostname != null && !hostname.isEmpty()) {
				net.setHostname(hostname);
			}
		}
		if(pendingHostnames.isEmpty()) {
			hasPending = false;
		}
	}

	// -------------------------------------------------------------------------
	// SystemModule boilerplate
	// -------------------------------------------------------------------------

	@Override
	public double getPowerConsumedPerSecondResting() {
		return 0;
	}

	@Override
	public double getPowerConsumedPerSecondCharging() {
		return 0;
	}

	@Override
	public String getName() {
		return "Network Module";
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		buffer.writeInt(blockUuids.size());
		for(long abs : blockUuids.keySet().toLongArray()) {
			String uuid = blockUuids.get(abs);
			buffer.writeLong(abs);
			buffer.writeString(uuid);

			NetworkInterface net = NetworkInterface.getByUuid(uuid);
			String hostname = net != null ? net.getHostname() : "";
			// Fall back to pending hostname if the interface hasn't been created yet
			if((hostname == null || hostname.isEmpty()) && pendingHostnames.containsKey(abs)) {
				hostname = pendingHostnames.get(abs);
			}
			buffer.writeString(hostname == null ? "" : hostname);
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		blockUuids.clear();
		pendingHostnames.clear();
		byte version = buffer.readByte();
		if(version != VERSION) {
			return;
		}
		int count = buffer.readInt();
		for(int i = 0; i < count; i++) {
			long abs = buffer.readLong();
			String uuid = buffer.readString();
			String hostname = buffer.readString();
			if(uuid != null && !uuid.isEmpty()) {
				blockUuids.put(abs, uuid);
			}
			if(hostname != null && !hostname.isEmpty()) {
				pendingHostnames.put(abs, hostname);
			}
		}
		hasPending = !pendingHostnames.isEmpty();
	}
}
