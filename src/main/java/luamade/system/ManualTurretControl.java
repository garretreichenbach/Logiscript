package luamade.system;

import javax.vecmath.Vector3f;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Turrets currently aimed by scripts instead of their AI, keyed by turret entity id. Read every server tick by
 * {@link luamade.mixin.TurretManualAimMixin}. Control lapses if the script stops touching it for {@link #TIMEOUT_MS},
 * so a crashed or stopped script hands the turret back to its AI.
 */
public final class ManualTurretControl {

	public static final long TIMEOUT_MS = 3000;

	public static final class Aim {
		/** World-space aim direction, normalized. */
		public final Vector3f direction = new Vector3f(0, 0, 1);
		public volatile boolean firing;
		volatile long touchedAt = System.currentTimeMillis();
	}

	private static final ConcurrentHashMap<Integer, Aim> aims = new ConcurrentHashMap<>();

	private ManualTurretControl() {
	}

	/** Starts or refreshes manual control of a turret and returns its aim state. */
	public static Aim take(int turretId) {
		Aim aim = aims.computeIfAbsent(turretId, id -> new Aim());
		aim.touchedAt = System.currentTimeMillis();
		return aim;
	}

	public static void release(int turretId) {
		aims.remove(turretId);
	}

	/** The live aim for a turret, or null when it's under AI control (never taken, released, or timed out). */
	public static Aim active(int turretId) {
		Aim aim = aims.get(turretId);
		if(aim != null && System.currentTimeMillis() - aim.touchedAt > TIMEOUT_MS) {
			aims.remove(turretId, aim);
			return null;
		}
		return aim;
	}
}
