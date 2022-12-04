package luamade.lua.element.block;

import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo {

    private final ElementInformation elementInfo;

    public BlockInfo(ElementInformation elementInfo) {
       this.elementInfo = elementInfo;
    }

    public String getName() {
        return elementInfo.getName();
    }

    public int getId() {
        return elementInfo.getId();
    }
}
