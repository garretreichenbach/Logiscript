package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPiecePlayerInteractListener;
import luamade.element.ElementRegistry;
import luamade.gui.DiskDriveDialog;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;

public class DiskDrive extends Block implements SegmentPiecePlayerInteractListener {

	public DiskDrive() {
		super("Disk Drive");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("Single-slot drive that can read and write LuaMade disk items.");
		blockInfo.setDeprecated(true);
		blockInfo.setInRecipe(false);
		blockInfo.setShoppable(false);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
		blockInfo.inventoryType = Inventory.INVENTORY_TYPE_OTHER;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(25, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(16).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(16).getTextureIds());
	}

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(segmentPiece.getType() != ElementRegistry.DISK_DRIVE.getId()) {
			return;
		}
		if(!DiskDriveDialog.isSingleSlotUiAvailable()) {
			return;
		}
		if(!(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
		org.schema.game.common.data.player.inventory.Inventory inventory = controller.getManagerContainer().getInventory(segmentPiece.getAbsoluteIndex());
		if(inventory != null) {
			(new DiskDriveDialog(inventory)).activate();
		}
	}
}
