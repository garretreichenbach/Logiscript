package luamade.lua.element.block;

import luamade.lua.data.LuaVec3i;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.entity.Entity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.client.controller.element.world.ClientSegmentProvider;
import org.schema.game.common.controller.SendableSegmentProvider;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.network.objects.remote.RemoteTextBlockPair;
import org.schema.game.network.objects.remote.TextBlockPair;

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
        segmentPiece.setActive(bool);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
        if(segmentPiece.getSegmentController().isOnServer()) segmentPiece.getSegmentController().sendBlockActivation(ElementCollection.getEncodeActivation(segmentPiece, true, bool, false));
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
            if(controller.getManagerContainer().getInventory(index) != null) return new Inventory(controller.getManagerContainer().getInventory(index), controller.getSegmentController().getSegmentBuffer().getPointUnsave(index));
        }
        return null;
    }

    @LuaMadeCallable
    public Boolean isDisplayModule() {
        return segmentPiece.getType() == ElementKeyMap.TEXT_BOX;
    }

    @LuaMadeCallable
    public void setDisplayText(String text) {
        if(isDisplayModule()) {
            segmentPiece.getSegmentController().getTextMap().remove(segmentPiece.getTextBlockIndex());
            segmentPiece.getSegmentController().getTextMap().put(segmentPiece.getTextBlockIndex(), text);
            segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
            if(segmentPiece.getSegmentController().isOnServer()) {
                TextBlockPair textBlockPair = new TextBlockPair();
                textBlockPair.block = segmentPiece.getTextBlockIndex();
                textBlockPair.text = text;
                segmentPiece.getSegmentController().getNetworkObject().textBlockChangeBuffer.add(new RemoteTextBlockPair(textBlockPair, true));
            } else {
                SendableSegmentProvider provider = ((ClientSegmentProvider) segmentPiece.getSegment().getSegmentController().getSegmentProvider()).getSendableSegmentProvider();
                TextBlockPair pair = new TextBlockPair();
                pair.block = segmentPiece.getTextBlockIndex();
                pair.text = text;
                provider.getNetworkObject().textBlockResponsesAndChangeRequests.add(new RemoteTextBlockPair(pair, false));
            }
        }
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
