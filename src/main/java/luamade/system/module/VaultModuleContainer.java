package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import luamade.lua.vault.SharedVaultLedger;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.UUID;

/**
 * Per-entity module that assigns a persistent UUID to each Vault block on that
 * entity. The UUID is used as the key for the vault's balance file in
 * {@link luamade.lua.vault.SharedVaultLedger}.
 *
 * <p>Mirrors {@link DataStoreModuleContainer}. A UUID is generated the first time
 * a vault block is placed or accessed and is never reused — the balance persists
 * independently of the block's position on the entity.
 *
 * <p>When a vault block is broken, the ledger file is deleted so destroyed vaults
 * don't accumulate orphaned credits on disk.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) uuid(String)] * count}
 */
public class VaultModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	private final Long2ObjectOpenHashMap<String> blockUuids = new Long2ObjectOpenHashMap<>();

	public VaultModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.VAULT.getId());
	}

	public static VaultModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.VAULT.getId()) instanceof VaultModuleContainer) {
			return (VaultModuleContainer) managerContainer.getModMCModule(ElementRegistry.VAULT.getId());
		}
		return null;
	}

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
		String uuid = blockUuids.remove(absIndex);
		if(uuid != null) {
			flagUpdatedData();
			// Credits inside a destroyed vault are lost by design — prevents infinite
			// credit mint by "repair"-style exploits where a balance file is preserved
			// while the block is broken and rebuilt.
			SharedVaultLedger.deleteVault(uuid);
		}
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
		// Intentionally do NOT touch the ledger here. removeBlock() is the
		// destruction path; handleRemove also fires on world unload/reload.
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
		return "Vault";
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
		if(version != VERSION) return;
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
