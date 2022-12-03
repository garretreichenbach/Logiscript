package luamade.lua.element.block;

import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;

public class BlockInfo {

    private final ElementInformation elementInfo;

    public BlockInfo(SegmentPiece segmentPiece) {
        elementInfo = segmentPiece.getInfo();
    }

    public String getElementName() {
        return elementInfo.getName();
    }

    public int getId() {
        return elementInfo.getId();
    }
}
