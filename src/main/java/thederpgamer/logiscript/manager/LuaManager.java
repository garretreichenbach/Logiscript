package thederpgamer.logiscript.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.Console;
import thederpgamer.logiscript.api.element.block.Block;
import thederpgamer.logiscript.api.element.block.BlockInfo;
import thederpgamer.logiscript.api.entity.Entity;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	private static final Class[] libs = new Class[] {
			Entity.class,
			BlockInfo.class,
			Block.class
	};

	public static Globals newInstance(SegmentPiece segmentPiece) {
		Globals globals = JsePlatform.debugGlobals();
		loadLibs(globals, segmentPiece);
		return globals;
	}

	private static void loadLibs(Globals globals, SegmentPiece segmentPiece) {
		new Console(globals, segmentPiece);
	}

	public static void run(String script, SegmentPiece segmentPiece) {
		try {
			Globals globals = newInstance(segmentPiece);
			LuaValue chunk = globals.load(script.trim());
			chunk.call();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}