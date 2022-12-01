package thederpgamer.logiscript.manager;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.server.Launcher;

import java.io.InputStream;
import java.io.Reader;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaLauncher implements Launcher {

	public static void runLua(String script, Object[] output) throws Exception {
		//Launcher launcher = LuajClassLoader.NewLauncher(LuaLauncher.class);
		//launcher.launch(script, output);
		Globals globals1 = JsePlatform.standardGlobals();
		LuaValue chunk = globals1.load(script, "main");
		output[0] = chunk.call();
	}

	Globals globals;

	public LuaLauncher() {
		globals = JsePlatform.standardGlobals();
	}

	public Object[] launch(String script, Object[] arg) {
		LuaValue chunk = globals.load(script, "main");
		return new Object[] { chunk.call(LuaValue.valueOf(arg[0].toString())) };
	}

	public Object[] launch(InputStream script, Object[] arg) {
		LuaValue chunk = globals.load(script, "main", "bt", globals);
		return new Object[] { chunk.call(LuaValue.valueOf(arg[0].toString())) };
	}

	public Object[] launch(Reader script, Object[] arg) {
		LuaValue chunk = globals.load(script, "main");
		return new Object[] { chunk.call(LuaValue.valueOf(arg[0].toString())) };
	}
}
