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
	public Vec3f getUp() {
		Vector3f up = new Vector3f(segmentController.getWorldTransform().basis.m01, segmentController.getWorldTransform().basis.m11, segmentController.getWorldTransform().basis.m21);
		if(up.lengthSquared() == 0) return new Vec3f(0, 1, 0);
		up.normalize();
		return new Vec3f(up);
	}

	@LuaMadeCallable
	public Float getRoll() {
		javax.vecmath.Matrix3f basis = segmentController.getWorldTransform().basis;
		Vector3f forward = new Vector3f(basis.m02, basis.m12, basis.m22);
		Vector3f shipUp = new Vector3f(basis.m01, basis.m11, basis.m21);
		if(forward.lengthSquared() == 0 || shipUp.lengthSquared() == 0) return 0f;
		forward.normalize();
		shipUp.normalize();
		float proj = forward.y;
		Vector3f refUp = new Vector3f(-proj * forward.x, 1f - proj * forward.y, -proj * forward.z);
		float refLen = refUp.length();
		if(refLen < 0.001f) return 0f;
		refUp.scale(1f / refLen);
		Vector3f cross = new Vector3f();
		cross.cross(refUp, shipUp);
		return (float) Math.atan2(cross.dot(forward), refUp.dot(shipUp));
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
