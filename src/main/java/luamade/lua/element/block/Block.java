package luamade.lua.element.block;

import org.schema.game.common.data.SegmentPiece;
import luamade.lua.entity.Entity;

public class Block {
    private final SegmentPiece segmentPiece;
    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    public int[] getPos() {
        return new int[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
    }

    public int getId() {
        return segmentPiece.getType();
    }

    public BlockInfo getInfo() {
        return new BlockInfo(segmentPiece);
    }

    public boolean isActive() {
        return segmentPiece.isActive();
    }

    public void setActive(boolean bool) {
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
    }

    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
    }
}
