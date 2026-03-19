package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.manager.RemoteSessionManager;
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
		if(module == null || !(getSegmentPiece().getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return false;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) getSegmentPiece().getSegmentController()).getManagerContainer());
		if(container == null) {
			return false;
		}
		container.linkToComputer(getSegmentPiece(), module.getUUID());
		return true;
	}

	@LuaMadeCallable
	public Boolean disconnect() {
		if(!(getSegmentPiece().getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return false;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) getSegmentPiece().getSegmentController()).getManagerContainer());
		if(container == null) {
			return false;
		}
		container.unlink(getSegmentPiece());
		return true;
	}

	@LuaMadeCallable
	public Boolean isConnected() {
		return getLinkedComputerUUID() != null;
	}

	@LuaMadeCallable
	public String getLinkedComputerUUID() {
		if(!(getSegmentPiece().getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return null;
		}
		AccessPointModuleContainer container = AccessPointModuleContainer.getContainer(((ManagedUsableSegmentController<?>) getSegmentPiece().getSegmentController()).getManagerContainer());
		if(container == null) {
			return null;
		}
		return container.getLinkedComputerUUID(getSegmentPiece());
	}

	@LuaMadeCallable
	public String getLinkedComputerName() {
		if(!(getSegmentPiece().getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return null;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) getSegmentPiece().getSegmentController();
		AccessPointModuleContainer accessPointContainer = AccessPointModuleContainer.getContainer(controller.getManagerContainer());
		ComputerModuleContainer computerContainer = ComputerModuleContainer.getContainer(controller.getManagerContainer());
		if(accessPointContainer == null || computerContainer == null) {
			return null;
		}
		String uuid = accessPointContainer.getLinkedComputerUUID(getSegmentPiece());
		ComputerModule linked = computerContainer.getModuleByUUID(uuid);
		return linked == null ? null : linked.getDisplayName();
	}

	@LuaMadeCallable
	public Boolean isSessionActive() {
		return RemoteSessionManager.isActive()
			&& RemoteSessionManager.getActiveAccessPointIndex() == getSegmentPiece().getAbsoluteIndex();
	}
}
