package luamade.network;

import api.common.GameServer;
import luamade.element.ElementRegistry;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.json.JSONObject;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.game.common.data.player.inventory.InventorySlot;

/**
 * Server-side Remote Control links. A remote stores which access point it was linked at (entity UID + block index, so
 * it survives restarts); the access point in turn names a computer, so relinking the access point redirects every
 * remote paired with it. Range is unlimited: a remote works as long as the access point's entity is loaded.
 */
public final class RemoteControlLinks {

	// "luamade" prefix: protected from script writes by CustomDataUtils
	private static final String ENTITY_UID_KEY = "luamadeRemoteEntityUid";
	private static final String ACCESS_POINT_INDEX_KEY = "luamadeRemoteAccessPointIndex";

	private RemoteControlLinks() {
	}

	/** Links the remote in {@code slot} to {@code accessPoint}. Returns null on success, or a message for the player. */
	static String link(PlayerState player, int slot, SegmentPiece accessPoint) {
		InventorySlot remote = remoteIn(player.getInventory(), slot);
		if(remote == null) {
			return "Hold a Remote Control to connect.";
		}
		ComputerModule module = linkedComputer(accessPoint);
		if(module == null) {
			return "Access point is not linked to a computer.";
		}
		JSONObject data = remote.hasCustomData() ? new JSONObject(remote.getCustomData().toString()) : new JSONObject();
		data.put(ENTITY_UID_KEY, accessPoint.getSegmentController().getUniqueIdentifier());
		data.put(ACCESS_POINT_INDEX_KEY, accessPoint.getAbsoluteIndex());
		data.put("name", "Remote Control (" + module.getDisplayName() + ")");
		remote.setCustomData(data);
		player.getInventory().sendInventoryModification(slot);
		return null;
	}

	/** The live computer the remote in {@code slot} controls, or null if the remote is unlinked or the link is broken. */
	static SegmentPiece resolveComputer(PlayerState player, int slot) {
		InventorySlot remote = remoteIn(player.getInventory(), slot);
		if(remote == null || !remote.hasCustomData()) {
			return null;
		}
		JSONObject data = remote.getCustomData();
		SegmentController entity = GameServer.getServerState().getSegmentControllersByName().get(data.optString(ENTITY_UID_KEY, ""));
		if(entity == null || !data.has(ACCESS_POINT_INDEX_KEY)) {
			return null;
		}
		SegmentPiece accessPoint = entity.getSegmentBuffer().getPointUnsave(data.optLong(ACCESS_POINT_INDEX_KEY));
		if(accessPoint == null || accessPoint.getType() != ElementRegistry.REMOTE_ACCESS_POINT.getId()) {
			return null;
		}
		ComputerModule module = linkedComputer(accessPoint);
		return module == null ? null : liveComputerPiece(module);
	}

	/** True if {@code player} carries a remote linked to {@code computer}; lets a session connect from any range. */
	static boolean hasRemoteFor(PlayerState player, SegmentPiece computer) {
		Inventory inventory = player.getInventory();
		for(int slot : inventory.getSlots()) {
			SegmentPiece linked = resolveComputer(player, slot);
			if(linked != null && linked.getSegmentController() == computer.getSegmentController() && linked.getAbsoluteIndex() == computer.getAbsoluteIndex()) {
				return true;
			}
		}
		return false;
	}

	static ComputerModule linkedComputer(SegmentPiece accessPoint) {
		if(!(accessPoint.getSegmentController() instanceof ManagedUsableSegmentController<?> controller)) {
			return null;
		}
		AccessPointModuleContainer accessPoints = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computers = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPoints == null || computers == null) {
			return null;
		}
		ComputerModule module = computers.getModuleByUUID(accessPoints.getLinkedComputerUUID(accessPoint));
		return module == null || liveComputerPiece(module) == null ? null : module;
	}

	static String displayName(SegmentPiece computer) {
		if(!(computer.getSegmentController() instanceof ManagedUsableSegmentController<?> controller)) {
			return "";
		}
		ComputerModuleContainer computers = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModule module = computers == null ? null : computers.getModule(computer);
		return module == null ? "" : module.getDisplayName();
	}

	private static SegmentPiece liveComputerPiece(ComputerModule module) {
		SegmentPiece piece = module.getSegmentPiece();
		if(piece == null || piece.getSegmentController() == null) {
			return null;
		}
		SegmentPiece live = piece.getSegmentController().getSegmentBuffer().getPointUnsave(piece.getAbsoluteIndex());
		return live != null && live.getType() == ElementRegistry.COMPUTER.getId() ? live : null;
	}

	private static InventorySlot remoteIn(Inventory inventory, int slot) {
		InventorySlot remote = slot < 0 ? null : inventory.getSlot(slot);
		return remote != null && remote.count() > 0 && remote.getType() == ElementRegistry.REMOTE_CONTROL.getId() ? remote : null;
	}
}
