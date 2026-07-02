package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

public class RemoteAccessPointBlock extends Block {

	public RemoteAccessPointBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
	}

	@LuaMadeCallable
	public Boolean connect() {
		ComputerModule module = getModule();
		if(module == null) {
			return false;
		}
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return false;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) piece.getSegmentController()).getManagerContainer());
		if(container == null) {
			return false;
		}
		container.linkToComputer(piece, module.getUUID());
		return true;
	}

	@LuaMadeCallable
	public Boolean disconnect() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return false;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) piece.getSegmentController()).getManagerContainer());
		if(container == null) {
			return false;
		}
		container.unlink(piece);
		return true;
	}

	@LuaMadeCallable
	public Boolean isConnected() {
		return getLinkedComputerUUID() != null;
	}

	@LuaMadeCallable
	public String getLinkedComputerUUID() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return null;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) piece.getSegmentController()).getManagerContainer());
		if(container == null) {
			return null;
		}
		return container.getLinkedComputerUUID(piece);
	}

	@LuaMadeCallable
	public String getLinkedComputerName() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return null;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		AccessPointModuleContainer accessPointContainer = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computerContainer = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPointContainer == null || computerContainer == null) {
			return null;
		}
		String uuid = accessPointContainer.getLinkedComputerUUID(piece);
		ComputerModule linked = computerContainer.getModuleByUUID(uuid);
		return linked == null ? null : linked.getDisplayName();
	}

	/**
	 * True if anyone is currently viewing the linked computer — direct and
	 * remote-access-point sessions are the same networked session mechanism
	 * now, so there's no separate "remote mode" to distinguish; this reflects
	 * whether the linked computer has any active viewer at all.
	 */
	@LuaMadeCallable
	public Boolean isSessionActive() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return false;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		AccessPointModuleContainer accessPointContainer = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computerContainer = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPointContainer == null || computerContainer == null) {
			return false;
		}
		String uuid = accessPointContainer.getLinkedComputerUUID(piece);
		ComputerModule linked = computerContainer.getModuleByUUID(uuid);
		if(linked == null || linked.getSegmentPiece() == null) {
			return false;
		}
		return computerContainer.hasAnyViewer(linked.getSegmentPiece().getAbsoluteIndex());
	}
}
