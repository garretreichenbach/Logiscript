package luamade.lua.util;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import org.luaj.vm2.LuaError;

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
}
