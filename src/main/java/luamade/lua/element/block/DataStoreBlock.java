package luamade.lua.element.block;

import luamade.element.ElementRegistry;
import luamade.lua.datastore.SharedDataStore;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.manager.PasswordAuthManager;
import luamade.system.module.ComputerModule;
import luamade.system.module.DataStoreModuleContainer;
import luamade.system.module.PasswordPermissionModuleContainer;
import luamade.utils.SegmentPieceUtils;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Lua-facing wrapper for a DataStore block.
 *
 * <h2>Access control</h2>
 * Access is determined by the StarMade permission blocks physically adjacent to
 * the DataStore:
 * <ul>
 *   <li>No adjacent permission block — only computers on the <em>same entity</em>
 *       as the DataStore may access it.</li>
 *   <li>Adjacent {@code FACTION_PERMISSION_MODULE} (936) — any computer belonging
 *       to the <em>same faction</em> as the DataStore's owning entity may access.</li>
 *   <li>Adjacent {@code PUBLIC_PERMISSION_MODULE} (346) — <em>any</em> computer
 *       may access regardless of faction.</li>
 *   <li>Adjacent {@code PASSWORD_PERMISSION_MODULE} — any computer whose faction
 *       has authenticated via {@link #auth} may access.</li>
 * </ul>
 *
 * <h2>Data scope</h2>
 * Each DataStore block has its own independent key-value store identified by a
 * persistent UUID. Data survives server restarts and is stored at
 * {@code <worldData>/datastores/<uuid>.json}.
 */
@LuaMadeClass("DataStore")
public class DataStoreBlock extends Block {

	private final ComputerModule module;

	public DataStoreBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
		this.module = module;
	}

	// -------------------------------------------------------------------------
	// Data access
	// -------------------------------------------------------------------------

	/**
	 * Returns the value stored at {@code key}, or {@code nil} when absent.
	 */
	@LuaMadeCallable
	public String getValue(String key) {
		checkAccess();
		return SharedDataStore.get(storeUuid(), key);
	}

	/**
	 * Sets {@code key} to {@code value}. Pass {@code nil} to delete the key.
	 */
	@LuaMadeCallable
	public void set(String key, String value) {
		checkAccess();
		SharedDataStore.set(storeUuid(), key, value);
	}

	/**
	 * Deletes {@code key}. Returns {@code true} when a key was actually removed.
	 */
	@LuaMadeCallable
	public Boolean delete(String key) {
		checkAccess();
		return SharedDataStore.delete(storeUuid(), key);
	}

	/**
	 * Returns {@code true} when {@code key} exists in the store.
	 */
	@LuaMadeCallable
	public Boolean has(String key) {
		checkAccess();
		return SharedDataStore.containsKey(storeUuid(), key);
	}

	/**
	 * Returns a table (array) of all keys in this store.
	 */
	@LuaMadeCallable
	public LuaTable keys() {
		checkAccess();
		return toTable(SharedDataStore.keys(storeUuid(), null));
	}

	/**
	 * Returns a table (array) of all keys that start with {@code prefix}.
	 */
	@LuaMadeCallable
	public LuaTable keys(String prefix) {
		checkAccess();
		return toTable(SharedDataStore.keys(storeUuid(), prefix));
	}

	/**
	 * Returns the total number of keys in this store.
	 */
	@LuaMadeCallable
	public Integer size() {
		checkAccess();
		return SharedDataStore.size(storeUuid());
	}

	/**
	 * Authenticates the calling computer's faction with any adjacent
	 * {@code PasswordPermissionModule} blocks whose password matches.
	 *
	 * <p>On success the faction is registered in {@link PasswordAuthManager} for the
	 * default TTL, granting access to this DataStore and also to any other StarMade
	 * blocks protected by the same PasswordPermissionModule (docking, rail, console,
	 * etc.).
	 *
	 * @param password the password to test
	 * @return {@code true} if the password matched at least one adjacent module
	 */
	@LuaMadeCallable
	public Boolean auth(String password) {
		if(password == null) return false;
		SegmentPiece datastorePiece = getSegmentPiece();
		short pwdModuleId = ElementRegistry.PASSWORD_PERMISSION_MODULE.getId();
		ArrayList<SegmentPiece> modules = SegmentPieceUtils.getMatchingAdjacent(datastorePiece, pwdModuleId);
		if(modules.isEmpty()) return false;

		int computerFaction = module.getSegmentPiece().getSegmentController().getFactionId();
		boolean anyMatch = false;
		for(SegmentPiece pwdModule : modules) {
			if(!(pwdModule.getSegmentController() instanceof ManagedUsableSegmentController<?>)) continue;
			ManagedUsableSegmentController<?> ctrl = (ManagedUsableSegmentController<?>) pwdModule.getSegmentController();
			PasswordPermissionModuleContainer container = PasswordPermissionModuleContainer.getContainer(ctrl.getManagerContainer());
			if(container == null) continue;
			String saltHash = container.getPasswordHash(pwdModule.getAbsoluteIndex());
			if(PasswordPermissionModuleBlock.verifyPassword(password, saltHash)) {
				PasswordAuthManager.authenticate(computerFaction, pwdModule.getSegmentController(), pwdModule.getAbsoluteIndex());
				anyMatch = true;
			}
		}
		return anyMatch;
	}

	/**
	 * Returns the persistent UUID that identifies this block's data store.
	 */
	@LuaMadeCallable
	public String getStoreId() {
		return storeUuid();
	}

	/**
	 * Returns the faction ID of the entity this DataStore is installed on.
	 */
	@LuaMadeCallable
	public Integer getOwnerFactionId() {
		return getSegmentPiece().getSegmentController().getFactionId();
	}

	/**
	 * Returns the current access level this computer has for this DataStore:
	 * {@code "public"}, {@code "faction"}, {@code "password"}, or {@code "entity"}.
	 */
	@LuaMadeCallable
	public String getAccessLevel() {
		SegmentPiece piece = getSegmentPiece();
		if(!SegmentPieceUtils.getMatchingAdjacent(piece, ElementKeyMap.FACTION_PUBLIC_EXCEPTION_ID).isEmpty()) {
			return "public";
		}
		if(!SegmentPieceUtils.getMatchingAdjacent(piece, ElementKeyMap.FACTION_FACTION_EXCEPTION_ID).isEmpty()) {
			return "faction";
		}
		if(!SegmentPieceUtils.getMatchingAdjacent(piece, ElementRegistry.PASSWORD_PERMISSION_MODULE.getId()).isEmpty()) {
			return "password";
		}
		return "entity";
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private String storeUuid() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("Data store is not available on this structure type");
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		DataStoreModuleContainer container = DataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			throw new LuaError("Data store module is not initialized");
		}
		return container.getOrAssignUuid(piece.getAbsoluteIndex());
	}

	/**
	 * Enforces the access model described in the class javadoc.
	 *
	 * <ol>
	 *   <li>Adjacent {@code PUBLIC_PERMISSION_MODULE} (346) → allow anyone.</li>
	 *   <li>Adjacent {@code FACTION_PERMISSION_MODULE} (936) → allow same faction.</li>
	 *   <li>Adjacent {@code PASSWORD_PERMISSION_MODULE} → allow if faction is authed.</li>
	 *   <li>Default → allow only computers on the same entity.</li>
	 * </ol>
	 */
	private void checkAccess() {
		SegmentPiece datastorePiece = getSegmentPiece();
		int computerFaction = module.getSegmentPiece().getSegmentController().getFactionId();

		// PUBLIC: any computer may access.
		if(!SegmentPieceUtils.getMatchingAdjacent(datastorePiece, ElementKeyMap.FACTION_PUBLIC_EXCEPTION_ID).isEmpty()) {
			return;
		}

		// FACTION: same-faction computers may access.
		if(!SegmentPieceUtils.getMatchingAdjacent(datastorePiece, ElementKeyMap.FACTION_FACTION_EXCEPTION_ID).isEmpty()) {
			int datastoreFaction = datastorePiece.getSegmentController().getFactionId();
			if(datastoreFaction != 0 && datastoreFaction == computerFaction) {
				return;
			}
			throw new LuaError("Access denied: computer is not in the same faction as this data store");
		}

		// PASSWORD: faction-authed computers may access.
		ArrayList<SegmentPiece> pwdModules = SegmentPieceUtils.getMatchingAdjacent(datastorePiece, ElementRegistry.PASSWORD_PERMISSION_MODULE.getId());
		if(!pwdModules.isEmpty()) {
			for(SegmentPiece pwdModule : pwdModules) {
				if(PasswordAuthManager.isAuthed(computerFaction, pwdModule.getSegmentController(), pwdModule.getAbsoluteIndex())) {
					return;
				}
			}
			throw new LuaError("Access denied: call auth(password) to authenticate with the adjacent Password Permission Module");
		}

		// DEFAULT: same-entity access only.
		if(datastorePiece.getSegmentController() == module.getSegmentPiece().getSegmentController()) {
			return;
		}
		throw new LuaError("Access denied: place a Permission Module adjacent to this data store to allow cross-entity access");
	}

	private static LuaTable toTable(List<String> list) {
		LuaTable table = new LuaTable();
		for(int i = 0; i < list.size(); i++) {
			table.rawset(i + 1, LuaValue.valueOf(list.get(i)));
		}
		return table;
	}
}
