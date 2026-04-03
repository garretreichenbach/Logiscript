package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.UUID;

/**
 * Per-entity module that assigns a persistent UUID to each DataStore block on
 * that entity. The UUID is used as the key for the block's data file in
 * {@link luamade.lua.datastore.SharedDataStore}.
 *
 * <p>A UUID is generated the first time a block is placed or first accessed and
 * is never reused, so data persists independently of the block's position within
 * an entity.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) uuid(String)] * count}
 */
public class DataStoreModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	private final Long2ObjectOpenHashMap<String> blockUuids = new Long2ObjectOpenHashMap<>();

	public DataStoreModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.DATA_STORE.getId());
	}

	public static DataStoreModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.DATA_STORE.getId()) instanceof DataStoreModuleContainer) {
			return (DataStoreModuleContainer) managerContainer.getModMCModule(ElementRegistry.DATA_STORE.getId());
		}
		return null;
	}

	/**
	 * Returns the persistent UUID for the DataStore block at {@code absIndex},
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

	public void removeBlock(long absIndex) {
		if(blockUuids.remove(absIndex) != null) {
			flagUpdatedData();
		}
	}

	// -------------------------------------------------------------------------
	// SystemModule boilerplate
	// -------------------------------------------------------------------------

	@Override
	public void handlePlace(long abs, byte orientation) {
		// Eagerly assign a UUID so it's stable from the moment the block is placed.
		getOrAssignUuid(abs);
	}

	@Override
	public void handleRemove(long abs) {
		blockUuids.remove(abs);
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
		return "Data Store";
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
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		blockUuids.clear();
		byte version = buffer.readByte();
		if(version != VERSION) {
			return;
		}
		int count = buffer.readInt();
		for(int i = 0; i < count; i++) {
			long abs = buffer.readLong();
			String uuid = buffer.readString();
			if(uuid != null && !uuid.isEmpty()) {
				blockUuids.put(abs, uuid);
			}
		}
	}
}
