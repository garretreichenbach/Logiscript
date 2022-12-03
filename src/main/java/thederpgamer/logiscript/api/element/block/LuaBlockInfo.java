package thederpgamer.logiscript.api.element.block;

import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;

public class LuaBlockInfo {

    private final ElementInformation elementInfo;

    public LuaBlockInfo(SegmentPiece segmentPiece) {
        elementInfo = segmentPiece.getInfo();
    }

    public String getElementName() {
        return elementInfo.getName();
    }

    public int getId() {
        return elementInfo.getId();
    }
}
