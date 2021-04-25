package dovtech.logiscript.utils;

import api.common.GameCommon;
import dovtech.logiscript.LogiScript;

/**
 * DataUtils
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public class DataUtils {

    public static String getResourcesPath() {
        return LogiScript.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
    }

    public static String getWorldDataPath() {
        String universeName = GameCommon.getUniqueContextId();
        if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
        else return null;
    }
}
