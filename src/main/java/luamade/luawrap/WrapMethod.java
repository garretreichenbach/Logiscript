package luamade.luawrap;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.CoerceLuaToJava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WrapMethod extends VarArgFunction {
    public final String name;
    private final Class<?> clazz;
    private final Map<Integer, VarArgFunction> methods = new HashMap<>();

    public WrapMethod(String name_in, Class<?> clazz_in) {
        this.name = name_in;
        this.clazz = clazz_in;

        Map<Integer, Method> ms = new HashMap<>();

        for (Method m : this.clazz.getMethods()) {
            if (m.getName().equals(name) && m.isAnnotationPresent(LuaMadeCallable.class))
                ms.put(m.getParameters().length, m);
        }

        for (final int argc : ms.keySet()) {
            final Method m = ms.get(argc);
            VarArgFunction f = new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs vargs) {
                    if (argc != vargs.narg() - 1) throw new LuaError("Incorrect argument count.");

                    if (!clazz.isInstance(vargs.arg1())) throw new LuaError("Method must be called with self.");

                    if (!m.isAnnotationPresent(LuaMadeCallable.class)) throw new LuaError("Method not LuaMadeCallable.");


                    try {
                        Class<?>[] argst = m.getParameterTypes();
                        Object[] argv = new Object[argc];
                        for (int i = 0; i < argc; ++i) {
                            Class<?> argt = argst[i];
                            Object arg = CoerceLuaToJava.coerce(vargs.arg(i + 2), argt);
                            argv[i] = arg;

                            if (!argt.isInstance(arg)) throw new LuaError(String.format("Got %s, expected %s.", arg.getClass(), argt));
                        }
                        Object out = m.invoke(vargs.arg1(), argv);

                        if (out instanceof LuaValue) return (LuaValue) out;
                        else return LuaValue.NIL;
                        //else throw new LuaError("Return value was not LuaValue.");

                    } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
                        throw new LuaError("Got Java exception: " + e);
                    }
                }
            };
            methods.put(argc, f);
        }
    }

    @Override
    public Varargs invoke(Varargs vargs) {
        int argc = vargs.narg() -1;

        if (argc < 0)
            throw new LuaError("Method must be supplied with 'self'.");

        if (methods.containsKey(argc)) {
            return methods.get(argc).invoke(vargs);
        }

        throw new LuaError("No matching Java method.");
    }
}
