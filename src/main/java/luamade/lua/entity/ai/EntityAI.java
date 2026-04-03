package luamade.lua.entity.ai;

import api.common.GameCommon;
import api.common.GameServer;
import luamade.lua.data.Vec3f;
import luamade.lua.data.Vec3i;
import luamade.lua.entity.RemoteEntity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.common.util.linAlg.Quat4fTools;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.ai.AIGameConfiguration;
import org.schema.game.common.controller.ai.AIConfiguationElements;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.data.SimpleGameObject;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.common.TargetProgram;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import org.schema.schine.ai.stateMachines.AiInterface;
import org.schema.schine.graphicsengine.core.settings.StateParameterNotFoundException;
import org.schema.schine.graphicsengine.core.settings.states.StaticStates;
import org.schema.schine.graphicsengine.core.settings.states.States;
import org.schema.schine.graphicsengine.forms.BoundingBox;
import org.schema.schine.network.objects.Sendable;

import javax.vecmath.Vector3f;

public class EntityAI extends LuaMadeUserdata {

	private final SegmentController segmentController;

	public EntityAI(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public void setActive(Boolean active) {
		if(segmentController instanceof ManagedUsableSegmentController) ((ManagedUsableSegmentController<?>) segmentController).activateAI(active, true);
	}

	@LuaMadeCallable
	public Boolean isActive() {
		return segmentController.isAIControlled();
	}

	@LuaMadeCallable
	public void moveToSector(Vec3i sector) {
		if(segmentController instanceof Ship) {
			try {
				((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).setSectorTarget(new Vector3i(sector.getX(), sector.getY(), sector.getZ()));
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@LuaMadeCallable
	public Vec3i getTargetSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		if(segmentController instanceof Ship) {
			try {
				sector = ((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).getSectorTarget();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return new Vec3i(sector.x, sector.y, sector.z);
	}

	@LuaMadeCallable
	public void setTarget(RemoteEntity entity) {
		int id = entity.getId();
		Sendable sendable = GameCommon.getGameObject(id);
		if(sendable instanceof SegmentController) {
			SegmentController target = (SegmentController) sendable;
			Vector3i sector = target.getSector(new Vector3i());
			Vector3i thisSector = segmentController.getSector(new Vector3i());
			Vector3i diff = new Vector3i(sector.x - thisSector.x, sector.y - thisSector.y, sector.z - thisSector.z);
			diff.absolute();
			//Check if target is in same or adjacent sector
			if(diff.x <= 1 && diff.y <= 1 && diff.z <= 1) {
				if(segmentController instanceof Ship) {
					try {
						((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).setTarget(target);
					} catch(Exception exception) {
						exception.printStackTrace();
					}
				}
			}
		}
	}

	@LuaMadeCallable
	public RemoteEntity getTarget() {
		SimpleGameObject target = null;
		if(segmentController instanceof Ship) {
			try {
				target = ((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).getTarget();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		if(target instanceof SegmentController) return new RemoteEntity((SegmentController) target);
		else return null;
	}

	@LuaMadeCallable
	public String getTargetType() {
		AIGameConfiguration<?, ?> config = getAiConfiguration();
		if(config != null) {
			try {
				return (String) config.get(Types.AIM_AT).getCurrentState();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return "None";
	}

	@LuaMadeCallable
	public String[] getAvailableTargetTypes() {
		AIGameConfiguration<?, ?> config = getAiConfiguration();
		if(config == null) {
			return new String[0];
		}

		try {
			AIConfiguationElements<?> setting = config.get(Types.AIM_AT);
			if(setting == null) {
				return new String[0];
			}

			States<? extends Object> possibleStates = setting.getPossibleStates();
			if(possibleStates instanceof StaticStates<?>) {
				Object[] states = ((StaticStates<?>) possibleStates).states;
				String[] values = new String[states.length];
				for(int i = 0; i < states.length; i++) {
					values[i] = String.valueOf(states[i]);
				}
				return values;
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return new String[0];
	}

	@LuaMadeCallable
	public void setTargetType(String type) {
		AIGameConfiguration<?, ?> config = getAiConfiguration();
		if(config == null) {
			throw new IllegalStateException("AI target preferences are only available on ships with AI configuration");
		}
		if(type == null || type.trim().isEmpty()) {
			throw new IllegalArgumentException("AI target type is required");
		}

		String canonicalType = resolveTargetType(type);
		try {
			AIConfiguationElements<?> setting = config.get(Types.AIM_AT);
			if("Any".equals(canonicalType)) {
				setting.switchSetting("Any", true);
			} else {
				setting.switchSetting("Any", false);
				setting.switchSetting(canonicalType, true);
			}
		} catch(StateParameterNotFoundException exception) {
			throw new IllegalArgumentException("Unknown AI target type '" + type + "'. Allowed target types: " + getAllowedTargetTypes(), exception);
		} catch(Exception exception) {
			throw new IllegalStateException("Failed to set AI target type to '" + canonicalType + "'", exception);
		}
	}

	private AIGameConfiguration<?, ?> getAiConfiguration() {
		if(segmentController instanceof Ship) {
			try {
				return (AIGameConfiguration<?, ?>) ((AiInterface) segmentController).getAiConfiguration();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return null;
	}

	private String resolveTargetType(String requestedType) {
		String trimmed = requestedType.trim();
		for(String availableType : getAvailableTargetTypes()) {
			if(availableType.equalsIgnoreCase(trimmed)) {
				return availableType;
			}
		}
		throw new IllegalArgumentException("Unknown AI target type '" + requestedType + "'. Allowed target types: " + getAllowedTargetTypes());
	}

	private String getAllowedTargetTypes() {
		String[] availableTypes = getAvailableTargetTypes();
		if(availableTypes.length == 0) {
			return "none";
		}

		StringBuilder builder = new StringBuilder();
		for(String availableType : availableTypes) {
			if(builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(availableType);
		}
		return builder.toString();
	}

	@LuaMadeCallable
	public void moveToPos(Vec3i pos) {
		if(segmentController instanceof Ship && segmentController.isOnServer()) {
			try {
				ShipAIEntity aiEntity = ((Ship) segmentController).getAiConfiguration().getAiEntityState();
				Vector3f position = new Vector3f(pos.getX(), pos.getY(), pos.getZ());
				//If position is already within 15 blocks of target, don't move
				Vector3f currentPos = new Vector3f(segmentController.getWorldTransform().origin);
				//if(Vector3fTools.distance(currentPos.x, currentPos.y, currentPos.z, position.x, position.y, position.z) > 15) {
					Vector3f direction = new Vector3f();
					direction.sub(position, currentPos);
					direction.normalize();
					aiEntity.moveTo(GameServer.getServerState().getController().getTimer(), direction, true);
				//} else aiEntity.stop();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@LuaMadeCallable
	public void moveToEntity(RemoteEntity entity) {
		if(entity.getSegmentController().getSectorId() != segmentController.getSectorId()) return;
		if(segmentController instanceof Ship && segmentController.isOnServer()) {
			try {
				//ShipAIEntity aiEntity = ((Ship) segmentController).getAiConfiguration().getAiEntityState();
				Vector3f position = new Vector3f(entity.getSegmentController().getWorldTransform().origin);
				moveToPos(new Vec3i((int) position.x, (int) position.y, (int) position.z));
				/*
				Vector3f moveToPos = calculateMoveToPos(segmentController, entity.getSegmentController());
				//If position is already within 15 blocks of target, don't move
				Vector3f currentPos = new Vector3f(segmentController.getWorldTransform().origin);
				if(Vector3fTools.distance(currentPos.x, currentPos.y, currentPos.z, position.x, position.y, position.z) > 15) {
					Vector3f direction = new Vector3f();
					direction.sub(moveToPos, currentPos);
					direction.normalize();
					aiEntity.moveTo(GameServer.getServerState().getController().getTimer(), direction, true);
				} else aiEntity.stop();
				 */
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@LuaMadeCallable
	public void stop() {
		if(segmentController instanceof Ship && segmentController.isOnServer()) {
			try {
				((Ship) segmentController).getAiConfiguration().getAiEntityState().stop();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@LuaMadeCallable
	public void navigateToPos(Vec3i pos, Integer stopRadius) {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		Vector3f target = new Vector3f(pos.getX(), pos.getY(), pos.getZ());
		Vector3f currentPos = new Vector3f(segmentController.getWorldTransform().origin);
		float dx = target.x - currentPos.x;
		float dy = target.y - currentPos.y;
		float dz = target.z - currentPos.z;
		float distSq = dx * dx + dy * dy + dz * dz;
		float threshold = Math.max(1, stopRadius);
		if(distSq <= threshold * threshold) {
			stop();
			return;
		}
		moveToPos(pos);
	}

	@LuaMadeCallable
	public Boolean hasReachedPos(Vec3i pos, Integer radius) {
		Vector3f target = new Vector3f(pos.getX(), pos.getY(), pos.getZ());
		Vector3f currentPos = new Vector3f(segmentController.getWorldTransform().origin);
		float dx = target.x - currentPos.x;
		float dy = target.y - currentPos.y;
		float dz = target.z - currentPos.z;
		float distSq = dx * dx + dy * dy + dz * dz;
		float threshold = Math.max(1, radius);
		return distSq <= threshold * threshold;
	}

	@LuaMadeCallable
	public void stopNavigation() {
		stop();
	}

	@LuaMadeCallable
	public Vec3f getHeading() {
		Vector3f forward = new Vector3f(
			segmentController.getWorldTransform().basis.m02,
			segmentController.getWorldTransform().basis.m12,
			segmentController.getWorldTransform().basis.m22
		);
		if(forward.lengthSquared() == 0) return new Vec3f(0, 0, 0);
		forward.normalize();
		return new Vec3f(forward);
	}

	@LuaMadeCallable
	public Boolean isAlignedWith(Vec3f direction, Float threshold) {
		Vector3f dir = new Vector3f(direction.getX(), direction.getY(), direction.getZ());
		if(dir.lengthSquared() == 0) return false;
		dir.normalize();
		Vector3f forward = new Vector3f(
			segmentController.getWorldTransform().basis.m02,
			segmentController.getWorldTransform().basis.m12,
			segmentController.getWorldTransform().basis.m22
		);
		forward.normalize();
		return forward.dot(dir) >= threshold;
	}

	@LuaMadeCallable
	public Boolean isFacingTowards(RemoteEntity entity, Float threshold) {
		Vector3f toTarget = new Vector3f(entity.getSegmentController().getWorldTransform().origin);
		toTarget.sub(segmentController.getWorldTransform().origin);
		if(toTarget.lengthSquared() == 0) return true;
		toTarget.normalize();
		Vector3f forward = new Vector3f(
			segmentController.getWorldTransform().basis.m02,
			segmentController.getWorldTransform().basis.m12,
			segmentController.getWorldTransform().basis.m22
		);
		forward.normalize();
		return forward.dot(toTarget) >= threshold;
	}

	@LuaMadeCallable
	public void faceDirection(Vec3f dir) {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		try {
			Vector3f direction = new Vector3f(dir.getX(), dir.getY(), dir.getZ());
			if(direction.lengthSquared() == 0) return;
			direction.normalize();
			ShipAIEntity aiEntity = ((Ship) segmentController).getAiConfiguration().getAiEntityState();
			aiEntity.moveTo(GameServer.getServerState().getController().getTimer(), direction, true);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	@LuaMadeCallable
	public void faceTowards(RemoteEntity entity) {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		Vector3f toTarget = new Vector3f(entity.getSegmentController().getWorldTransform().origin);
		toTarget.sub(segmentController.getWorldTransform().origin);
		if(toTarget.lengthSquared() == 0) return;
		toTarget.normalize();
		Vec3f dir = new Vec3f(toTarget);
		faceDirection(dir);
	}

	@LuaMadeCallable
	public Vec3f getUp() {
		Vector3f up = new Vector3f(
			segmentController.getWorldTransform().basis.m01,
			segmentController.getWorldTransform().basis.m11,
			segmentController.getWorldTransform().basis.m21
		);
		if(up.lengthSquared() == 0) return new Vec3f(0, 1, 0);
		up.normalize();
		return new Vec3f(up);
	}

	@LuaMadeCallable
	public Float getRoll() {
		Matrix3f basis = segmentController.getWorldTransform().basis;
		Vector3f forward = new Vector3f(basis.m02, basis.m12, basis.m22);
		Vector3f shipUp = new Vector3f(basis.m01, basis.m11, basis.m21);
		if(forward.lengthSquared() == 0 || shipUp.lengthSquared() == 0) return 0f;
		forward.normalize();
		shipUp.normalize();
		// Project galactic up (0,1,0) onto the plane perpendicular to the ship's forward
		float proj = forward.y;
		Vector3f refUp = new Vector3f(-proj * forward.x, 1f - proj * forward.y, -proj * forward.z);
		float refLen = refUp.length();
		if(refLen < 0.001f) return 0f; // ship pointing straight up/down: roll undefined
		refUp.scale(1f / refLen);
		// Signed angle from the galactic-up reference to the ship's up, around the forward axis
		Vector3f cross = new Vector3f();
		cross.cross(refUp, shipUp);
		return (float) Math.atan2(cross.dot(forward), refUp.dot(shipUp));
	}

	@LuaMadeCallable
	public Boolean isRollAligned(Float threshold) {
		Matrix3f basis = segmentController.getWorldTransform().basis;
		Vector3f shipUp = new Vector3f(basis.m01, basis.m11, basis.m21);
		if(shipUp.lengthSquared() == 0) return false;
		shipUp.normalize();
		// Galactic up is (0,1,0); dot product with it equals the Y component
		return shipUp.y >= threshold;
	}

	/**
	 * Commands the ship to face the given direction while holding the given up vector, giving
	 * full roll control.  The ship will also thrust in the forward direction.
	 */
	@LuaMadeCallable
	public void faceWithRoll(Vec3f forwardDir, Vec3f upDir) {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		try {
			Vector3f forward = new Vector3f(forwardDir.getX(), forwardDir.getY(), forwardDir.getZ());
			Vector3f desiredUp = new Vector3f(upDir.getX(), upDir.getY(), upDir.getZ());
			if(forward.lengthSquared() == 0) return;
			forward.normalize();
			ShipAIEntity aiEntity = ((Ship) segmentController).getAiConfiguration().getAiEntityState();
			Quat4f q = buildOrientationQuat(forward, desiredUp);
			// Thrust in the forward direction without letting moveTo override the orientation
			aiEntity.moveTo(GameServer.getServerState().getController().getTimer(), forward, false);
			aiEntity.orientate(GameServer.getServerState().getController().getTimer(), q);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	/**
	 * Corrects the ship's roll so its up vector aligns with galactic up (0, 1, 0) while
	 * preserving its current heading.  Does not apply thrust.
	 */
	@LuaMadeCallable
	public void alignRollToGalacticUp() {
		if(!(segmentController instanceof Ship) || !segmentController.isOnServer()) return;
		try {
			Matrix3f basis = segmentController.getWorldTransform().basis;
			Vector3f forward = new Vector3f(basis.m02, basis.m12, basis.m22);
			if(forward.lengthSquared() == 0) return;
			forward.normalize();
			Quat4f q = buildOrientationQuat(forward, new Vector3f(0, 1, 0));
			ShipAIEntity aiEntity = ((Ship) segmentController).getAiConfiguration().getAiEntityState();
			aiEntity.orientate(GameServer.getServerState().getController().getTimer(), q);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	/**
	 * Builds a full-orientation quaternion from a desired forward and an approximate up vector.
	 * Gram-Schmidt ensures the axes are orthonormal; the resulting quaternion has w != 0 so
	 * ShipAIEntity.orientate() treats it as a full orientation rather than forward-only.
	 */
	private static Quat4f buildOrientationQuat(Vector3f forward, Vector3f desiredUp) {
		Vector3f right = new Vector3f();
		right.cross(desiredUp, forward);
		if(right.lengthSquared() < 1e-6f) {
			// Desired up is (nearly) parallel to forward — pick an arbitrary perpendicular
			right.set(forward.y, -forward.x, 0);
			if(right.lengthSquared() < 1e-6f) {
				right.set(0, forward.z, -forward.y);
			}
		}
		right.normalize();
		Vector3f up = new Vector3f();
		up.cross(forward, right);
		up.normalize();

		// Build rotation matrix: col0=right, col1=up, col2=forward
		Matrix3f m = new Matrix3f();
		m.m00 = right.x;   m.m10 = right.y;   m.m20 = right.z;
		m.m01 = up.x;      m.m11 = up.y;       m.m21 = up.z;
		m.m02 = forward.x; m.m12 = forward.y;  m.m22 = forward.z;

		Quat4f q = new Quat4f();
		Quat4fTools.set(m, q);
		// If w == 0, the engine treats the quaternion as forward-only; nudge it
		if(q.w == 0) q.w = Float.MIN_VALUE;
		return q;
	}

	private static Vector3f calculateMoveToPos(SegmentController segmentController, SegmentController target) {
		Vector3f position = new Vector3f(target.getWorldTransform().origin);
		BoundingBox boundingBox = segmentController.getBoundingBox();
		BoundingBox targetBoundingBox = new BoundingBox(target.getBoundingBox());
		//Move bounds out by 5 blocks just to give a clearance area
		boundingBox.min.x -= 5;
		boundingBox.min.y -= 5;
		boundingBox.min.z -= 5;
		boundingBox.max.x += 5;
		boundingBox.max.y += 5;
		boundingBox.max.z += 5;
		//Move position to area in front of box but not clipping into it
		position.x += (targetBoundingBox.max.x - targetBoundingBox.min.x) / 2 + (boundingBox.max.x - boundingBox.min.x) / 2;
		position.y += (targetBoundingBox.max.y - targetBoundingBox.min.y) / 2 + (boundingBox.max.y - boundingBox.min.y) / 2;
		position.z += (targetBoundingBox.max.z - targetBoundingBox.min.z) / 2 + (boundingBox.max.z - boundingBox.min.z) / 2;
		return position;
	}
}
