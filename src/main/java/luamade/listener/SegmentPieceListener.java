package luamade.listener;

import api.listener.fastevents.segmentpiece.SegmentPieceAddListener;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPiecePlayerInteractListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.gui.DiskDriveDialog;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Segment;

public class SegmentPieceListener implements SegmentPiecePlayerInteractListener, SegmentPieceAddListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>) {
			if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
				ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segmentPiece.getSegmentController()).getManagerContainer());
				if(container != null) {
					ComputerModule module = container.getOrCreateModule(segmentPiece);
					if(module != null) {
						(new ComputerDialog(module)).activate();
					}
				}
			} else if(segmentPiece.getType() == ElementRegistry.DISK_DRIVE.getId()) {
				if(!DiskDriveDialog.isSingleSlotUiAvailable()) {
					return;
				}

				ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
				org.schema.game.common.data.player.inventory.Inventory inventory = controller.getManagerContainer().getInventory(segmentPiece.getAbsoluteIndex());
				if(inventory != null) {
					(new DiskDriveDialog(inventory)).activate();
				}
			}
		}
	}

	@Override
	public void onAdd(SegmentController segmentController, short type, byte orientation, byte x, byte y, byte z, Segment segment, boolean updateSegmentBuffer, long index, boolean server) {
		SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(index);
		if(segmentPiece != null) {
			if(segmentController instanceof ManagedUsableSegmentController<?>) {
				if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segmentController).getManagerContainer());
					if(container != null) {
						container.addModule(segmentPiece);
					}
				}
			}
		}
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		SegmentPiece segmentPiece = segment.getSegmentController().getSegmentBuffer().getPointUnsave(ElementCollection.getIndex(x, y, z));
		if(segmentPiece != null) {
			if(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>) {
				if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segment.getSegmentController()).getManagerContainer());
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
				if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) sendableSegmentController).getManagerContainer());
					if(container != null) {
						container.removeModule(segmentPiece);
					}
				}
			}
		}
	}
}
