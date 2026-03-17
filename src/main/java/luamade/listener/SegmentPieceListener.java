package luamade.listener;

import api.listener.fastevents.segmentpiece.*;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
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

public class SegmentPieceListener implements SegmentPiecePlayerInteractListener, SegmentPieceAddByMetadataListener, SegmentPieceAddListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>) {
			if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
				ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) segmentPiece.getSegmentController()).getManagerContainer());
				if(container != null && container.getModule(segmentPiece) != null) {
					(new ComputerDialog(container.getModule(segmentPiece))).activate();
				}
			}
		}
	}

	@Override
	public void onAdd(short type, byte orientation, byte x, byte y, byte z, long index, boolean server) {

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
