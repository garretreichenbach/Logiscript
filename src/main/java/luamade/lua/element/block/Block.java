package luamade.lua.element.block;

import luamade.lua.entity.Entity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaInteger;
import org.schema.game.common.data.SegmentPiece;

public class Block extends LuaMadeUserdata {
    private final SegmentPiece segmentPiece;
    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    @LuaMadeCallable
    public LuaInteger[] getPos() {
        return new LuaInteger[] {LuaInteger.valueOf(segmentPiece.getAbsolutePosX()), LuaInteger.valueOf(segmentPiece.getAbsolutePosY()), LuaInteger.valueOf(segmentPiece.getAbsolutePosZ())};
    }

    @LuaMadeCallable
    public LuaInteger getId() {
        return LuaInteger.valueOf(segmentPiece.getType());
    }

    @LuaMadeCallable
    public BlockInfo getInfo() {
        return new BlockInfo(segmentPiece.getInfo());
    }

    @LuaMadeCallable
    public LuaBoolean isActive() {
        return LuaBoolean.valueOf(segmentPiece.isActive());
    }

    @LuaMadeCallable
    public void setActive(LuaBoolean bool) {
        segmentPiece.setActive(bool.v);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
    }

    @LuaMadeCallable
    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
    }
}
