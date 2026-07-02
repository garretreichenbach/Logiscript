package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import api.utils.element.Blocks;
import luamade.element.ElementRegistry;
import luamade.system.module.ProjectorModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.world.Segment;

/**
 * A holographic projector block. A computer's Lua script drives it through the
 * {@code projector} peripheral to render 2D/3D vector graphics in world space
 * near the block. Frames are server-authoritative and rendered identically for
 * every nearby player (see {@link ProjectorModuleContainer} and
 * {@link luamade.gui.ProjectorWorldDrawer}).
 *
 * <p>Mirrors {@link Vault}: block → {@link api.utils.game.module.util.SystemModule}
 * container → tag-sync. There is no interact dialog in v1, so this reuses an
 * existing block texture and is not activatable. Destroying the block clears its
 * frame.
 */
public class Projector extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public Projector() {
		super("Projector");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A holographic projector. A computer script drives it (peripherals.wrap(block, \"projector\")) to draw 2D/3D vector graphics in world space, visible to every nearby player. Coordinates are block-space offsets relative to the projector, clamped to +/-5 blocks per axis.");
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 4);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(false);
		blockInfo.volume = 0.2f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo,
				ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(),
				(int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(),
				new FactoryResource(1, ElementKeyMap.TEXT_BOX),
				new FactoryResource(200, (short) 220));
	}

	@Override
	public void initResources() {
		// v1 reuses an existing block model/texture; a bespoke projector mesh is a follow-up.
		blockInfo.setBuildIconNum(Blocks.DECORATIVE_SERVER.getInfo().getBuildIconNum());
		blockInfo.setTextureId(Blocks.DECORATIVE_SERVER.getInfo().getTextureIds());
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.PROJECTOR.getId()) return;
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) return;
		long absIndex = ElementCollection.getIndex(x, y, z);
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
		ProjectorModuleContainer container = ProjectorModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) container.removeBlock(absIndex);
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.PROJECTOR.getId()) return;
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) return;
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		ProjectorModuleContainer container = ProjectorModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) container.removeBlock(segmentPiece.getAbsoluteIndex());
	}
}
