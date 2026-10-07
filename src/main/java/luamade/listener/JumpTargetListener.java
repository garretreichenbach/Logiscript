package luamade.listener;

import api.event.EventResult;
import api.event.entity.ShipJumpEngageListener;
import luamade.LuaMade;
import luamade.manager.JumpScriptTargetManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;

/**
 * Hooks into {@link ShipJumpEngageListener} to redirect FTL jumps when a Lua script
 * has set a target sector via {@link luamade.lua.element.system.module.JumpDrive#setTarget}.
 *
 * <p>The target is consumed on the first jump after it is set, so one call to
 * {@code setTarget} affects exactly one jump.
 */
public class JumpTargetListener {

	public static void register(LuaMade instance) {
		ShipJumpEngageListener.TYPE.register((context, isServer) -> {
			SegmentController controller = context.getController();
			if(controller == null) return EventResult.CONTINUE;

			Vector3i target = JumpScriptTargetManager.consumeTarget(controller);
			if(target != null) context.setNewSector(target);
			return EventResult.CONTINUE;
		}, instance);
	}
}
