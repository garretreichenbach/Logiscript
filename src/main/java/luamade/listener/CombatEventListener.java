package luamade.listener;

import api.event.EventResult;
import api.event.block.SegmentPieceDamageListener;
import api.event.systems.ShieldHitListener;
import luamade.LuaMade;
import luamade.system.module.ComputerModuleContainer;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.damage.DamageDealerType;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.controller.elements.ManagerContainer;

public class CombatEventListener {

	public static void register(LuaMade instance) {
		SegmentPieceDamageListener.TYPE.register((controller, pos, type, damage, damageType, from, isServer) -> {
            try {
                LuaTable luaEvent = new LuaTable();
                luaEvent.set("type", "block_damage");
                luaEvent.set("damageType", damageType != null ? damageType.name() : "GENERAL");
                luaEvent.set("damage", damage);
                luaEvent.set("blockType", type);
                if(from != null) {
                    luaEvent.set("attackerName", from.getName() != null ? from.getName() : "");
                    luaEvent.set("attackerFaction", from.getFactionId());
                }
                luaEvent.set("isServer", LuaValue.valueOf(isServer));
                dispatchToComputers(controller, luaEvent);
            } catch(Exception ignored) {
            }
            return damage;
        }, instance);

		ShieldHitListener.TYPE.register((context, isServer) -> {
			try {
				SegmentController controller = context.getHitController();
				if(controller != null) {
					LuaTable luaEvent = new LuaTable();
					luaEvent.set("type", "shield_hit");
					luaEvent.set("damageType", context.getDamageType() != null ? context.getDamageType().name() : "GENERAL");
					luaEvent.set("isServer", LuaValue.valueOf(isServer));
					dispatchToComputers(controller, luaEvent);
				}
			} catch(Exception ignored) {
			}
			return EventResult.CONTINUE;
		}, instance);
	}

	private static void dispatchToComputers(SegmentController controller, LuaTable event) {
		if(!(controller instanceof ManagedUsableSegmentController)) return;
		ManagerContainer<?> mc = ((ManagedUsableSegmentController<?>) controller).getManagerContainer();
		ComputerModuleContainer container = ComputerModuleContainer.getContainer(mc);
		if(container == null) return;
		container.forEachComputerModule(module -> module.getCombatEventApi().pushEvent(event));
	}
}
