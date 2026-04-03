package luamade.listener;

import api.listener.Listener;
import api.listener.events.entity.ShipJumpEngageEvent;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.manager.JumpScriptTargetManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;

/**
 * Hooks into {@link ShipJumpEngageEvent} to redirect FTL jumps when a Lua script
 * has set a target sector via {@link luamade.lua.element.system.module.JumpDrive#setTarget}.
 *
 * <p>The target is consumed on the first jump after it is set, so one call to
 * {@code setTarget} affects exactly one jump.
 */
public class JumpTargetListener {

	public static void register(LuaMade instance) {
		StarLoader.registerListener(ShipJumpEngageEvent.class, new Listener<ShipJumpEngageEvent>() {
			@Override
			public void onEvent(ShipJumpEngageEvent event) {
				SegmentController controller = event.getController();
				if(controller == null) return;

				Vector3i target = JumpScriptTargetManager.consumeTarget(controller);
				if(target == null) return;

				event.setNewSector(target);
			}
		}, instance);
	}
}
