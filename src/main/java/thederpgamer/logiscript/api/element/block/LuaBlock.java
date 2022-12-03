package thederpgamer.logiscript.api.element.block;

import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.entity.Entity;
import thederpgamer.logiscript.api.entity.LuaEntity;

public class LuaBlock {
    private final SegmentPiece segmentPiece;
    public LuaBlock(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    public int[] getPos() {
        return new int[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
    }

    public int getId() {
        return segmentPiece.getType();
    }

    public LuaBlockInfo getInfo() {
        return new LuaBlockInfo(segmentPiece);
    }

    public boolean isActive() {
        return segmentPiece.isActive();
    }

    public void setActive(boolean bool) {
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
    }

    public LuaEntity getEntity() {
        return new LuaEntity(segmentPiece.getSegmentController());
    }
}
