package luamade.lua.element.system.module;

import api.common.GameServer;
import luamade.lua.data.Vec3f;
import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;
import org.schema.game.server.ai.ShipAIEntity;

import javax.vecmath.Vector3f;

public class Thrust extends Module {

	public Thrust(SegmentController segmentController) {
		super(segmentController);
	}

	@LuaMadeCallable
	public Float getTMR() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getThrustMassRatio();
		else return 0.0f;
	}

	@LuaMadeCallable
	public Float getThrust() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getActualThrust();
		else return 0.0f;
	}

	@LuaMadeCallable
	public Float getMaxSpeed() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getMaxSpeedAbsolute();
		else return 0.0f;
	}

	@LuaMadeCallable
	@Override
	public Integer getSize() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrust().getElementManager().totalSize;
		else return 0;
	}

	@LuaMadeCallable
	public void startMovement() {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		Vector3f forward = new Vector3f(
			segmentController.getWorldTransform().basis.m02,
			segmentController.getWorldTransform().basis.m12,
			segmentController.getWorldTransform().basis.m22
		);
		startMovement(new Vec3f(forward));
	}

	@LuaMadeCallable
	public void startMovement(Vec3f direction) {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer() || direction == null) return;
		Vector3f moveDirection = new Vector3f(direction.getX(), direction.getY(), direction.getZ());
		if(moveDirection.lengthSquared() == 0) return;
		moveDirection.normalize();
		try {
			Ship ship = (Ship) segmentController;
			if(ship.getAiConfiguration() == null) return;
			ShipAIEntity aiEntity = ship.getAiConfiguration().getAiEntityState();
			if(aiEntity == null) return;
			aiEntity.moveTo(GameServer.getServerState().getController().getTimer(), moveDirection, true);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	@LuaMadeCallable
	public void stopMovement() {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		try {
			Ship ship = (Ship) segmentController;
			if(ship.getAiConfiguration() == null) return;
			ShipAIEntity aiEntity = ship.getAiConfiguration().getAiEntityState();
			if(aiEntity == null) return;
			aiEntity.stop();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	private ShipManagerContainer getContainer() {
		try {
			return ((Ship) segmentController).getManagerContainer();
		} catch(Exception exception) {
			return null;
		}
	}
}
