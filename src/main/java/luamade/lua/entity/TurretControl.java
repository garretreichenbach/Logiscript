package luamade.lua.entity;

import luamade.lua.data.Vec3f;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.ManualTurretControl;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.Ship;

import javax.vecmath.Vector3f;

/**
 * Script free-aim for one turret, from {@code console.getTurret(entity)}. Aiming takes the turret off its AI targeting
 * (see {@link luamade.mixin.TurretManualAimMixin}); {@link #release()}, or {@link ManualTurretControl#TIMEOUT_MS} without
 * any call, gives it back. Directions and points are world coordinates, like {@code Entity.getPos()}.
 */
@LuaMadeClass("TurretControl")
public class TurretControl extends LuaMadeUserdata {

	private final Ship turret;

	public TurretControl(Ship turret) {
		this.turret = turret;
	}

	/** Points the turret along a world-space direction. */
	@LuaMadeCallable
	public void aim(Vec3f direction) {
		Vector3f dir = new Vector3f(direction.getX(), direction.getY(), direction.getZ());
		if(dir.lengthSquared() < 1.0e-6f) {
			throw new LuaError("Aim direction must not be zero");
		}
		dir.normalize();
		take().direction.set(dir);
	}

	/** Points the turret at a world-space point. */
	@LuaMadeCallable
	public void aimAt(Vec3f point) {
		Vector3f origin = turret.getWorldTransform().origin;
		aim(new Vec3f(point.getX() - origin.x, point.getY() - origin.y, point.getZ() - origin.z));
	}

	/** Holds or stops the trigger; the turret fires its weapons along its aim, at their normal rate. */
	@LuaMadeCallable
	public void fire(Boolean firing) {
		take().firing = firing != null && firing;
	}

	/** Hands the turret back to its AI. */
	@LuaMadeCallable
	public void release() {
		ManualTurretControl.release(turret.getId());
	}

	@LuaMadeCallable
	public Boolean isManual() {
		return ManualTurretControl.active(turret.getId()) != null;
	}

	/** Current world-space facing of the turret (its forward axis). */
	@LuaMadeCallable
	public Vec3f getFacing() {
		Vector3f forward = new Vector3f(0, 0, 1);
		turret.getWorldTransform().basis.transform(forward);
		return new Vec3f(forward);
	}

	@LuaMadeCallable
	public Entity getEntity() {
		return Entity.wrap(turret);
	}

	private ManualTurretControl.Aim take() {
		if(!turret.isAIControlled()) {
			// the mixin rides the AI tick, so the turret's AI has to be running
			turret.activateAI(true, true);
		}
		return ManualTurretControl.take(turret.getId());
	}
}
