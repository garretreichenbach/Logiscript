package luamade.lua.element.block;

import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo extends LuaMadeUserdata {

    private final ElementInformation elementInfo;

    public BlockInfo(ElementInformation elementInfo) {
       this.elementInfo = elementInfo;
    }

    @LuaCallable
    public String getName() {
        return elementInfo.getName();
    }

    @LuaCallable
    public Short getId() {
        return elementInfo.getId();
    }
}
