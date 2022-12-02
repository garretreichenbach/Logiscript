package thederpgamer.logiscript.api;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public interface LuaInterface {

	void initialize(Globals globals);

	String getName();

	String[] getMethods();

	LuaFunction getMethod(String name);
}
