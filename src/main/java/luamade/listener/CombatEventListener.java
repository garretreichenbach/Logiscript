package luamade.listener;

import api.listener.Listener;
import api.listener.events.systems.ShieldHitEvent;
import api.listener.fastevents.FastListenerCommon;
import api.listener.fastevents.segmentpiece.SegmentPieceDamageListener;
import api.mod.StarLoader;
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
		FastListenerCommon.segmentPieceDamageListeners.add(new SegmentPieceDamageListener() {
			@Override
			public int onBlockDamage(SegmentController controller, long pos, short type, int damage, DamageDealerType damageType, Damager from, boolean isServer) {
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
			}
		});

		StarLoader.registerListener(ShieldHitEvent.class, new Listener<ShieldHitEvent>() {
			@Override
			public void onEvent(ShieldHitEvent event) {
				try {
					SegmentController controller = event.getHitController();
					if(controller == null) return;

					LuaTable luaEvent = new LuaTable();
					luaEvent.set("type", "shield_hit");
					luaEvent.set("damageType", event.getDamageType() != null ? event.getDamageType().name() : "GENERAL");
					luaEvent.set("isHighDamage", LuaValue.valueOf(event.isHighDamage()));
					luaEvent.set("isLowDamage", LuaValue.valueOf(event.isLowDamage()));
					luaEvent.set("isServer", LuaValue.valueOf(event.isServer()));
					dispatchToComputers(controller, luaEvent);
				} catch(Exception ignored) {
				}
			}
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
