package luamade.lua.entity.ai;

import api.common.GameCommon;
import api.common.GameServer;
import luamade.lua.data.LuaVec3i;
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
	public void moveToSector(LuaVec3i sector) {
		if(segmentController instanceof Ship) {
			try {
				((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).setSectorTarget(new Vector3i(sector.getX(), sector.getY(), sector.getZ()));
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	@LuaMadeCallable
	public LuaVec3i getTargetSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		if(segmentController instanceof Ship) {
			try {
				sector = ((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).getSectorTarget();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return new LuaVec3i(sector.x, sector.y, sector.z);
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
	public void moveToPos(LuaVec3i pos) {
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
				moveToPos(new LuaVec3i((int) position.x, (int) position.y, (int) position.z));
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
