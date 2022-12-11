package luamade.lua.element.block;

import luamade.lua.LuaVec3i;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.entity.Entity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;

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
    public void setActive(boolean bool) {
        System.err.println("Setting active to " + bool);
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
        if (segmentPiece.getSegmentController().isOnServer())
            segmentPiece.getSegmentController().sendBlockActivation(ElementCollection.getEncodeActivation(segmentPiece, true, bool, false));
    }

    @LuaMadeCallable
    public Entity getEntity() {
        return new Entity(segmentPiece.getSegmentController());
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
            if(controller.getManagerContainer().getInventory(index) != null) return new Inventory(controller.getManagerContainer().getInventory(index), segmentPiece);
        }
        return null;
    }

    public SegmentPiece getSegmentPiece() {
        return segmentPiece;
    }
}
