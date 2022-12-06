package luamade.luawrap;

import org.luaj.vm2.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class WrapUtils {
    public static Varargs safeWrap(Object o) {
        if(o instanceof Varargs)
            return (Varargs) o;

        else if(o instanceof Boolean)
            return LuaValue.valueOf((Boolean) o);

        else if(o instanceof Integer)
            return LuaValue.valueOf((Integer) o);

        else if(o instanceof Double)
            return LuaValue.valueOf((Double) o);

        else if(o instanceof String)
            return LuaValue.valueOf((String) o);

        else
            return LuaValue.NIL;
    }

    public static Object safeUnwrapSingle(LuaValue o, Class<?> clazz) {
        if (clazz.isArray()) throw new LuaError("No arrays.");

        if (Varargs.class.isAssignableFrom(clazz)) {
            if (clazz.isInstance(o))
                return o;
            else
                throw new LuaError(String.format("No automated Lua->Lua coercions (%s -> %s).", o.getClass(), clazz));
        }

        Object out;

        if (o instanceof LuaString) {
            out = ((LuaString) o).tojstring();
        }

        else if (o instanceof LuaBoolean) {
            out = ((LuaBoolean) o).booleanValue();
        }

        else if (o instanceof LuaDouble) {
            out = ((LuaDouble) o).todouble();
        }

        else if (o instanceof LuaInteger) {
            out = ((LuaInteger) o).toint();
        }
        else
            out = null;

        if (clazz.isInstance(out))
            return out;

        return null;
    }

    public static Object[] safeUnwrapArray(LuaValue o, Class<?> clazz) {
        LuaTable t = o.checktable();
        if (!clazz.isArray()) throw new LuaError("Only conversions to arrays.");
        Class<?> et = clazz.getComponentType();
        if (et.isArray()) throw new LuaError("No nested arrays.");

        ArrayList<Object> arr = new ArrayList<>();

        do {
            Object entry = safeUnwrapSingle( t.rawget(arr.size()+1), et);
            if (!et.isInstance(entry))
                break;

            arr.add(entry);
        } while (true);

        Object[] out = (Object[]) (Array.newInstance(et, arr.size()));

        for (int i = 0; i < arr.size(); ++i)
            out[i] = arr.get(i);

        return out;
    }

    public static Object safeUnwrap(LuaValue o, Class<?> clazz) {
        if (clazz.isArray())
            return safeUnwrapArray(o, clazz);
        else
            return safeUnwrapSingle(o, clazz);
    }
}
