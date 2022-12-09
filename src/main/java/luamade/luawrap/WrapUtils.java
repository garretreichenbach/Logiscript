package luamade.luawrap;

import org.luaj.vm2.*;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class WrapUtils {
    public static LuaValue wrapSingle(Object o) {
        if(o instanceof LuaValue)
            return (LuaValue) o;

        else if(o instanceof Boolean)
            return LuaValue.valueOf((Boolean) o);

        else if(o instanceof Integer)
            return LuaValue.valueOf((Integer) o);

        else if(o instanceof Long)
            return LuaValue.valueOf((Long) o);

        else if(o instanceof Double)
            return LuaValue.valueOf((Double) o);

        else if(o instanceof String)
            return LuaValue.valueOf((String) o);

        else if (o == null)
            return LuaValue.NIL;

        throw new LuaError(String.format("Object %s not wrapable.", o.getClass()));
    }

    public static LuaTable wrapArray(Object[] o) {
        LuaTable t = new LuaTable();
        for (int i = 0; i < o.length; ++i)
            t.rawset(i+1, wrapSingle(o[i]));
        return t;
    }

    public static Varargs wrap(Object o) {
        if (o instanceof Object[])
            return wrapArray((Object[]) o);
        else
            return wrapSingle(o);
    }

    public static Object unwrapSingle(LuaValue o, Class<?> clazz) {
        if (clazz.isArray()) throw new LuaError("No arrays.");

        if (Varargs.class.isAssignableFrom(clazz)) {
            if (clazz.isInstance(o))
                return o;

            throw new LuaError(String.format("No automated Lua->Lua coercions (%s -> %s).", o.getClass(), clazz));
        }

        if (clazz == String.class)
            return o.checkstring().tojstring();

        else if (clazz == boolean.class || clazz == Boolean.class)
            return o.toboolean();

        else if (clazz == double.class || clazz == Double.class)
            return o.checkdouble();

        else if (clazz == float.class || clazz == Float.class)
            return (float) o.checkdouble();

        else if (clazz == int.class || clazz == Integer.class)
            return o.checkint();

        else if (clazz == long.class || clazz == Long.class)
            return o.checkint();

        throw new LuaError(String.format("Cannot unwrap %s to %s.", o.getClass(), clazz));
    }

    public static Object[] unwrapArray(LuaValue o, Class<?> clazz) {
        if (!clazz.isArray()) throw new LuaError("Only conversions to arrays.");
        Class<?> et = clazz.getComponentType();
        if (et.isArray()) throw new LuaError("No nested arrays.");
        LuaTable t = o.checktable();

        ArrayList<Object> arr = new ArrayList<>();

        for (LuaValue e = t.rawget(1); !(e instanceof LuaNil); e = t.rawget(1+arr.size())) {
            arr.add(WrapUtils.unwrapSingle(e, et));
        }

        Object[] out = (Object[]) (Array.newInstance(et, arr.size()));

        for (int i = 0; i < out.length; ++i)
            out[i] = arr.get(i);

        if (clazz.isInstance(out))
            return out;

        throw new LuaError(String.format("Could not unwrap to array of %s.", et));
    }

    public static Object unwrap(LuaValue o, Class<?> clazz) {
        if (clazz.isArray())
            return unwrapArray(o, clazz);
        else
            return unwrapSingle(o, clazz);
    }

    public static Set<String> listMethods(Class<?> clazz) {
        Set<String> out = new HashSet<>();
        for (Method m : clazz.getMethods())
            out.add(m.getName());
        return out;
    }
}
