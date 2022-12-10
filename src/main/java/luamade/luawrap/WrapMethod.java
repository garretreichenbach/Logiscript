package luamade.luawrap;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaNil;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class WrapMethod extends VarArgFunction {
    public final String name;
    private final Class<?> clazz;
    private final Map<Integer, VarArgFunction> methods = new HashMap<>();
    private VarArgFunction varadic = null;

    public WrapMethod(String name_in, Class<?> clazz_in) {
        this.name = name_in;
        this.clazz = clazz_in;

        Map<Integer, Method> ms = new HashMap<>();
        VarArgFunction vm = null;

        for (Method m : this.clazz.getMethods())
            if (m.getName().equals(name) && m.isAnnotationPresent(LuaMadeCallable.class)) {
                if (!ms.containsKey(m.getParameters().length))
                    ms.put(m.getParameters().length, m);
                else
                    throw new LuaError(String.format("Ambiguous @LuaMadeCallable '%s:%s'.", clazz, name));
            }

        for (final int argc : ms.keySet()) {
            final Method m = ms.get(argc);
            final Class<?>[] argst = m.getParameterTypes();
            final boolean isVrad = argc > 0 && argst[argc-1] == Varargs.class;
            VarArgFunction f = new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs vargs) {
                    if (!isVrad ? argc+1 != vargs.narg() : argc > vargs.narg()) //If the function isn't varadic, the argument count must match exactly
                        throw new LuaError("Incorrect argument count."); //If the function is varadic, the final Vararg can be nil.
                    if (!clazz.isInstance(vargs.arg1())) throw new LuaError("Method must be called with self.");
                    if (!m.isAnnotationPresent(LuaMadeCallable.class)) throw new LuaError("Method not LuaMadeCallable.");

                    try {
                        Object[] argv = new Object[argc];
                        LuaValue o = vargs.arg1();

                        for (int i = 0; i < argc; ++i) {
                            vargs = vargs.subargs(2);

                            if (i+1 == argc && argst[i] == Varargs.class)
                                argv[i] = vargs;
                            else
                                argv[i] = WrapUtils.unwrap(vargs.arg1(), argst[i]);
                        }
                        Object out = m.invoke(o, argv);
                        return WrapUtils.wrap(out);

                    } catch (IllegalAccessException | IllegalArgumentException e) {
                        throw new LuaError("Got Java exception: " + e);
                    }
                    catch (InvocationTargetException e) {
                        throw new LuaError("Java method threw exception: " + e.getCause());
                    }
                }
            };
            methods.put(argc, f);
            if (isVrad) vm = f;
        }
        varadic = vm;
    }

    @Override
    public Varargs invoke(Varargs vargs) {
        int argc = vargs.narg() -1;

        if (argc < 0)
            throw new LuaError("Method must be supplied with 'self'.");

        if (methods.containsKey(argc))
            return methods.get(argc).invoke(vargs);

        if (varadic != null)
            return varadic.invoke(vargs);

        throw new LuaError("No matching Java method.");
    }
}
