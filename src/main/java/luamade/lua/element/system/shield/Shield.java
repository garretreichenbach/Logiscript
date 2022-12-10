package luamade.lua.element.system.shield;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.elements.ShieldLocal;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Shield extends LuaMadeUserdata {

	private final ShieldLocal shieldLocal;

	public Shield(ShieldLocal shieldLocal) {
		this.shieldLocal = shieldLocal;
	}

	@LuaMadeCallable
	public Double getCurrent() {
		return shieldLocal.getShields();
	}

	@LuaMadeCallable
	public Double getCapacity() {
		return shieldLocal.getShieldCapacity();
	}

	@LuaMadeCallable
	public Double getRegen() {
		return shieldLocal.getRechargeRate();
	}

	@LuaMadeCallable
	public Boolean isActive() {
		return shieldLocal.active;
	}
}
