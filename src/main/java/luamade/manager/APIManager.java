package luamade.manager;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Manages the API functionality and allows for other mods to add their own functions.
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class APIManager {

	private static final String[] classes = {
			"Console", "Channel", "LuaVec3",
			"Faction", "FactionMember", "FactionRank",
			"Entity", "RemoteEntity", "EntityAI",
			"Block", "BlockInfo", "Inventory", "ItemStack",
			"Reactor", "Chamber", "ThrustSystem", "Shipyard"
	};

	/**
	 * Registers default class functions.
	 */
	public static void initialize() {
		try {
			for(String name : classes) {
				Class<?> cls = Class.forName(name);
				//Check if class is a LuaMadeUserdata
				if(LuaMadeUserdata.class.isAssignableFrom(cls)) registerClass((Class<? extends LuaMadeUserdata>) cls);
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	/**
	 * Registers an API class.
	 * @param cls The class to register.
	 */
	public static void registerClass(Class<? extends LuaMadeUserdata> cls) {
		Method[] methods = cls.getMethods();
		ArrayList<Method> methodList = new ArrayList<>();
		for(Method m : methods) {
			if(m.isAnnotationPresent(LuaMadeCallable.class)) methodList.add(m);
		}
		LuaMadeUserdata.registerClass(cls, methodList.toArray(new Method[0]));
	}

	/**
	 * Adds a new API function to the specified class.
	 * <p>Useful for extending functionality of existing Lua classes, particularly the ones included in the mod.</p>
	 * @param cls The class to add the function to.
	 * @param method The method to add.
	 */
	public static void addMethod(Class<? extends LuaMadeUserdata> cls, Method method) {
		LuaMadeUserdata.registerMethod(cls, method);
	}
}
