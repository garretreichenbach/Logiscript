package luamade.lua.entity;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.Faction;
import luamade.lua.LuaVec3i;
import luamade.lua.element.system.shield.ShieldSystem;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
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
	public Integer getId() {
		return segmentController.getId();
	}

	@LuaMadeCallable
	public String getName() {
		return segmentController.getRealName();
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

	@LuaMadeCallable
	public Float getSpeed() {
		return segmentController.getSpeedCurrent();
	}

	@LuaMadeCallable
	public Float getMass() {
		return segmentController.getMass();
	}

	@LuaMadeCallable
	public LuaVec3i getPos() {
		Transform transform = segmentController.getWorldTransform();
		Vector3i pos = new Vector3i(transform.origin);
		return new LuaVec3i(pos.x, pos.y, pos.z);
	}

	@LuaMadeCallable
	public LuaVec3i getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new LuaVec3i(sector.x, sector.y, sector.z);
	}

	@LuaMadeCallable
	public LuaVec3i getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new LuaVec3i(system.x, system.y, system.z);
	}

	@LuaMadeCallable
	public ShieldSystem getShieldSystem() {
		return new ShieldSystem(segmentController);
	}

	@LuaMadeCallable
	public String getEntityType() {
		return segmentController.getTypeString();
	}
}