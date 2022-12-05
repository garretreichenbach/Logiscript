package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo extends LuaMadeUserdata {

    private final ElementInformation elementInfo;

    public BlockInfo(ElementInformation elementInfo) {
       this.elementInfo = elementInfo;
    }

    @LuaMadeCallable
    public String getName() {
        return elementInfo.getName();
    }

    @LuaMadeCallable
    public Short getId() {
        return elementInfo.getId();
    }
}
