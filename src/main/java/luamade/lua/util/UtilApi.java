package luamade.lua.util;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import org.luaj.vm2.LuaError;

import java.util.regex.Pattern;

public class UtilApi extends LuaMadeUserdata {

	@LuaMadeCallable
	public long now() {
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public int sleep(int millis) {
		int clamped = Math.max(0, Math.min(millis, ConfigManager.getScriptTimeoutMs()));
		try {
			Thread.sleep(clamped);
			return clamped;
		} catch(InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new LuaError("Sleep interrupted");
		}
	}

	@LuaMadeCallable
	public double clamp(double value, double min, double max) {
		double low = Math.min(min, max);
		double high = Math.max(min, max);
		return Math.max(low, Math.min(high, value));
	}

	@LuaMadeCallable
	public long round(double value) {
		return Math.round(value);
	}

	@LuaMadeCallable
	public boolean startsWith(String value, String prefix) {
		if(value == null || prefix == null) {
			return false;
		}
		return value.startsWith(prefix);
	}

	@LuaMadeCallable
	public boolean endsWith(String value, String suffix) {
		if(value == null || suffix == null) {
			return false;
		}
		return value.endsWith(suffix);
	}

	@LuaMadeCallable
	public String[] split(String value, String delimiter) {
		if(value == null) {
			return new String[0];
		}
		if(delimiter == null || delimiter.isEmpty()) {
			return new String[] {value};
		}
		return value.split(Pattern.quote(delimiter), -1);
	}

	@LuaMadeCallable
	public String join(String[] values, String delimiter) {
		if(values == null || values.length == 0) {
			return "";
		}
		String joinDelimiter = delimiter == null ? "" : delimiter;
		StringBuilder builder = new StringBuilder();
		for(int i = 0; i < values.length; i++) {
			if(i > 0) {
				builder.append(joinDelimiter);
			}
			if(values[i] != null) {
				builder.append(values[i]);
			}
		}
		return builder.toString();
	}
}
