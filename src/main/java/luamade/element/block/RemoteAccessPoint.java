package luamade.element.block;

import api.block.BlockConfig;
import api.event.block.SegmentPieceKilledListener;
import api.event.block.SegmentPiecePlayerInteractListener;
import api.event.block.SegmentPieceRemoveListener;
import api.network.packets.PacketUtil;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.manager.ResourceManager;
import luamade.network.PacketCSRemoteControl;
import luamade.system.module.AccessPointModuleContainer;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.client.view.cubes.shapes.BlockStyle;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Segment;

import javax.annotation.Nullable;

public class RemoteAccessPoint extends Block implements SegmentPiecePlayerInteractListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public RemoteAccessPoint() {
		super("Remote Access Point");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("Pairs Remote Control items with the computer this access point is linked to. Activate it while holding a remote to link it; the remote then works from anywhere.");
		blockInfo.setDeprecated(false);
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.inventoryGroup = "Networking";
		blockInfo.volume = 0.1f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(20, (short) 220));
	}

	@Override
	public void initResources() {
		BlockConfig.assignLod(blockInfo, LuaMade.getInstance(), ResourceManager.Models.REMOTE_ACCESS_POINT.getName(), null);
		blockInfo.blockStyle = BlockStyle.NORMAL24; // assignLod leaves SPRITE, which can't reach every face
		if(!ResourceManager.BlockIcons.REMOTE_ACCESS_POINT.apply(blockInfo)) {
			blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
		}
	}

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager interactionManager) {
		if(segmentPiece.getType() != ElementRegistry.REMOTE_ACCESS_POINT.getId() || interactionManager == null) {
			return;
		}
		// The server validates the held item and the access point's link, stores the link on the remote, then answers
		// with PacketSCRemoteOpen to open the linked computer's terminal.
		PacketUtil.sendPacketToServer(PacketCSRemoteControl.link(interactionManager.getSelectedSlot(), segmentPiece.getSegmentController().getId(), segmentPiece.getAbsoluteIndex()));
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			return;
		}

		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?> controller)) {
			return;
		}

		SegmentPiece segmentPiece = segment.getSegmentController().getSegmentBuffer().getPointUnsave(org.schema.game.common.data.element.ElementCollection.getIndex(x, y, z));
		if(segmentPiece == null) {
			return;
		}

        AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeAccessPoint(segmentPiece.getAbsoluteIndex());
		}
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			return;
		}

		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?> controller)) {
			return;
		}

        AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeAccessPoint(segmentPiece.getAbsoluteIndex());
		}
	}
}
