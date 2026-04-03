package luamade.manager;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Stores script-set FTL jump targets per entity.
 *
 * <p>Targets are keyed by {@link SegmentController} via a {@link WeakHashMap} so
 * entries are automatically collected when entities unload — no explicit cleanup
 * is needed. This is intentionally not persisted across restarts.
 *
 * <p>Targets are consumed (cleared) once a {@link api.listener.events.entity.ShipJumpEngageEvent}
 * is received for that entity, so a single {@code setTarget} call affects only the
 * next jump.
 */
public final class JumpScriptTargetManager {

	private static final Map<SegmentController, Vector3i> targets = new WeakHashMap<>();

	private JumpScriptTargetManager() {
	}

	/**
	 * Registers a script jump destination for {@code entity}. Replaces any
	 * previously set target for the same entity.
	 */
	public static synchronized void setTarget(SegmentController entity, Vector3i sector) {
		targets.put(entity, new Vector3i(sector));
	}

	/**
	 * Removes the script target for {@code entity}.
	 */
	public static synchronized void clearTarget(SegmentController entity) {
		targets.remove(entity);
	}

	/**
	 * Returns the script target for {@code entity}, or {@code null} if none is set.
	 * Does NOT consume the target — call {@link #consumeTarget} to remove it after use.
	 */
	public static synchronized Vector3i getTarget(SegmentController entity) {
		return targets.get(entity);
	}

	/**
	 * Returns and removes the script target for {@code entity}. Returns {@code null}
	 * when no target is set.
	 */
	public static synchronized Vector3i consumeTarget(SegmentController entity) {
		return targets.remove(entity);
	}
}
