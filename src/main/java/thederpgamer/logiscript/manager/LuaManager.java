package thederpgamer.logiscript.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.Console;
import thederpgamer.logiscript.api.LuaInterface;
import thederpgamer.logiscript.api.element.block.Block;
import thederpgamer.logiscript.api.element.block.BlockInfo;
import thederpgamer.logiscript.api.entity.Entity;

import java.lang.reflect.Constructor;

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
		Globals globals = JsePlatform.standardGlobals();
		loadLibs(globals, segmentPiece);
		return globals;
	}

	private static void loadLibs(Globals globals, SegmentPiece segmentPiece) {
		Console console = new Console(segmentPiece);
		console.initialize(globals);
		for(Class lib : libs) {
			try {
				for(Constructor constructor : lib.getConstructors()) {
					if(constructor.getParameters().length == 0) {
						LuaInterface luaInterface = (LuaInterface) constructor.newInstance();
						luaInterface.initialize(globals);
						break;
					}
				}
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	public static void run(String script, Object[] output, SegmentPiece segmentPiece) {
		Globals globals = newInstance(segmentPiece);
		LuaValue chunk = globals.load(script, segmentPiece.toString());
		output[0] = chunk.call();
	}
}