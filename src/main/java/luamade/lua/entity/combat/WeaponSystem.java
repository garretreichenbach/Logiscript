package luamade.lua.entity.combat;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.elements.ShipManagerContainer;
import org.schema.game.common.controller.elements.StationaryManagerContainer;
import org.schema.game.common.controller.elements.weapon.WeaponCollectionManager;
import org.schema.game.common.controller.elements.beam.damageBeam.DamageBeamCollectionManager;
import org.schema.game.common.controller.elements.missile.dumb.DumbMissileCollectionManager;

import java.util.ArrayList;
import java.util.List;

public class WeaponSystem extends LuaMadeUserdata {

	private final SegmentController segmentController;

	public WeaponSystem(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public WeaponGroup[] getCannons() {
		return getGroups("CANNON");
	}

	@LuaMadeCallable
	public WeaponGroup[] getBeams() {
		return getGroups("BEAM");
	}

	@LuaMadeCallable
	public WeaponGroup[] getMissiles() {
		return getGroups("MISSILE");
	}

	@LuaMadeCallable
	public WeaponGroup[] getAllWeapons() {
		ArrayList<WeaponGroup> groups = new ArrayList<>();
		for(WeaponGroup g : getCannons()) groups.add(g);
		for(WeaponGroup g : getBeams()) groups.add(g);
		for(WeaponGroup g : getMissiles()) groups.add(g);
		return groups.toArray(new WeaponGroup[0]);
	}

	@LuaMadeCallable
	public Integer getCannonCount() {
		return getCannons().length;
	}

	@LuaMadeCallable
	public Integer getBeamCount() {
		return getBeams().length;
	}

	@LuaMadeCallable
	public Integer getMissileCount() {
		return getMissiles().length;
	}

	@LuaMadeCallable
	public Float getMissileCapacity() {
		if(segmentController instanceof Ship) {
			return ((Ship) segmentController).getManagerContainer().getMissileCapacity();
		} else if(segmentController instanceof SpaceStation) {
			return ((SpaceStation) segmentController).getManagerContainer().getMissileCapacity();
		}
		return 0f;
	}

	@LuaMadeCallable
	public Float getMissileCapacityMax() {
		if(segmentController instanceof Ship) {
			return ((Ship) segmentController).getManagerContainer().getMissileCapacityMax();
		} else if(segmentController instanceof SpaceStation) {
			return ((SpaceStation) segmentController).getManagerContainer().getMissileCapacityMax();
		}
		return 0f;
	}

	@LuaMadeCallable
	public Float getMissileReloadTime() {
		if(segmentController instanceof Ship) {
			return ((Ship) segmentController).getManagerContainer().getMissileCapacityReloadTime();
		} else if(segmentController instanceof SpaceStation) {
			return ((SpaceStation) segmentController).getManagerContainer().getMissileCapacityReloadTime();
		}
		return 0f;
	}

	private WeaponGroup[] getGroups(String typeName) {
		ArrayList<WeaponGroup> groups = new ArrayList<>();
		try {
			if(segmentController instanceof Ship) {
				ShipManagerContainer mc = ((Ship) segmentController).getManagerContainer();
				addGroups(mc, typeName, groups);
			} else if(segmentController instanceof SpaceStation) {
				StationaryManagerContainer<?> mc = ((SpaceStation) segmentController).getManagerContainer();
				addGroups(mc, typeName, groups);
			}
		} catch(Exception ignored) {
		}
		return groups.toArray(new WeaponGroup[0]);
	}

	private void addGroups(ShipManagerContainer mc, String typeName, List<WeaponGroup> groups) {
		switch(typeName) {
			case "CANNON":
				for(WeaponCollectionManager cm : mc.getWeapon().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
			case "BEAM":
				for(DamageBeamCollectionManager cm : mc.getBeam().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
			case "MISSILE":
				for(DumbMissileCollectionManager cm : mc.getMissile().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
		}
	}

	private void addGroups(StationaryManagerContainer<?> mc, String typeName, List<WeaponGroup> groups) {
		switch(typeName) {
			case "CANNON":
				for(WeaponCollectionManager cm : mc.getWeapon().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
			case "BEAM":
				for(DamageBeamCollectionManager cm : mc.getBeam().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
			case "MISSILE":
				for(DumbMissileCollectionManager cm : mc.getMissile().getCollectionManagers()) {
					groups.add(new WeaponGroup(segmentController, cm, typeName));
				}
				break;
		}
	}
}
