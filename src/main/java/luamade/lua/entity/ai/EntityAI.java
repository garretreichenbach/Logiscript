package luamade.lua.entity.ai;

import api.common.GameCommon;
import luamade.lua.LuaVec3i;
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
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.schine.ai.stateMachines.AiInterface;
import org.schema.schine.network.objects.Sendable;

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
				((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).setSectorTarget(new Vector3i(sector.x(), sector.y(), sector.z()));
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
}
