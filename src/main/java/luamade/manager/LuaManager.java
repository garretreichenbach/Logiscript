package luamade.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;
import luamade.api.Console;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	public static void run(String script, SegmentPiece segmentPiece) {
		try {
			Globals globals = JsePlatform.debugGlobals();
			LuaValue console = CoerceJavaToLua.coerce(new Console(segmentPiece));
			globals.set("console", console);
			LuaValue chunk = globals.load(script.trim());
			chunk.call();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}