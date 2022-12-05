package luamade.lua.entity;

import luamade.lua.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
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

	@LuaMadeCallable
	public LuaInteger getId() {
		return LuaInteger.valueOf(segmentController.getId());
	}

	@LuaMadeCallable
	public LuaString getName() {
		return LuaString.valueOf(segmentController.getRealName());
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

	@LuaMadeCallable
	public LuaDouble getSpeed() {
		return (LuaDouble) LuaDouble.valueOf(segmentController.getSpeedCurrent());
	}
}