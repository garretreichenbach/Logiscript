package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo extends LuaMadeUserdata {

    private final ElementInformation elementInfo;

    public BlockInfo(ElementInformation elementInfo) {
       this.elementInfo = elementInfo;
    }

    @LuaMadeCallable
    public LuaString getName() {
        return LuaString.valueOf(elementInfo.getName());
    }

    @LuaMadeCallable
    public LuaInteger getId() {
        return LuaInteger.valueOf(elementInfo.getId());
    }
}
