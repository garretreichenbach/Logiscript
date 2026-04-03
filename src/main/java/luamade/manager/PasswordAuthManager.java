package luamade.manager;

import org.schema.game.common.controller.SegmentController;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry of authenticated (faction, PasswordPermissionModule) pairs.
 *
 * <p>Auth is granted when a Lua script successfully calls {@code auth(password)} on a
 * {@link luamade.lua.element.block.PasswordPermissionModuleBlock} (or implicitly via
 * {@link luamade.lua.element.block.DataStoreBlock#auth}). It expires after a fixed TTL.
 *
 * <p>Keys encode the faction ID, the owning entity's runtime ID, and the module
 * block's absolute index. Entity runtime IDs are stable for the lifetime of the
 * game session, making this safe for in-session ephemeral state. Auth state is
 * intentionally not persisted across server restarts.
 */
public final class PasswordAuthManager {

	/** Default auth lifetime in milliseconds (5 minutes). */
	public static final long DEFAULT_TTL_MS = 5L * 60 * 1000;

	/** key: "{factionId}@{entityId}:{absIndex}" → expiry timestamp (ms). */
	private static final ConcurrentHashMap<String, Long> authState = new ConcurrentHashMap<>();

	private PasswordAuthManager() {
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Grants {@code factionId} access through the PasswordPermissionModule at
	 * {@code moduleAbsIndex} on {@code moduleEntity} for {@link #DEFAULT_TTL_MS} ms.
	 */
	public static void authenticate(int factionId, SegmentController moduleEntity, long moduleAbsIndex) {
		authenticate(factionId, moduleEntity, moduleAbsIndex, DEFAULT_TTL_MS);
	}

	/**
	 * Grants access for a custom TTL (milliseconds).
	 */
	public static void authenticate(int factionId, SegmentController moduleEntity, long moduleAbsIndex, long ttlMs) {
		if(factionId == 0) return; // never grant access to the "no faction" group
		authState.put(key(factionId, moduleEntity, moduleAbsIndex), System.currentTimeMillis() + ttlMs);
	}

	/**
	 * Returns {@code true} if {@code factionId} is currently authenticated through
	 * the PasswordPermissionModule at {@code moduleAbsIndex} on {@code moduleEntity}.
	 * Expired entries are removed lazily.
	 */
	public static boolean isAuthed(int factionId, SegmentController moduleEntity, long moduleAbsIndex) {
		String k = key(factionId, moduleEntity, moduleAbsIndex);
		Long expiry = authState.get(k);
		if(expiry == null) return false;
		if(System.currentTimeMillis() > expiry) {
			authState.remove(k);
			return false;
		}
		return true;
	}

	/**
	 * Revokes all auth entries for the given faction / module combination.
	 * Useful for explicit logout.
	 */
	public static void deauthenticate(int factionId, SegmentController moduleEntity, long moduleAbsIndex) {
		authState.remove(key(factionId, moduleEntity, moduleAbsIndex));
	}

	/** Removes all expired entries. Can be called periodically if needed. */
	public static void evictExpired() {
		long now = System.currentTimeMillis();
		authState.entrySet().removeIf(e -> now > e.getValue());
	}

	// -------------------------------------------------------------------------
	// Internal
	// -------------------------------------------------------------------------

	private static String key(int factionId, SegmentController entity, long absIndex) {
		return factionId + "@" + entity.getId() + ":" + absIndex;
	}
}
