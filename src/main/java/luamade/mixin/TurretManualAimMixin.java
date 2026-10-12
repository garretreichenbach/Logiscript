package luamade.mixin;

import luamade.system.ManualTurretControl;
import org.schema.common.util.linAlg.Quat4fTools;
import org.schema.game.common.controller.Ship;
import org.schema.game.network.objects.NetworkShip;
import org.schema.game.server.ai.ShipAIEntity;
import org.schema.game.server.ai.program.turret.TurretShipAIEntity;
import org.schema.schine.graphicsengine.core.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.vecmath.Vector3f;

/**
 * Script free-aim for turrets. While {@link ManualTurretControl} holds an aim for this turret, its AI tick is replaced
 * with what vanilla's own MANUAL turret mode does for a gunner: point {@code orientateDir} along a world direction,
 * orientate toward it, and (when firing) set a target point along that line and call {@code doShooting}. Otherwise the
 * vanilla AI runs untouched.
 */
@Mixin(TurretShipAIEntity.class)
public abstract class TurretManualAimMixin extends ShipAIEntity {

	@Shadow
	public Vector3f orientateDir;

	private TurretManualAimMixin() {
		super(null, null);
	}

	@Inject(method = "updateAIServer", at = @At("HEAD"), cancellable = true)
	private void luamade$scriptAim(Timer timer, CallbackInfo ci) {
		Ship turret = getEntity();
		ManualTurretControl.Aim aim = turret == null ? null : ManualTurretControl.active(turret.getId());
		if(aim == null) {
			return;
		}
		ci.cancel();
		NetworkShip network = turret.getNetworkObject();
		network.moveDir.set(new Vector3f());
		orientateDir.set(aim.direction);
		network.orientationDir.set(orientateDir, 0);
		orientate(timer, Quat4fTools.getNewQuat(orientateDir.x, orientateDir.y, orientateDir.z, 0));
		if(aim.firing) {
			Vector3f target = new Vector3f();
			target.scaleAdd(Math.max(200f, getShootingRange()), orientateDir, turret.getWorldTransform().origin);
			network.targetPosition.set(target);
			network.targetVelocity.set(new Vector3f());
			network.targetId.set(-1);
			network.targetType.set((byte) 0);
			doShooting(unit, timer);
		}
	}
}
