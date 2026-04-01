package luamade.lua.entity;

import api.utils.game.SegmentControllerUtils;
import luamade.lua.data.BoundingBox;
import luamade.lua.data.Vec3f;
import luamade.lua.data.Vec3i;
import luamade.lua.element.system.shield.ShieldSystem;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;

import javax.vecmath.Vector3f;

/**
 * Limited version of Entity class to prevent access to methods that could lead to abuse.
 * <p>Good for accessing some details of entities without being able to modify them, such as nearby entities.</p>
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
	public Vec3f getHeading() {
		Vector3f forward = new Vector3f(segmentController.getWorldTransform().basis.m02, segmentController.getWorldTransform().basis.m12, segmentController.getWorldTransform().basis.m22);
		if(forward.lengthSquared() == 0) return new Vec3f(0, 0, 0);
		forward.normalize();
		return new Vec3f(forward);
	}

	@LuaMadeCallable
	public Float getMass() {
		return segmentController.getMass();
	}

	@LuaMadeCallable
	public Vec3f getPos() {
		return new Vec3f(segmentController.getWorldTransform().origin);
	}

	@LuaMadeCallable
	public BoundingBox getBoundingBox() {
		return new BoundingBox(segmentController.getBoundingBox());
	}

	@LuaMadeCallable
	public Vec3i getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new Vec3i(sector.x, sector.y, sector.z);
	}

	@LuaMadeCallable
	public Vec3i getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new Vec3i(system.x, system.y, system.z);
	}

	@LuaMadeCallable
	public ShieldSystem getShieldSystem() {
		return new ShieldSystem(segmentController);
	}

	@LuaMadeCallable
	public String getEntityType() {
		return segmentController.getTypeString();
	}

	@LuaMadeCallable
	public String getPilot() {
		if(segmentController instanceof Ship && segmentController.isConrolledByActivePlayer())
			return SegmentControllerUtils.getAttachedPlayers(segmentController).get(0).getName();
		return null;
	}

	public SegmentController getSegmentController() {
		return segmentController;
	}
}
