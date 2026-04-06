package luamade.lua.datastore;

import luamade.LuaMade;
import luamade.utils.DataUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global registry that maps user-chosen store names to their backing UUIDs and
 * access-control metadata. Persisted to {@code <worldData>/networked_datastores.json}.
 *
 * <p>This registry is loaded once at server start and kept in memory. It allows
 * any computer to resolve a Networked Data Store by name without the owning
 * entity being loaded.
 *
 * <p>Names are globally unique and case-insensitive.
 */
public final class NetworkedDataStoreRegistry {

	/**
	 * Access levels that can be configured on a networked data store.
	 */
	public enum AccessLevel {
		/** Only computers on the same entity can access. */
		ENTITY,
		/** Only computers belonging to the same faction can access. */
		FACTION,
		/** Any computer can access. */
		PUBLIC;

		public static AccessLevel fromString(String s) {
			if(s == null) return ENTITY;
			switch(s.toLowerCase()) {
				case "public": return PUBLIC;
				case "faction": return FACTION;
				default: return ENTITY;
			}
		}
	}

	/**
	 * Metadata for a single registered networked data store.
	 */
	public static final class StoreEntry {
		private final String uuid;
		private final String name;
		private volatile int ownerFactionId;
		private volatile AccessLevel accessLevel;

		public StoreEntry(String uuid, String name, int ownerFactionId, AccessLevel accessLevel) {
			this.uuid = uuid;
			this.name = name;
			this.ownerFactionId = ownerFactionId;
			this.accessLevel = accessLevel;
		}

		public String getUuid() {
			return uuid;
		}

		public String getName() {
			return name;
		}

		public int getOwnerFactionId() {
			return ownerFactionId;
		}

		public void setOwnerFactionId(int ownerFactionId) {
			this.ownerFactionId = ownerFactionId;
		}

		public AccessLevel getAccessLevel() {
			return accessLevel;
		}

		public void setAccessLevel(AccessLevel accessLevel) {
			this.accessLevel = accessLevel;
		}
	}

	/** Normalized name → store entry. */
	private static final ConcurrentHashMap<String, StoreEntry> entries = new ConcurrentHashMap<>();

	private static final Object fileLock = new Object();

	private NetworkedDataStoreRegistry() {
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	/**
	 * Registers a new networked data store. Returns {@code false} if the name
	 * is already taken.
	 */
	public static boolean register(String name, String uuid, int ownerFactionId, AccessLevel accessLevel) {
		if(name == null || name.isEmpty() || uuid == null || uuid.isEmpty()) return false;
		String key = normalize(name);
		StoreEntry entry = new StoreEntry(uuid, name, ownerFactionId, accessLevel);
		if(entries.putIfAbsent(key, entry) != null) {
			return false;
		}
		persist();
		return true;
	}

	/**
	 * Removes a networked data store registration and deletes its backing data.
	 */
	public static boolean unregister(String name) {
		if(name == null) return false;
		StoreEntry removed = entries.remove(normalize(name));
		if(removed != null) {
			SharedDataStore.deleteStore(removed.getUuid());
			persist();
			return true;
		}
		return false;
	}

	/**
	 * Resolves a store name to its entry. Returns {@code null} if not found.
	 */
	public static StoreEntry resolve(String name) {
		if(name == null) return null;
		return entries.get(normalize(name));
	}

	/**
	 * Checks if a name is already registered.
	 */
	public static boolean isNameTaken(String name) {
		if(name == null) return false;
		return entries.containsKey(normalize(name));
	}

	/**
	 * Loads the registry from disk. Called once at server start.
	 */
	public static void load() {
		entries.clear();
		File file = registryFile();
		if(file == null || !file.exists()) return;
		try {
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			JSONObject root = new JSONObject(json);
			Iterator<String> it = root.keys();
			while(it.hasNext()) {
				String key = it.next();
				JSONObject obj = root.optJSONObject(key);
				if(obj == null) continue;
				String uuid = obj.optString("uuid", null);
				String name = obj.optString("name", key);
				int factionId = obj.optInt("ownerFactionId", 0);
				AccessLevel access = AccessLevel.fromString(obj.optString("accessLevel", "entity"));
				if(uuid != null && !uuid.isEmpty()) {
					entries.put(normalize(name), new StoreEntry(uuid, name, factionId, access));
				}
			}
			LuaMade.getInstance().logDebug("Loaded " + entries.size() + " networked data store registrations");
		} catch(Exception ex) {
			LuaMade.getInstance().logWarning("Failed to load networked data store registry: " + ex.getMessage());
		}
	}

	/**
	 * Saves the registry to disk. Called on server shutdown.
	 */
	public static void saveAll() {
		persist();
	}

	// -------------------------------------------------------------------------
	// Internal
	// -------------------------------------------------------------------------

	private static void persist() {
		File file = registryFile();
		if(file == null) return;
		synchronized(fileLock) {
			try {
				File parent = file.getParentFile();
				if(parent != null && !parent.exists()) {
					parent.mkdirs();
				}
				JSONObject root = new JSONObject();
				for(StoreEntry entry : entries.values()) {
					JSONObject obj = new JSONObject();
					obj.put("uuid", entry.getUuid());
					obj.put("name", entry.getName());
					obj.put("ownerFactionId", entry.getOwnerFactionId());
					obj.put("accessLevel", entry.getAccessLevel().name().toLowerCase());
					root.put(normalize(entry.getName()), obj);
				}
				Files.write(file.toPath(), root.toString(2).getBytes(StandardCharsets.UTF_8));
			} catch(IOException ex) {
				LuaMade.getInstance().logWarning("Failed to persist networked data store registry: " + ex.getMessage());
			}
		}
	}

	private static File registryFile() {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath == null || worldDataPath.trim().isEmpty()) return null;
		return new File(worldDataPath, "networked_datastores.json");
	}

	private static String normalize(String name) {
		return name.toLowerCase().trim();
	}
}
