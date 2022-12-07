package luamade.luawrap;

import org.luaj.vm2.*;
import org.luaj.vm2.lib.TwoArgFunction;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public abstract class LuaMadeUserdata extends LuaUserdata {
    private final static LuaTable metaTable = new LuaTable();
    private final static Map<Class<?>, Map<String, WrapMethod>> methodWraps = new HashMap<>();

    private final static LuaFunction f = new TwoArgFunction() {
        @Override public LuaValue call(LuaValue ud, LuaValue key) {
            if (! (ud instanceof LuaMadeUserdata) || !key.isstring()) throw new LuaError("LuaMade userdatum must be indexed by string.");

            String methodName = key.tojstring();

            if (!methodWraps.containsKey(ud.getClass())) methodWraps.put(ud.getClass(), new HashMap<String, WrapMethod>());

            Map<String, WrapMethod> methods = methodWraps.get(ud.getClass());

            if (!methods.containsKey(methodName)) methods.put(methodName, new WrapMethod(methodName, ((LuaMadeUserdata) ud).getClass()));

            return methods.get(key.tojstring());
        }
    };
    public LuaMadeUserdata() {
        super(new Object(), getMeta());
    }

    private static LuaTable getMeta() {
        metaTable.set(INDEX, f);
        return metaTable;
    }

    /**
     * Registers a class with the LuaMade API.
     * <p>Internal use only</p>
     * @param cls The class to register.
     * @param methods The methods to register.
     */
    public static void registerClass(Class<? extends LuaMadeUserdata> cls, Method[] methods) {
        if(!methodWraps.containsKey(cls)) methodWraps.put(cls, new HashMap<String, WrapMethod>());
        for(Method method : methods) {
            if(!method.isAnnotationPresent(LuaMadeCallable.class)) continue;
            String name = method.getName();
            if(!methodWraps.get(cls).containsKey(name)) methodWraps.get(cls).put(name, new WrapMethod(name, cls));
        }
    }

    /**
     * Registers a method for a class.
     * <p>Internal use only</p>
     * @param cls The class to register the method for.
     * @param method The method to register.
     */
    public static void registerMethod(Class<? extends LuaMadeUserdata> cls, Method method) {
        if(!methodWraps.containsKey(cls)) methodWraps.put(cls, new HashMap<String, WrapMethod>());
        String name = method.getName();
        if(!methodWraps.get(cls).containsKey(name)) methodWraps.get(cls).put(name, new WrapMethod(name, cls));
    }
}