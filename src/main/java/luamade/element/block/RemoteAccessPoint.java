package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPiecePlayerInteractListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import luamade.element.ElementRegistry;
import luamade.manager.RemoteSessionManager;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.game.common.data.world.Segment;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class RemoteAccessPoint extends Block implements SegmentPiecePlayerInteractListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	private static final String REMOTE_COMPUTER_UUID_KEY = "luamadeRemoteComputerUuid";
	private static final String REMOTE_ACCESS_POINT_INDEX_KEY = "luamadeRemoteAccessPointIndex";
	private static final Field SELECTED_SLOT_FIELD = resolveSelectedSlotField();

	public RemoteAccessPoint() {
		super("Remote Access Point");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("Lets linked remote control items forward input to a LuaMade computer without opening its UI.");
		blockInfo.setInRecipe(false);
		blockInfo.setShoppable(false);
		blockInfo.setDeprecated(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(20, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(451).getTextureIds());
	}

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager interactionManager) {
		if(segmentPiece.getType() != ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			return;
		}
		if(!(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();

		InventorySlot heldSlot = getHeldSlot(playerState, interactionManager);
		if(heldSlot == null || heldSlot.getType() != ElementRegistry.REMOTE_CONTROL.getId()) {
			notifyPlayer(playerState, "Hold a Remote Controller to connect.");
			return;
		}

		AccessPointModuleContainer accessPointContainer = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computerContainer = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPointContainer == null || computerContainer == null) {
			notifyPlayer(playerState, "Remote connection unavailable on this structure.");
			return;
		}

		String computerUuid = accessPointContainer.getLinkedComputerUUID(segmentPiece);
		if(computerUuid == null || computerUuid.trim().isEmpty()) {
			notifyPlayer(playerState, "Access point is not linked to a computer.");
			return;
		}

		ComputerModule module = computerContainer.getModuleByUUID(computerUuid);
		if(module == null) {
			notifyPlayer(playerState, "Linked computer is offline or missing.");
			return;
		}

		storeRemoteLinkMetadata(heldSlot, segmentPiece, module);
		RemoteSessionManager.connect(module, segmentPiece.getAbsoluteIndex(), playerState);
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			return;
		}
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		SegmentPiece segmentPiece = segment.getSegmentController().getSegmentBuffer().getPointUnsave(org.schema.game.common.data.element.ElementCollection.getIndex(x, y, z));
		if(segmentPiece == null) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
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
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeAccessPoint(segmentPiece.getAbsoluteIndex());
		}
	}

	private static void notifyPlayer(PlayerState playerState, String message) {
		if(playerState == null || message == null || message.isEmpty()) {
			return;
		}
		if(tryInvokeMessageMethod(playerState, "sendServerMessage", message)) {
			return;
		}
		if(tryInvokeMessageMethod(playerState, "sendClientMessage", message)) {
			return;
		}
		tryInvokeMessageMethod(playerState, "sendTextMessage", message);
	}

	private static boolean tryInvokeMessageMethod(PlayerState playerState, String methodName, String message) {
		try {
			Method method = playerState.getClass().getMethod(methodName, String.class);
			method.invoke(playerState, message);
			return true;
		} catch(Exception ignored) {
			return false;
		}
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
