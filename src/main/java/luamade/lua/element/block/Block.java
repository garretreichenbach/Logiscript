package luamade.lua.element.block;

import luamade.lua.data.Vec3i;
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

import java.util.Locale;

public class Block extends LuaMadeUserdata {
    private final SegmentPiece segmentPiece;

    public Block(SegmentPiece piece) {
        this.segmentPiece = piece;
    }

    public static Block wrap(SegmentPiece piece) {
        return wrapAs(piece, "auto");
    }

    public static Block wrapAs(SegmentPiece piece, String target) {
        if(piece == null) {
            return null;
        }

        String kind = target == null ? "auto" : target.trim().toLowerCase(Locale.ROOT);

        if("block".equals(kind) || "base".equals(kind)) {
            return new Block(piece);
        }

        if("display".equals(kind) || "displaymodule".equals(kind) || "display_module".equals(kind)) {
            if(piece.getType() == ElementKeyMap.TEXT_BOX) {
                return new DisplayModuleBlock(piece);
            }
            return null;
        }

        if("inventory".equals(kind)) {
            if(hasInventoryAt(piece)) {
                return new InventoryBlock(piece);
            }
            return null;
        }

        if(piece.getType() == ElementKeyMap.TEXT_BOX) {
            return new DisplayModuleBlock(piece);
        }

        if(hasInventoryAt(piece)) {
            return new InventoryBlock(piece);
        }

        return new Block(piece);
    }

    @LuaMadeCallable
    public Vec3i getPos() {
        return new Vec3i(segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ());
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
        return Entity.wrap(segmentPiece.getSegmentController());
    }

    @LuaMadeCallable
    public Entity getEntityInfo() {
        return getEntity();
    }

    @LuaMadeCallable
    public Boolean hasInventory() {
        return hasInventoryAt(segmentPiece);
    }

    @LuaMadeCallable
    public Inventory getInventory() {
        return getInventoryAt(segmentPiece);
    }

    @LuaMadeCallable
    public Boolean isDisplayModule() {
        return segmentPiece.getType() == ElementKeyMap.TEXT_BOX;
    }

    @LuaMadeCallable
    public void setActive(boolean active) {
        segmentPiece.setActive(active);
        segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
        if(segmentPiece.getSegmentController().isOnServer()) {
            segmentPiece.getSegmentController().sendBlockActivation(ElementCollection.getEncodeActivation(segmentPiece, true, active, false));
        }
    }

    @LuaMadeCallable
    public void setDisplayText(String text) {
        if(segmentPiece.getType() != ElementKeyMap.TEXT_BOX) {
            return;
        }

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

    @LuaMadeCallable
    public String getDisplayText() {
        if(isDisplayModule()) return segmentPiece.getSegmentController().getTextMap().get(segmentPiece.getTextBlockIndex());
        return null;
    }

    public SegmentPiece getSegmentPiece() {
        return segmentPiece;
    }

    private static Inventory getInventoryAt(SegmentPiece piece) {
        long index = piece.getAbsoluteIndex();
        if(piece.getSegmentController() instanceof ManagedSegmentController<?>) {
            ManagedSegmentController<?> controller = (ManagedSegmentController<?>) piece.getSegmentController();
            if(controller.getManagerContainer().getInventory(index) != null) {
                return new Inventory(controller.getManagerContainer().getInventory(index), controller.getSegmentController().getSegmentBuffer().getPointUnsave(index));
            }
        }
        return null;
    }

    private static boolean hasInventoryAt(SegmentPiece piece) {
        return getInventoryAt(piece) != null;
    }
}
