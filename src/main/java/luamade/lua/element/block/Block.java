package luamade.lua.element.block;

import luamade.lua.data.LuaVec3i;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.entity.Entity;
import luamade.lua.entity.EntityInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;

public class Block extends LuaMadeUserdata {
    private final SegmentPiece segmentPiece;

    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    @LuaMadeCallable
    public LuaVec3i getPos() {
        return new LuaVec3i(segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ());
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
    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
    }

    @LuaMadeCallable
    public EntityInfo getEntityInfo() {
        return new EntityInfo(segmentPiece.getSegmentController());
    }

    @LuaMadeCallable
    public BlockControl getControl() {
        return new BlockControl(this);
    }

    @LuaMadeCallable
    public Boolean hasInventory() {
        return getInventory() != null;
    }

    @LuaMadeCallable
    public Inventory getInventory() {
        long index = segmentPiece.getAbsoluteIndex();
        if(segmentPiece.getSegmentController() instanceof ManagedSegmentController<?>) {
            ManagedSegmentController<?> controller = (ManagedSegmentController<?>) segmentPiece.getSegmentController();
            if(controller.getManagerContainer().getInventory(index) != null) return new Inventory(controller.getManagerContainer().getInventory(index), controller.getSegmentController().getSegmentBuffer().getPointUnsave(index));
        }
        return null;
    }

    @LuaMadeCallable
    public Boolean isDisplayModule() {
        return segmentPiece.getType() == ElementKeyMap.TEXT_BOX;
    }

    @LuaMadeCallable
    public String getDisplayText() {
        if(isDisplayModule()) return segmentPiece.getSegmentController().getTextMap().get(segmentPiece.getTextBlockIndex());
        return null;
    }

    public SegmentPiece getSegmentPiece() {
        return segmentPiece;
    }
}
