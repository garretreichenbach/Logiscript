package luamade.lua.datastore;

import luamade.LuaMade;
import luamade.utils.DataUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-block key→value store backed by JSON files on disk.
 *
 * <p>Each DataStore block gets a persistent UUID (assigned when first placed,
 * stored in {@link luamade.system.module.DataStoreModuleContainer}). That UUID
 * identifies its data file at {@code <worldData>/datastores/<uuid>.json}.
 *
 * <p>Limits (enforced on write):
 * <ul>
 *   <li>Keys  — max 256 characters, must not be empty.</li>
 *   <li>Values — max 65 536 characters (64 KiB).</li>
 *   <li>Total keys per store — max 10 000.</li>
 * </ul>
 */
public final class SharedDataStore {

	public static final int MAX_KEY_LENGTH = 256;
	public static final int MAX_VALUE_LENGTH = 65536;
	public static final int MAX_KEYS = 10000;

	/**
	 * store UUID → live key/value map.
	 */
	private static final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> stores = new ConcurrentHashMap<>();

	/**
	 * Guards per-store JSON writes so concurrent mutations don't corrupt the file.
	 */
	private static final ConcurrentHashMap<String, Object> storeLocks = new ConcurrentHashMap<>();

	private SharedDataStore() {
	}

	// -------------------------------------------------------------------------
	// Public API used by DataStoreBlock
	// -------------------------------------------------------------------------

	public static String get(String storeUuid, String key) {
		return store(storeUuid).get(key);
	}

	/**
	 * Sets {@code key} to {@code value} and persists the change.
	 *
	 * @throws IllegalArgumentException when limits are exceeded.
	 */
	public static void set(String storeUuid, String key, String value) {
		validateKey(key);
		if(value == null) {
			delete(storeUuid, key);
			return;
		}
		if(value.length() > MAX_VALUE_LENGTH) {
			throw new IllegalArgumentException("Value exceeds maximum length of " + MAX_VALUE_LENGTH + " characters");
		}

		ConcurrentHashMap<String, String> store = store(storeUuid);
		if(!store.containsKey(key) && store.size() >= MAX_KEYS) {
			throw new IllegalArgumentException("Data store is full (max " + MAX_KEYS + " keys)");
		}
		store.put(key, value);
		persist(storeUuid, store);
	}

	/**
	 * Removes {@code key}; no-op when absent. Persists only when a key was actually removed.
	 */
	public static boolean delete(String storeUuid, String key) {
		ConcurrentHashMap<String, String> store = store(storeUuid);
		boolean removed = store.remove(key) != null;
		if(removed) {
			persist(storeUuid, store);
		}
		return removed;
	}

	/**
	 * Returns all keys with the given prefix (empty prefix = all keys).
	 */
	public static List<String> keys(String storeUuid, String prefix) {
		ConcurrentHashMap<String, String> store = store(storeUuid);
		List<String> result = new ArrayList<>();
		for(String key : store.keySet()) {
			if(prefix == null || prefix.isEmpty() || key.startsWith(prefix)) {
				result.add(key);
			}
		}
		return result;
	}

	public static int size(String storeUuid) {
		return store(storeUuid).size();
	}

	public static boolean containsKey(String storeUuid, String key) {
		return store(storeUuid).containsKey(key);
	}

	/**
	 * Writes all loaded stores to disk. Called on server shutdown.
	 */
	public static void saveAll() {
		for(Map.Entry<String, ConcurrentHashMap<String, String>> entry : stores.entrySet()) {
			persist(entry.getKey(), entry.getValue());
		}
	}

	// -------------------------------------------------------------------------
	// Internal helpers
	// -------------------------------------------------------------------------

	private static ConcurrentHashMap<String, String> store(String storeUuid) {
		ConcurrentHashMap<String, String> existing = stores.get(storeUuid);
		if(existing != null) {
			return existing;
		}
		return stores.computeIfAbsent(storeUuid, SharedDataStore::load);
	}

	private static ConcurrentHashMap<String, String> load(String storeUuid) {
		ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
		File file = storeFile(storeUuid);
		if(file == null || !file.exists()) {
			return map;
		}
		try {
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			JSONObject obj = new JSONObject(json);
			Iterator<String> it = obj.keys();
			while(it.hasNext()) {
				String key = it.next();
				Object val = obj.opt(key);
				if(val instanceof String) {
					map.put(key, (String) val);
				}
			}
		} catch(Exception ex) {
			LuaMade.getInstance().logWarning("Failed to load data store " + storeUuid + ": " + ex.getMessage());
		}
		return map;
	}

	private static void persist(String storeUuid, ConcurrentHashMap<String, String> store) {
		File file = storeFile(storeUuid);
		if(file == null) {
			return;
		}
		synchronized(lockFor(storeUuid)) {
			try {
				File parent = file.getParentFile();
				if(parent != null && !parent.exists()) {
					parent.mkdirs();
				}
				JSONObject obj = new JSONObject();
				for(Map.Entry<String, String> entry : store.entrySet()) {
					obj.put(entry.getKey(), entry.getValue());
				}
				Files.write(file.toPath(), obj.toString(2).getBytes(StandardCharsets.UTF_8));
			} catch(IOException ex) {
				LuaMade.getInstance().logWarning("Failed to persist data store " + storeUuid + ": " + ex.getMessage());
			}
		}
	}

	private static File storeFile(String storeUuid) {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath == null || worldDataPath.trim().isEmpty()) {
			return null;
		}
		File dir = new File(worldDataPath, "datastores");
		return new File(dir, storeUuid + ".json");
	}

	private static Object lockFor(String storeUuid) {
		return storeLocks.computeIfAbsent(storeUuid, id -> new Object());
	}

	private static void validateKey(String key) {
		if(key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Key must not be empty");
		}
		if(key.length() > MAX_KEY_LENGTH) {
			throw new IllegalArgumentException("Key exceeds maximum length of " + MAX_KEY_LENGTH + " characters");
		}
	}
}
