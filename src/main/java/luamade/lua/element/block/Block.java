package luamade.lua.element.block;

import luamade.lua.entity.Entity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.SegmentPiece;

public class Block extends LuaMadeUserdata {
    private final SegmentPiece segmentPiece;
    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    @LuaMadeCallable
    public Integer[] getPos() {
        return new Integer[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
    }

    @LuaMadeCallable
    public Short getId() {
        return segmentPiece.getType();
    }

    @LuaMadeCallable
    public BlockInfo getInfo() {
        return new BlockInfo(segmentPiece.getInfo());
    }

    @LuaMadeCallable
    public Boolean isActive() {
        return segmentPiece.isActive();
    }

    @LuaMadeCallable
    public void setActive(Boolean bool) {
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
    }

    @LuaMadeCallable
    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
    }
}
