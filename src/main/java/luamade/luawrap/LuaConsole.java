package luamade.luawrap;

import luamade.lua.element.block.Block;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.schema.game.common.data.SegmentPiece;

public class LuaConsole extends LuaMadeUserdata {

    private final SegmentPiece segmentPiece;

    public LuaConsole(SegmentPiece segmentPiece) {
        this.segmentPiece = segmentPiece;
    }

    public Block getBlock() {
        return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
    }
    private static final LuaFunction lua_getBlock = new OneArgFunction() {
        @Override
        public LuaValue call(LuaValue self) {
            return CoerceJavaToLua.coerce(((LuaConsole) self).getBlock());
        }
    };

    @LuaMadeCallable
    public LuaValue getIntInc(Integer a) {
        return LuaValue.valueOf(a+1);
    }

    @LuaMadeCallable
    public LuaValue getIntInc(Integer a, String b) {
        return LuaValue.valueOf(a+b.length()+1);
    }
}
