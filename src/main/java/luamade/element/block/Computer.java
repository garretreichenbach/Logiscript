package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.*;
import api.utils.element.Blocks;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.client.view.cubes.shapes.BlockStyle;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Segment;

public class Computer extends Block implements SegmentPiecePlayerInteractListener, SegmentPieceConsoleInteractListener, SegmentPieceAddListener, SegmentPieceAddByMetadataListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public Computer() {
		super("Computer");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A fully programmable computer utilizing the LUA language.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice((long) (Blocks.DISPLAY_MODULE.getInfo().price * 1.5f));
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.type = Blocks.DISPLAY_MODULE.getInfo().type;
		blockInfo.volume = 0.1f;
		blockInfo.consoleAccessible = true;
	}

	@Override
	public void postInitData() {
		BlockUtils.addControlling(blockInfo, ElementInformation::isConsole);
		if(ElementRegistry.isRRSInstalled()) {
			BlockConfig.addRecipe(blockInfo, ElementRegistry.RRSElements.BLOCK_ASSEMBLER.getId(), (int) Blocks.DISPLAY_MODULE.getInfo().getFactoryBakeTime(),
					new FactoryResource(1, ElementRegistry.RRSElements.PRISMATIC_CIRCUIT.getId()),
					new FactoryResource(5, ElementRegistry.RRSElements.THRENS_WIRE_MATRIX.getId()),
					new FactoryResource(1, ElementRegistry.RRSElements.CRYSTAL_PANEL.getId()));
		} else {
			BlockConfig.addRecipe(blockInfo, Blocks.ADVANCED_FACTORY.getId(), (int) Blocks.DISPLAY_MODULE.getInfo().getFactoryBakeTime(),
					new FactoryResource(1, Blocks.DISPLAY_MODULE.getId()),
					new FactoryResource(1, Blocks.BLUE_CONSOLE.getId()));
		}
	}

	@Override
	public void initResources() {
		BlockConfig.assignLod(blockInfo, LuaMade.getInstance(), "Computer", null);
		blockInfo.blockStyle = BlockStyle.NORMAL24;
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
	}

	@Override
	public void onConsoleInteract(SegmentPiece consolePiece, SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		onInteract(segmentPiece, playerState, playerInteractionControlManager);
	}

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(segmentPiece.getType() == getId()) {
			if(!(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
				return;
			}
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
			ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
			if(container != null) {
				ComputerModule module = container.getOrCreateModule(segmentPiece);
				if(module != null) {
					(new ComputerDialog(module)).activate();
				}
			}
		}
	}

	@Override
	public void onAdd(SegmentController segmentController, short type, byte orientation, byte x, byte y, byte z, Segment segment, boolean updateSegmentBuffer, long index, boolean server) {
		if(type == getId()) {
			SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(index);
			if(segmentPiece != null) {
				if(segmentController instanceof ManagedUsableSegmentController<?>) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segmentController).getManagerContainer());
					if(container != null) {
						container.addModule(segmentPiece);
					}
				}
			}
		}
	}

	@Override
	public void onAdd(short newType, byte orientation, byte x, byte y, byte z, long absIndex, SegmentController segmentController, boolean server) {
		SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(absIndex);
		if(segmentPiece != null && newType == getId()) {
			if(segmentController instanceof ManagedUsableSegmentController<?>) {
				ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segmentController).getManagerContainer());
				if(container != null) {
					container.addModule(segmentPiece);
				}
			}
		}
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type == getId()) {
			SegmentPiece segmentPiece = segment.getSegmentController().getSegmentBuffer().getPointUnsave(ElementCollection.getIndex(x, y, z));
			if(segmentPiece != null) {
				if(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>) {
					ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
					if(container != null) {
						container.removeModule(segmentPiece);
					}
				}
			}
		}
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece != null) {
			if(sendableSegmentController instanceof ManagedUsableSegmentController<?>) {
				ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
				if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
					if(container != null) {
						container.removeModule(segmentPiece);
					}
				}
			}
		}
	}
}