package luamade.lua.entity.ai;

import api.common.GameCommon;
import api.common.GameServer;
import com.bulletphysics.linearmath.Transform;
import luamade.lua.data.Vec3i;
import luamade.lua.entity.RemoteEntity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.ai.AIGameConfiguration;
import org.schema.game.common.controller.ai.Types;
import org.schema.game.common.data.SimpleGameObject;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.schine.ai.stateMachines.AiInterface;
import org.schema.schine.graphicsengine.forms.BoundingBox;
import org.schema.schine.network.objects.Sendable;

import javax.vecmath.Vector3f;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
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
		if(segmentController instanceof Ship) {
			try {
				AIGameConfiguration<?, ?> config = ((AIGameConfiguration<?, ?>) ((AiInterface) segmentController).getAiConfiguration());
				return (String) config.get(Types.AIM_AT).getCurrentState();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return "None";
	}

	@LuaMadeCallable
	public void setTargetType(String type) {
		if(segmentController instanceof Ship) {
			try {
				AIGameConfiguration<?, ?> config = ((AIGameConfiguration<?, ?>) ((AiInterface) segmentController).getAiConfiguration());
				if(type.equals("Any")) config.get(Types.AIM_AT).switchSetting("Any", true);
				else {
					config.get(Types.AIM_AT).switchSetting("Any", false);
					config.get(Types.AIM_AT).switchSetting(type, true);
				}
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
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
	public Vec3i getHeading() {
		Transform t = segmentController.getWorldTransform();
		// Z+ is the forward axis in StarMade local space; read the 3rd column of the basis matrix
		float fx = t.basis.m02, fy = t.basis.m12, fz = t.basis.m22;
		float ax = Math.abs(fx), ay = Math.abs(fy), az = Math.abs(fz);
		if(ax >= ay && ax >= az) return new Vec3i(fx > 0 ? 1 : -1, 0, 0);
		if(ay >= ax && ay >= az) return new Vec3i(0, fy > 0 ? 1 : -1, 0);
		return new Vec3i(0, 0, fz > 0 ? 1 : -1);
	}

	@LuaMadeCallable
	public Boolean isAlignedWith(Vec3i direction, Float threshold) {
		Vector3f dir = new Vector3f(direction.getX(), direction.getY(), direction.getZ());
		if(dir.lengthSquared() == 0) return false;
		dir.normalize();
		Transform t = segmentController.getWorldTransform();
		Vector3f forward = new Vector3f(t.basis.m02, t.basis.m12, t.basis.m22);
		forward.normalize();
		return forward.dot(dir) >= threshold;
	}

	@LuaMadeCallable
	public Boolean isFacingTowards(RemoteEntity entity, Float threshold) {
		Vector3f toTarget = new Vector3f(entity.getSegmentController().getWorldTransform().origin);
		toTarget.sub(segmentController.getWorldTransform().origin);
		if(toTarget.lengthSquared() == 0) return true;
		toTarget.normalize();
		Transform t = segmentController.getWorldTransform();
		Vector3f forward = new Vector3f(t.basis.m02, t.basis.m12, t.basis.m22);
		forward.normalize();
		return forward.dot(toTarget) >= threshold;
	}

	@LuaMadeCallable
	public void faceDirection(Vec3i dir) {
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
		Vec3i dir = new Vec3i(Math.round(toTarget.x), Math.round(toTarget.y), Math.round(toTarget.z));
		faceDirection(dir);
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
