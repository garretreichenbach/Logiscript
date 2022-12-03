package luamade.lua.entity;

import luamade.lua.Faction;
import org.schema.game.common.controller.SegmentController;

/**
 * Limited version of Entity class to prevent access to methods that could lead to abuse.
 * <p>Good for accessing some details of entities without being able to modify them, such as nearby entities.</p>
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class RemoteEntity {

	private final SegmentController segmentController;

	public RemoteEntity(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public int getId() {
		return segmentController.getId();
	}

	public String getName() {
		return segmentController.getRealName();
	}

	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}
}
