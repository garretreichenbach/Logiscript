package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import luamade.lua.datastore.NetworkedDataStoreRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.UUID;

/**
 * Per-entity module that assigns a persistent UUID and tracks the registered
 * name for each Networked Data Store block on the entity.
 *
 * <p>The UUID is used as the backing key in {@link luamade.lua.datastore.SharedDataStore}.
 * The name is registered in {@link NetworkedDataStoreRegistry} for global resolution.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) uuid(String) name(String)] * count}
 */
public class NetworkedDataStoreModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	/** abs block index → UUID */
	private final Long2ObjectOpenHashMap<String> blockUuids = new Long2ObjectOpenHashMap<>();
	/** abs block index → registered name (may be empty if not yet named) */
	private final Long2ObjectOpenHashMap<String> blockNames = new Long2ObjectOpenHashMap<>();

	public NetworkedDataStoreModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.NETWORKED_DATA_STORE.getId());
	}

	public static NetworkedDataStoreModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.NETWORKED_DATA_STORE.getId()) instanceof NetworkedDataStoreModuleContainer) {
			return (NetworkedDataStoreModuleContainer) managerContainer.getModMCModule(ElementRegistry.NETWORKED_DATA_STORE.getId());
		}
		return null;
	}

	/**
	 * Returns the persistent UUID for the block at {@code absIndex},
	 * creating one if it does not yet exist.
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

	/**
	 * Returns the registered name for the block, or {@code null} if not named.
	 */
	public String getName(long absIndex) {
		return blockNames.get(absIndex);
	}

	/**
	 * Sets the registered name for a block. Updates internal tracking only;
	 * the caller is responsible for registry operations.
	 */
	public void setName(long absIndex, String name) {
		if(name == null || name.isEmpty()) {
			blockNames.remove(absIndex);
		} else {
			blockNames.put(absIndex, name);
		}
		flagUpdatedData();
	}

	/**
	 * Removes a block, unregistering its name from the global registry
	 * (which also deletes the backing data).
	 */
	public void removeBlock(long absIndex) {
		String name = blockNames.remove(absIndex);
		blockUuids.remove(absIndex);
		if(name != null && !name.isEmpty()) {
			NetworkedDataStoreRegistry.unregister(name);
		}
		flagUpdatedData();
	}

	// -------------------------------------------------------------------------
	// SystemModule boilerplate
	// -------------------------------------------------------------------------

	@Override
	public void handlePlace(long abs, byte orientation) {
		getOrAssignUuid(abs);
	}

	@Override
	public void handleRemove(long abs) {
		removeBlock(abs);
	}

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
		return "Networked Data Store";
	}

	@Override
	public void handle(Timer timer) {
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		buffer.writeInt(blockUuids.size());
		for(long abs : blockUuids.keySet().toLongArray()) {
			buffer.writeLong(abs);
			buffer.writeString(blockUuids.get(abs));
			String name = blockNames.get(abs);
			buffer.writeString(name != null ? name : "");
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		blockUuids.clear();
		blockNames.clear();
		byte version = buffer.readByte();
		if(version != VERSION) return;
		int count = buffer.readInt();
		for(int i = 0; i < count; i++) {
			long abs = buffer.readLong();
			String uuid = buffer.readString();
			String name = buffer.readString();
			if(uuid != null && !uuid.isEmpty()) {
				blockUuids.put(abs, uuid);
				if(name != null && !name.isEmpty()) {
					blockNames.put(abs, name);
				}
			}
		}
	}
}
