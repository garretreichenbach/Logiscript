package luamade.utils;

import api.common.GameClient;
import api.common.GameCommon;
import luamade.LuaMade;

public class DataUtils {

	public static String getResourcesPath() {
		return LuaMade.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}

	public static String getWorldDataPath() {
		String universeName = GameCommon.getUniqueContextId();
		if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
		else {
			try {
				LuaMade.getInstance().logWarning("Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.");
			} catch(Exception ignored) {
			}
			return null;
		}
	}
}
