package thederpgamer.logiscript.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.Console;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	public static Globals newInstance(SegmentPiece segmentPiece) {
		Globals globals = JsePlatform.standardGlobals();
		loadLibs(globals, segmentPiece);
		return globals;
	}

	private static void loadLibs(Globals globals, SegmentPiece segmentPiece) {
		Console console = new Console(segmentPiece);
		console.initialize(globals);
	}

	public static void run(String script, Object[] output, SegmentPiece segmentPiece) {
		Globals globals = newInstance(segmentPiece);
		LuaValue chunk = globals.load(script, segmentPiece.toString());
		output[0] = chunk.call();
	}
}