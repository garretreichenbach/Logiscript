package luamade.lua.entity;

import luamade.lua.Faction;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;

/**
 * Limited version of Entity class to prevent access to methods that could lead to abuse.
 * <p>Good for accessing some details of entities without being able to modify them, such as nearby entities.</p>
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class RemoteEntity extends LuaMadeUserdata {

	private final SegmentController segmentController;

	public RemoteEntity(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaCallable
	public Integer getId() {
		return segmentController.getId();
	}

	@LuaCallable
	public String getName() {
		return segmentController.getRealName();
	}

	@LuaCallable
	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

	@LuaCallable
	public Float getSpeed() {
		return segmentController.getSpeedCurrent();
	}
}