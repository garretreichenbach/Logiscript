package luamade.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for data store contents received from the server.
 * Populated by {@link PacketSCDataStoreContents} when the server responds
 * to a {@link PacketCSRequestDataStoreContents}.
 */
public final class ClientDataStoreCache {

	private static final ConcurrentHashMap<String, Map<String, String>> cache = new ConcurrentHashMap<>();

	private ClientDataStoreCache() {
	}

	public static void update(String storeUuid, Map<String, String> data) {
		cache.put(storeUuid, Collections.unmodifiableMap(new HashMap<>(data)));
	}

	public static Map<String, String> get(String storeUuid) {
		return cache.getOrDefault(storeUuid, Collections.emptyMap());
	}

	public static void remove(String storeUuid) {
		cache.remove(storeUuid);
	}
}
