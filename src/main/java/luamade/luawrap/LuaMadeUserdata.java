package luamade.luawrap;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaUserdata;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.TwoArgFunction;

public abstract class LuaMadeUserdata extends LuaUserdata implements LuaInterface {
    private final static LuaTable metaTable = new LuaTable();

    private final static LuaFunction f = new TwoArgFunction() {
        @Override public LuaValue call(LuaValue ud, LuaValue key) {
            if (! (ud instanceof LuaMadeUserdata) || !key.isstring())
                return NIL;
            return ((LuaMadeUserdata) ud).luaInterface(key.tojstring());
        }
    };
    public LuaMadeUserdata() {
        super(new Object(), getMeta());
    }

    private static LuaTable getMeta() {
        metaTable.set(INDEX, f);
        return metaTable;
    }

    @Override
    public abstract LuaFunction luaInterface(String fname);
}
