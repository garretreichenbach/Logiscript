package luamade.lua.entity.combat;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ElementCollectionManager;
import org.schema.game.common.controller.elements.weapon.WeaponCollectionManager;
import org.schema.game.common.controller.elements.weapon.WeaponUnit;
import org.schema.game.common.controller.elements.beam.damageBeam.DamageBeamCollectionManager;
import org.schema.game.common.controller.elements.beam.damageBeam.DamageBeamUnit;
import org.schema.game.common.controller.elements.missile.dumb.DumbMissileCollectionManager;
import org.schema.game.common.controller.elements.missile.dumb.DumbMissileUnit;

import java.util.ArrayList;

public class WeaponGroup extends LuaMadeUserdata {

	private final SegmentController segmentController;
	private final Object collectionManager;
	private final String type;

	public WeaponGroup(SegmentController segmentController, Object collectionManager, String type) {
		this.segmentController = segmentController;
		this.collectionManager = collectionManager;
		this.type = type;
	}

	@LuaMadeCallable
	public String getType() {
		return type;
	}

	@LuaMadeCallable
	public Integer getBlockCount() {
		if(collectionManager instanceof ElementCollectionManager) {
			return ((ElementCollectionManager<?, ?, ?>) collectionManager).getTotalSize();
		}
		return 0;
	}

	@LuaMadeCallable
	public WeaponStats[] getUnits() {
		ArrayList<WeaponStats> stats = new ArrayList<>();
		try {
			if(collectionManager instanceof WeaponCollectionManager) {
				WeaponCollectionManager cm = (WeaponCollectionManager) collectionManager;
				for(Object unit : cm.getElementCollections()) {
					stats.add(new WeaponStats((WeaponUnit) unit, "CANNON"));
				}
			} else if(collectionManager instanceof DamageBeamCollectionManager) {
				DamageBeamCollectionManager cm = (DamageBeamCollectionManager) collectionManager;
				for(Object unit : cm.getElementCollections()) {
					stats.add(new WeaponStats((DamageBeamUnit) unit, "BEAM"));
				}
			} else if(collectionManager instanceof DumbMissileCollectionManager) {
				DumbMissileCollectionManager cm = (DumbMissileCollectionManager) collectionManager;
				for(Object unit : cm.getElementCollections()) {
					stats.add(new WeaponStats((DumbMissileUnit) unit, "MISSILE"));
				}
			}
		} catch(Exception ignored) {
		}
		return stats.toArray(new WeaponStats[0]);
	}

	@LuaMadeCallable
	public Float getDamage() {
		float total = 0;
		for(WeaponStats unit : getUnits()) total += unit.getDamage();
		return total;
	}

	@LuaMadeCallable
	public Float getSpeed() {
		WeaponStats[] units = getUnits();
		if(units.length == 0) return 0f;
		return units[0].getSpeed();
	}

	@LuaMadeCallable
	public Float getRange() {
		WeaponStats[] units = getUnits();
		if(units.length == 0) return 0f;
		return units[0].getRange();
	}

	@LuaMadeCallable
	public Float getReloadTime() {
		WeaponStats[] units = getUnits();
		if(units.length == 0) return 0f;
		return units[0].getReloadTime();
	}

	@LuaMadeCallable
	public Float getPowerConsumption() {
		float total = 0;
		for(WeaponStats unit : getUnits()) total += unit.getPowerConsumption();
		return total;
	}

	@LuaMadeCallable
	public Float getDPS() {
		float totalDamage = getDamage();
		float reloadMs = getReloadTime();
		if(reloadMs <= 0) return 0f;
		return totalDamage / (reloadMs / 1000f);
	}

	public Object getCollectionManager() {
		return collectionManager;
	}
}
