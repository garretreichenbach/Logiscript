package luamade.lua.element.system.shield;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.elements.ShieldLocal;

import java.util.Collection;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ShieldSystem extends LuaMadeUserdata {

	private final SegmentController segmentController;

	public ShieldSystem(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public Boolean isShielded() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			return ship.getManagerContainer().getShieldCapacityManager().getTotalSize() > 0 && ship.getManagerContainer().getShieldRegenManager().getTotalSize() > 0;
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			return spaceStation.getManagerContainer().getShieldCapacityManager().getTotalSize() > 0 && spaceStation.getManagerContainer().getShieldRegenManager().getTotalSize() > 0;
		} else return false;
	}

	@LuaMadeCallable
	public Double getCurrent() {
		if(!isShielded()) return 0.0;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			return ship.getManagerContainer().getShieldCapacityManager().getShieldAddOn().getShields();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			return spaceStation.getManagerContainer().getShieldCapacityManager().getShieldAddOn().getShields();
		} else return 0.0;
	}

	@LuaMadeCallable
	public Double getCapacity() {
		if(!isShielded()) return 0.0;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			return ship.getManagerContainer().getShieldCapacityManager().getShieldAddOn().getShieldCapacity();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			return spaceStation.getManagerContainer().getShieldCapacityManager().getShieldAddOn().getShieldCapacity();
		} else return 0.0;
	}

	@LuaMadeCallable
	public Double getRegen() {
		if(!isShielded()) return 0.0;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			return ship.getManagerContainer().getShieldRegenManager().getShieldAddOn().getShieldRechargeRate();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			return spaceStation.getManagerContainer().getShieldRegenManager().getShieldAddOn().getShieldRechargeRate();
		} else return 0.0;
	}

	@LuaMadeCallable
	public Shield[] getAllShields() {
		if(!isShielded()) return new Shield[0];
		Collection<ShieldLocal> shields;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			shields = ship.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			shields = spaceStation.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else return new Shield[0];
		Shield[] shieldArray = new Shield[shields.size()];
		int i = 0;
		for(ShieldLocal shield : shields) {
			shieldArray[i] = new Shield(shield);
			i++;
		}
		return shieldArray;
	}

	@LuaMadeCallable
	public Shield[] getActiveShields() {
		if(!isShielded()) return null;
		Collection<ShieldLocal> shields;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			shields = ship.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			shields = spaceStation.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else return new Shield[0];
		Shield[] shieldArray = new Shield[shields.size()];
		int i = 0;
		for(ShieldLocal shield : shields) {
			if(shield.active) {
				shieldArray[i] = new Shield(shield);
				i++;
			}
		}
		return shieldArray;
	}

	@LuaMadeCallable
	public Boolean isShieldActive(Integer index) {
		if(!isShielded()) return false;
		Collection<ShieldLocal> shields;
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			shields = ship.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else if(segmentController instanceof SpaceStation) {
			SpaceStation spaceStation = (SpaceStation) segmentController;
			shields = spaceStation.getManagerContainer().getShieldAddOn().getShieldLocalAddOn().getAllShields();
		} else return false;
		return shields != null && shields.size() > index && shields.toArray(new ShieldLocal[0])[index].active;
	}

	private Ship getShip() {
		if(segmentController instanceof Ship) return (Ship) segmentController;
		else return null;
	}

	private SpaceStation getSpaceStation() {
		if(segmentController instanceof SpaceStation) return (SpaceStation) segmentController;
		else return null;
	}
}
