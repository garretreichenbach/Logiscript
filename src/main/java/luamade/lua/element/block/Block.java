package luamade.lua.element.block;

import luamade.lua.entity.Entity;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.SegmentPiece;

public class Block extends LuaMadeUserdata {
    private final SegmentPiece segmentPiece;
    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    @LuaCallable
    public Integer[] getPos() {
        return new Integer[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
    }

    @LuaCallable
    public Short getId() {
        return segmentPiece.getType();
    }

    @LuaCallable
    public BlockInfo getInfo() {
        return new BlockInfo(segmentPiece.getInfo());
    }

    @LuaCallable
    public Boolean isActive() {
        return segmentPiece.isActive();
    }

    @LuaCallable
    public void setActive(Boolean bool) {
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
    }

    @LuaCallable
    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
    }
}
