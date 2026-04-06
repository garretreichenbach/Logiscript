package luamade.lua.element.block;

import luamade.element.ElementRegistry;
import luamade.lua.datastore.NetworkedDataStoreRegistry;
import luamade.lua.datastore.NetworkedDataStoreRegistry.AccessLevel;
import luamade.lua.datastore.NetworkedDataStoreRegistry.StoreEntry;
import luamade.lua.datastore.SharedDataStore;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.system.module.ComputerModule;
import luamade.system.module.NetworkedDataStoreModuleContainer;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.util.List;

/**
 * Lua-facing wrapper for a Networked Data Store block (peripheral access).
 *
 * <p>This wrapper is used when a computer has direct access to the physical
 * block (same entity or adjacent). It provides the full API including
 * registration, naming, and access-level configuration.
 *
 * <h2>Access control</h2>
 * Access is controlled by a configurable access level stored in the global
 * registry rather than by adjacent permission blocks:
 * <ul>
 *   <li>{@code "entity"} — only computers on the same entity (default).</li>
 *   <li>{@code "faction"} — any computer belonging to the same faction.</li>
 *   <li>{@code "public"} — any computer.</li>
 * </ul>
 */
@LuaMadeClass("NetworkedDataStore")
public class NetworkedDataStoreBlock extends Block {

	private final ComputerModule module;

