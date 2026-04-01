package luamade.lua.element.block;

import api.utils.element.Blocks;
import com.bulletphysics.linearmath.Transform;
import luamade.lua.data.Vec3f;
import luamade.lua.data.Vec3i;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.entity.Entity;
import luamade.lua.peripheral.PeripheralRegistry;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.LuaError;
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
	private final ComputerModule module;

	public Block(SegmentPiece piece) {
		this(piece, null);
	}

	public Block(SegmentPiece piece, ComputerModule module) {
		segmentPiece = piece;
		this.module = module;
	}

	public static Block wrap(SegmentPiece piece) {
		return wrapAs(piece, "auto", null);
	}

	public static Block wrap(SegmentPiece piece, ComputerModule module) {
		return wrapAs(piece, "auto", module);
	}

	public static Block wrapAs(SegmentPiece piece, String target) {
		return wrapAs(piece, target, null);
	}

	public static Block wrapAs(SegmentPiece piece, String target, ComputerModule module) {
		return PeripheralRegistry.wrapAs(piece, target, module);
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

	public static boolean hasInventoryAt(SegmentPiece piece) {
		return getInventoryAt(piece) != null;
	}

	@LuaMadeCallable
	public Vec3i getPos() {
		SegmentPiece livePiece = requireLiveSegmentPiece();
		return new Vec3i(livePiece.getAbsolutePosX(), livePiece.getAbsolutePosY(), livePiece.getAbsolutePosZ());
	}

	@LuaMadeCallable
	public Vec3f getWorldPos() {
		SegmentPiece livePiece = requireLiveSegmentPiece();
		Transform transform = new Transform();
		livePiece.getTransform(transform);
		return new Vec3f(transform.origin);
	}

	@LuaMadeCallable
	public Short getId() {
		return requireLiveSegmentPiece().getType();
	}

	@LuaMadeCallable
	public BlockInfo getInfo() {
		return new BlockInfo(requireLiveSegmentPiece().getInfo());
	}

	@LuaMadeCallable
	public Boolean isActive() {
		return requireLiveSegmentPiece().isActive();
	}

	@LuaMadeCallable
	public Entity getEntity() {
		return Entity.wrap(requireLiveSegmentPiece().getSegmentController());
	}

	@LuaMadeCallable
	public Entity getEntityInfo() {
		return getEntity();
	}

	@LuaMadeCallable
	public Boolean hasInventory() {
		return hasInventoryAt(requireLiveSegmentPiece());
	}

	@LuaMadeCallable
	public Inventory getInventory() {
		return getInventoryAt(requireLiveSegmentPiece());
	}

	@LuaMadeCallable
	public Boolean isDisplayModule() {
		return requireLiveSegmentPiece().getType() == ElementKeyMap.TEXT_BOX;
	}

	@LuaMadeCallable
	public void setActive(boolean active) {
		SegmentPiece livePiece = requireLiveSegmentPiece();
		livePiece.setActive(active);
		livePiece.applyToSegment(livePiece.getSegmentController().isOnServer());
		if(livePiece.getSegmentController().isOnServer()) {
			livePiece.getSegmentController().sendBlockActivation(ElementCollection.getEncodeActivation(livePiece, true, active, false));
		}
	}

	@LuaMadeCallable
	public String getDisplayText() {
		SegmentPiece livePiece = requireDisplayModuleSegmentPiece();
		return livePiece.getSegmentController().getTextMap().get(livePiece.getTextBlockIndex());
	}

	@LuaMadeCallable
	public void setDisplayText(String text) {
		SegmentPiece livePiece = requireDisplayModuleSegmentPiece();
		String safeText = text == null ? "" : text;

		livePiece.getSegmentController().getTextMap().remove(livePiece.getTextBlockIndex());
		livePiece.getSegmentController().getTextMap().put(livePiece.getTextBlockIndex(), safeText);
		livePiece.applyToSegment(livePiece.getSegmentController().isOnServer());

		if(livePiece.getSegmentController().isOnServer()) {
			TextBlockPair textBlockPair = new TextBlockPair();
			textBlockPair.block = livePiece.getTextBlockIndex();
			textBlockPair.text = safeText;
			livePiece.getSegmentController().getNetworkObject().textBlockChangeBuffer.add(new RemoteTextBlockPair(textBlockPair, true));
		} else {
			SendableSegmentProvider provider = ((ClientSegmentProvider) livePiece.getSegment().getSegmentController().getSegmentProvider()).getSendableSegmentProvider();
			TextBlockPair pair = new TextBlockPair();
			pair.block = livePiece.getTextBlockIndex();
			pair.text = safeText;
			provider.getNetworkObject().textBlockResponsesAndChangeRequests.add(new RemoteTextBlockPair(pair, false));
		}
	}

	public SegmentPiece getSegmentPiece() {
		return requireLiveSegmentPiece();
	}

	protected SegmentPiece requireLiveSegmentPiece() {
		if(segmentPiece == null || segmentPiece.getSegmentController() == null) {
			throw new LuaError("Block reference is no longer valid");
		}

		SegmentPiece livePiece = resolveLiveSegmentPiece();
		if(livePiece == null) {
			throw new LuaError("Block no longer exists");
		}
		if(livePiece.getType() != segmentPiece.getType()) {
			throw new LuaError("Block type changed since it was referenced");
		}
		return livePiece;
	}

	protected SegmentPiece requireDisplayModuleSegmentPiece() {
		SegmentPiece livePiece = requireLiveSegmentPiece();
		if(livePiece.getType() != Blocks.DISPLAY_MODULE.getId()) {
			throw new LuaError("Block is not a display module");
		}
		return livePiece;
	}

	private SegmentPiece resolveLiveSegmentPiece() {
		if(segmentPiece == null || segmentPiece.getSegmentController() == null) {
			return null;
		}
		return segmentPiece.getSegmentController().getSegmentBuffer().getPointUnsave(segmentPiece.getAbsoluteIndex());
	}

	protected ComputerModule getModule() {
		return module;
	}
}
