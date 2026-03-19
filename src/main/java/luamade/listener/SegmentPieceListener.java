package luamade.listener;

import api.listener.fastevents.segmentpiece.SegmentPieceAddListener;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPiecePlayerInteractListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.gui.DiskDriveDialog;
import luamade.manager.RemoteSessionManager;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.json.JSONObject;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.game.common.data.world.Segment;

import java.lang.reflect.Field;

public class SegmentPieceListener implements SegmentPiecePlayerInteractListener, SegmentPieceAddListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	private static final String REMOTE_COMPUTER_UUID_KEY = "luamadeRemoteComputerUuid";
	private static final String REMOTE_ACCESS_POINT_INDEX_KEY = "luamadeRemoteAccessPointIndex";
	private static final Field SELECTED_SLOT_FIELD = resolveSelectedSlotField();

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(!(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}

		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
		if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
			ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
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

			org.schema.game.common.data.player.inventory.Inventory inventory = controller.getManagerContainer().getInventory(segmentPiece.getAbsoluteIndex());
			if(inventory != null) {
				(new DiskDriveDialog(inventory)).activate();
			}
		} else if(segmentPiece.getType() == ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			handleRemoteAccessPointInteract(segmentPiece, playerState, playerInteractionControlManager, controller);
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
				ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
				if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
					ComputerModuleContainer container = ComputerModuleContainer.getContainer(controller.getManagerContainer());
					if(container != null) {
						container.removeModule(segmentPiece);
					}
				} else if(segmentPiece.getType() == ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
					AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
					if(container != null) {
						container.removeAccessPoint(segmentPiece.getAbsoluteIndex());
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
				} else if(segmentPiece.getType() == ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
					AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
					if(container != null) {
						container.removeAccessPoint(segmentPiece.getAbsoluteIndex());
					}
				}
			}
		}
	}

	private void handleRemoteAccessPointInteract(SegmentPiece accessPoint, PlayerState playerState, PlayerInteractionControlManager interactionManager, ManagedUsableSegmentController<?> controller) {
		InventorySlot heldSlot = getHeldSlot(playerState, interactionManager);
		if(heldSlot == null || heldSlot.getType() != ElementRegistry.REMOTE_CONTROL.getId()) {
			return;
		}

		AccessPointModuleContainer accessPointContainer = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computerContainer = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPointContainer == null || computerContainer == null) {
			return;
		}

		String computerUuid = accessPointContainer.getLinkedComputerUUID(accessPoint);
		if(computerUuid == null || computerUuid.trim().isEmpty()) {
			return;
		}

		ComputerModule module = computerContainer.getModuleByUUID(computerUuid);
		if(module == null) {
			return;
		}

		storeRemoteLinkMetadata(heldSlot, accessPoint, module);
		RemoteSessionManager.connect(module, accessPoint.getAbsoluteIndex(), playerState);
	}

	private static InventorySlot getHeldSlot(PlayerState playerState, PlayerInteractionControlManager interactionManager) {
		if(playerState == null || interactionManager == null || SELECTED_SLOT_FIELD == null) {
			return null;
		}
		try {
			int selectedSlot = SELECTED_SLOT_FIELD.getInt(interactionManager);
			if(selectedSlot < 0) {
				return null;
			}
			return playerState.getInventory().getSlot(selectedSlot);
		} catch(IllegalAccessException exception) {
			return null;
		}
	}

	private static void storeRemoteLinkMetadata(InventorySlot slot, SegmentPiece accessPoint, ComputerModule module) {
		if(slot == null || accessPoint == null || module == null) {
			return;
		}
		JSONObject customData = slot.getOrCreateCustomData();
		customData.put(REMOTE_COMPUTER_UUID_KEY, module.getUUID());
		customData.put(REMOTE_ACCESS_POINT_INDEX_KEY, accessPoint.getAbsoluteIndex());
		slot.setCustomData(customData);
	}

	private static Field resolveSelectedSlotField() {
		try {
			Field field = PlayerInteractionControlManager.class.getDeclaredField("selectedSlot");
			field.setAccessible(true);
			return field;
		} catch(Exception exception) {
			return null;
		}
	}
}
