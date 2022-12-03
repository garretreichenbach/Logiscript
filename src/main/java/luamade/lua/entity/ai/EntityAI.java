package luamade.lua.entity.ai;

import api.common.GameCommon;
import luamade.lua.entity.RemoteEntity;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.SimpleGameObject;
import org.schema.game.server.ai.program.common.TargetProgram;
import org.schema.schine.network.objects.Sendable;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class EntityAI {

	private final SegmentController segmentController;

	public EntityAI(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public void moveToSector(int[] sector) {
		if(segmentController instanceof Ship) {
			try {
				((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).setSectorTarget(new Vector3i(sector[0], sector[1], sector[2]));
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	public int[] getTargetSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		if(segmentController instanceof Ship) {
			try {
				sector = ((TargetProgram<?>) (((Ship) segmentController).getAiConfiguration().getAiEntityState().getCurrentProgram())).getSectorTarget();
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
		return new int[] {sector.x, sector.y, sector.z};
	}

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
}
