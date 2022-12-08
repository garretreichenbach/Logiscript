package luamade.manager;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaFunction;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Manages the API functionality and allows for other mods to add their own functions.
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class APIManager {
	/**
	 * Adds a new API function to the specified class.
	 * <p>Useful for extending functionality of existing Lua classes, particularly the ones included in the mod.</p>
	 * @param cls The class to add the function to.
	 * @param name The name of the method.
	 * @param method The method to add.
	 */
	public static void addMethod(Class<? extends LuaMadeUserdata> cls, String name, LuaFunction method) {
		LuaMadeUserdata.graftMethod(cls, name, method);
	}
}
