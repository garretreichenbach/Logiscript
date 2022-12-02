package thederpgamer.logiscript.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import thederpgamer.logiscript.api.Console;
import thederpgamer.logiscript.api.LuaInterface;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	private static final Class[] libClasses = {
			Console.class
	};

	public static Globals newInstance() {
		Globals globals = JsePlatform.standardGlobals();
		loadLibs(globals);
		return globals;
	}

	private static void loadLibs(Globals globals) {
		for(Class cls : libClasses) {
			try {
				LuaTable lib = (LuaTable) cls.newInstance();
				LuaInterface luaInterface = (LuaInterface) lib;
				globals.set(luaInterface.getName(), lib);
			} catch(InstantiationException | IllegalAccessException exception) {
				exception.printStackTrace();
			}
		}
	}

	public static void run(String script, Object[] output) {
		Globals globals = newInstance();
		LuaValue chunk = globals.load(script, "main");
		output[0] = chunk.call();
	}
}