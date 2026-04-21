package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import api.utils.element.Blocks;
import luamade.element.ElementRegistry;
import luamade.system.module.PasswordPermissionModuleContainer;
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
 * A custom permission module that gates access with a password.
 *
 * <p>Complements StarMade's built-in {@code PUBLIC_PERMISSION_MODULE} (346) and
 * {@code FACTION_PERMISSION_MODULE} (936). When placed adjacent to a block, it
 * requires a faction to first authenticate via a Lua script before access is
 * granted through StarMade's permission system.
 *
 * <p>Passwords are configured by Lua scripts via
 * {@link luamade.lua.element.block.PasswordPermissionModuleBlock}.
 * Authentication is also performed via Lua, meaning the block itself has no
 * player-interact UI — it is intentionally a computer-configured peripheral.
 */
public class PasswordPermissionModule extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public PasswordPermissionModule() {
		super("Password Permission Module");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A password-gated permission module. When placed adjacent to a block, it allows access only to factions that have authenticated via a computer script. Compatible with StarMade's native permission system.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(false);
		blockInfo.volume = 0.05f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(30, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(Blocks.PUBLIC_PERMISSION_MODULE.getInfo().getBuildIconNum());
		blockInfo.setTextureId(Blocks.PUBLIC_PERMISSION_MODULE.getInfo().getTextureIds());
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.PASSWORD_PERMISSION_MODULE.getId()) {
			return;
		}
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		long absIndex = ElementCollection.getIndex(x, y, z);
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
		PasswordPermissionModuleContainer container = PasswordPermissionModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(absIndex);
		}
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.PASSWORD_PERMISSION_MODULE.getId()) {
			return;
		}
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		PasswordPermissionModuleContainer container = PasswordPermissionModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(segmentPiece.getAbsoluteIndex());
		}
	}
}