	public NetworkedDataStoreBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
		this.module = module;
	}

	// -------------------------------------------------------------------------
	// Registration & configuration (requires physical access)
	// -------------------------------------------------------------------------

	/**
	 * Registers this block's data store with the given globally-unique name.
	 * Returns {@code false} if the name is already taken or if the store is
	 * already registered.
	 */
	@LuaMadeCallable
	public Boolean register(String name) {
		checkSameEntity();
		if(name == null || name.isEmpty()) {
			throw new LuaError("Name must not be empty");
		}
		if(name.length() > 64) {
			throw new LuaError("Name must be 64 characters or fewer");
		}
		if(!name.matches("[a-zA-Z0-9_\\-]+")) {
			throw new LuaError("Name may only contain letters, digits, hyphens, and underscores");
		}

		NetworkedDataStoreModuleContainer container = getContainer();
		long absIndex = getSegmentPiece().getAbsoluteIndex();
		String uuid = container.getOrAssignUuid(absIndex);
		int factionId = getSegmentPiece().getSegmentController().getFactionId();

		// Check if already registered under a different name
		String currentName = container.getName(absIndex);
		if(currentName != null && !currentName.isEmpty()) {
			throw new LuaError("This store is already registered as '" + currentName + "'. Unregister it first.");
		}

		boolean success = NetworkedDataStoreRegistry.register(name, uuid, factionId, AccessLevel.ENTITY);
		if(success) {
			container.setName(absIndex, name);
		}
		return success;
	}

	/**
	 * Unregisters this block's data store from the global registry and
	 * deletes all its data.
	 */
	@LuaMadeCallable
	public Boolean unregister() {
		checkSameEntity();
		NetworkedDataStoreModuleContainer container = getContainer();
		long absIndex = getSegmentPiece().getAbsoluteIndex();
		String name = container.getName(absIndex);
		if(name == null || name.isEmpty()) return false;
		boolean success = NetworkedDataStoreRegistry.unregister(name);
		if(success) {
			container.setName(absIndex, null);
		}
		return success;
	}

	/**
	 * Returns the registered name, or {@code nil} if not yet registered.
	 */
	@LuaMadeCallable
	public String getStoreName() {
		NetworkedDataStoreModuleContainer container = getContainer();
		return container.getName(getSegmentPiece().getAbsoluteIndex());
	}

	/**
	 * Returns the persistent UUID backing this block's data.
	 */
	@LuaMadeCallable
	public String getStoreId() {
		return storeUuid();
	}

	/**
	 * Sets the access level for this store. Must be {@code "entity"},
	 * {@code "faction"}, or {@code "public"}.
	 */
	@LuaMadeCallable
	public void setAccessLevel(String level) {
		checkSameEntity();
		String name = getRegisteredName();
		StoreEntry entry = NetworkedDataStoreRegistry.resolve(name);
		if(entry == null) throw new LuaError("Store is not registered");
		AccessLevel accessLevel = AccessLevel.fromString(level);
		entry.setAccessLevel(accessLevel);
		NetworkedDataStoreRegistry.saveAll();
	}

	/**
	 * Returns the current access level: {@code "entity"}, {@code "faction"},
	 * or {@code "public"}.
	 */
	@LuaMadeCallable
	public String getAccessLevel() {
		String name = getContainerName();
		if(name == null || name.isEmpty()) return "entity";
		StoreEntry entry = NetworkedDataStoreRegistry.resolve(name);
		if(entry == null) return "entity";
		return entry.getAccessLevel().name().toLowerCase();
	}

	/**
	 * Returns the faction ID of the entity this block is installed on.
	 */
	@LuaMadeCallable
	public Integer getOwnerFactionId() {
		return getSegmentPiece().getSegmentController().getFactionId();
	}

	/**
	 * Returns whether this store is currently registered in the global registry.
	 */
	@LuaMadeCallable
	public Boolean isRegistered() {
		String name = getContainerName();
		return name != null && !name.isEmpty() && NetworkedDataStoreRegistry.isNameTaken(name);
	}

	// -------------------------------------------------------------------------
	// Data access (same API as DataStoreBlock)
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public String getValue(String key) {
		checkAccess();
		return SharedDataStore.get(storeUuid(), key);
	}

	@LuaMadeCallable
	public void set(String key, String value) {
		checkAccess();
		SharedDataStore.set(storeUuid(), key, value);
	}

	@LuaMadeCallable
	public Boolean delete(String key) {
		checkAccess();
		return SharedDataStore.delete(storeUuid(), key);
	}

	@LuaMadeCallable
	public Boolean has(String key) {
		checkAccess();
		return SharedDataStore.containsKey(storeUuid(), key);
	}

	@LuaMadeCallable
	public LuaTable keys() {
		checkAccess();
		return toTable(SharedDataStore.keys(storeUuid(), null));
	}

	@LuaMadeCallable
	public LuaTable keys(String prefix) {
		checkAccess();
		return toTable(SharedDataStore.keys(storeUuid(), prefix));
	}

	@LuaMadeCallable
	public Integer size() {
		checkAccess();
		return SharedDataStore.size(storeUuid());
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private String storeUuid() {
		NetworkedDataStoreModuleContainer container = getContainer();
		return container.getOrAssignUuid(getSegmentPiece().getAbsoluteIndex());
	}

	private NetworkedDataStoreModuleContainer getContainer() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("Networked data store is not available on this structure type");
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		NetworkedDataStoreModuleContainer container = NetworkedDataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			throw new LuaError("Networked data store module is not initialized");
		}
		return container;
	}

	private String getContainerName() {
		try {
			NetworkedDataStoreModuleContainer container = getContainer();
			return container.getName(getSegmentPiece().getAbsoluteIndex());
		} catch(LuaError e) {
			return null;
		}
	}

	private String getRegisteredName() {
		String name = getContainerName();
		if(name == null || name.isEmpty()) {
			throw new LuaError("This store is not registered. Call register(name) first.");
		}
		return name;
	}

	/**
	 * Checks that the calling computer is on the same entity as this block.
	 * Used for configuration operations that require physical ownership.
	 */
	private void checkSameEntity() {
		if(getSegmentPiece().getSegmentController() != module.getSegmentPiece().getSegmentController()) {
			throw new LuaError("This operation requires the computer to be on the same entity as the Networked Data Store");
		}
	}

	/**
	 * Enforces the access level configured in the registry.
	 */
	private void checkAccess() {
		String name = getContainerName();
		if(name == null || name.isEmpty()) {
			// Not registered yet — same-entity only
			checkSameEntity();
			return;
		}
		StoreEntry entry = NetworkedDataStoreRegistry.resolve(name);
		if(entry == null) {
			checkSameEntity();
			return;
		}
		switch(entry.getAccessLevel()) {
			case PUBLIC:
				return;
			case FACTION:
				int computerFaction = module.getSegmentPiece().getSegmentController().getFactionId();
				if(computerFaction != 0 && computerFaction == entry.getOwnerFactionId()) return;
				throw new LuaError("Access denied: computer is not in the same faction as this networked data store");
			case ENTITY:
			default:
				checkSameEntity();
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
