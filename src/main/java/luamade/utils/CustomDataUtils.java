package luamade.utils;

import org.json.JSONObject;
import org.luaj.vm2.LuaError;

/**
 * Helpers for exposing vanilla block/item custom data (JSON) to Lua.
 * Keys prefixed with {@code luamade} are owned by the mod (disk ids, remote links) and survive script writes.
 */
public final class CustomDataUtils {

	private static final String RESERVED_PREFIX = "luamade";

	private CustomDataUtils() {
	}

	public static String toJson(JSONObject data) {
		return data == null ? "{}" : data.toString();
	}

	/** Parses script-supplied JSON, keeping reserved keys from {@code existing}. May be empty. */
	public static JSONObject fromScript(String json, JSONObject existing) {
		JSONObject incoming;
		try {
			incoming = json == null || json.trim().isEmpty() ? new JSONObject() : new JSONObject(json);
		} catch(Exception exception) {
			throw new LuaError("Invalid JSON object: " + exception.getMessage());
		}
		for(String key : names(incoming)) {
			if(key.startsWith(RESERVED_PREFIX)) {
				incoming.remove(key);
			}
		}
		if(existing != null) {
			for(String key : names(existing)) {
				if(key.startsWith(RESERVED_PREFIX)) {
					incoming.put(key, existing.get(key));
				}
			}
		}
		return incoming;
	}

	private static String[] names(JSONObject data) {
		String[] names = JSONObject.getNames(data);
		return names == null ? new String[0] : names;
	}

	/** Copy so callers never mutate vanilla's pooled/shared JSON instances. */
	public static JSONObject copy(JSONObject data) {
		return data == null ? new JSONObject() : new JSONObject(data.toString());
	}
}
