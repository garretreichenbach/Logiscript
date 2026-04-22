package luamade.lua.datastore;

import luamade.lua.datastore.NetworkedDataStoreRegistry.AccessLevel;
import luamade.lua.datastore.NetworkedDataStoreRegistry.StoreEntry;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.data.SegmentPiece;

import java.util.List;

/**
 * A lightweight remote handle to a Networked Data Store, resolved by name
 * through the global registry. Does not require the owning entity to be loaded.
 *
 * <p>Returned by {@link luamade.lua.networking.NetworkInterface#getDataStore}.
 * Provides read/write access subject to the store's configured access level.
 */
@LuaMadeClass("RemoteDataStore")
public class RemoteDataStoreHandle extends LuaMadeUserdata {

	private final String storeName;
	private final SegmentPiece segmentPiece;

	public RemoteDataStoreHandle(String storeName, SegmentPiece segmentPiece) {
		this.storeName = storeName;
		this.segmentPiece = segmentPiece;
	}

	// -------------------------------------------------------------------------
	// Data access
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public String getValue(String key) {
		checkAccess();
		return SharedDataStore.get(resolveUuid(), key);
	}

	@LuaMadeCallable
	public void set(String key, String value) {
		checkAccess();
		SharedDataStore.set(resolveUuid(), key, value);
	}

	@LuaMadeCallable
	public Boolean delete(String key) {
		checkAccess();
		return SharedDataStore.delete(resolveUuid(), key);
	}

	@LuaMadeCallable
	public Boolean has(String key) {
		checkAccess();
		return SharedDataStore.containsKey(resolveUuid(), key);
	}

	@LuaMadeCallable
	public LuaTable keys() {
		checkAccess();
		return toTable(SharedDataStore.keys(resolveUuid(), null));
	}

	@LuaMadeCallable
	public LuaTable keys(String prefix) {
		checkAccess();
		return toTable(SharedDataStore.keys(resolveUuid(), prefix));
	}

	@LuaMadeCallable
	public Integer size() {
		checkAccess();
		return SharedDataStore.size(resolveUuid());
	}

	// -------------------------------------------------------------------------
	// Info
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public String getStoreName() {
		return storeName;
	}

	@LuaMadeCallable
	public String getAccessLevel() {
		StoreEntry entry = resolveEntry();
		return entry.getAccessLevel().name().toLowerCase();
	}

	@LuaMadeCallable
	public Integer getOwnerFactionId() {
		StoreEntry entry = resolveEntry();
		return entry.getOwnerFactionId();
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private StoreEntry resolveEntry() {
		StoreEntry entry = NetworkedDataStoreRegistry.resolve(storeName);
		if(entry == null) {
			throw new LuaError("Networked data store '" + storeName + "' is not registered");
		}
		return entry;
	}

	private String resolveUuid() {
		return resolveEntry().getUuid();
	}

	private void checkAccess() {
		StoreEntry entry = resolveEntry();
		switch(entry.getAccessLevel()) {
			case PUBLIC:
				return;
			case FACTION:
				int computerFaction = segmentPiece.getSegmentController().getFactionId();
				if(computerFaction != 0 && computerFaction == entry.getOwnerFactionId()) return;
				throw new LuaError("Access denied: computer is not in the same faction as networked data store '" + storeName + "'");
			case ENTITY:
			default:
				throw new LuaError("Access denied: networked data store '" + storeName + "' has entity-level access and can only be accessed locally");
		}
	}

	private static LuaTable toTable(List<String> list) {
		LuaTable table = new LuaTable();
		for(int i = 0; i < list.size(); i++) {
			table.rawset(i + 1, LuaValue.valueOf(list.get(i)));
		}
		return table;
	}
}
