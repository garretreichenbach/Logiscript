package luamade.lua.entity.combat;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.elements.weapon.WeaponUnit;
import org.schema.game.common.controller.elements.beam.damageBeam.DamageBeamUnit;
import org.schema.game.common.controller.elements.missile.MissileUnit;

public class WeaponStats extends LuaMadeUserdata {

	private final Object unit;
	private final String type;

	public WeaponStats(Object unit, String type) {
		this.unit = unit;
		this.type = type;
	}

	@LuaMadeCallable
	public String getType() {
		return type;
	}

	@LuaMadeCallable
	public Float getDamage() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getDamage();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getBeamPower();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getDamage();
		return 0f;
	}

	@LuaMadeCallable
	public Float getBaseDamage() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getBaseDamage();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getBaseBeamPower();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getBaseDamage();
		return 0f;
	}

	@LuaMadeCallable
	public Float getSpeed() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getSpeed();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getSpeed();
		return 0f;
	}

	@LuaMadeCallable
	public Float getRange() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getDistanceRaw();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getDistanceFull();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getDistanceRaw();
		return 0f;
	}

	@LuaMadeCallable
	public Float getReloadTime() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getReloadTimeMs();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getReloadTimeMs();
		return 0f;
	}

	@LuaMadeCallable
	public Float getPowerConsumption() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getPowerConsumption();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getPowerConsumption();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getPowerConsumption();
		return 0f;
	}

	@LuaMadeCallable
	public Float getBasePowerConsumption() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getBasePowerConsumption();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getBasePowerConsumption();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getBasePowerConsumption();
		return 0f;
	}

	@LuaMadeCallable
	public Double getPowerPerSecond() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getPowerConsumedPerSecondCharging();
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getPowerConsumedPerSecondCharging();
		if(unit instanceof MissileUnit) return ((MissileUnit<?, ?, ?>) unit).getPowerConsumedPerSecondCharging();
		return 0.0;
	}

	@LuaMadeCallable
	public Float getTickRate() {
		if(unit instanceof DamageBeamUnit) return ((DamageBeamUnit) unit).getTickRate();
		return 0f;
	}

	@LuaMadeCallable
	public Float getImpactForce() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getImpactForce();
		return 0f;
	}

	@LuaMadeCallable
	public Float getRecoil() {
		if(unit instanceof WeaponUnit) return ((WeaponUnit) unit).getRecoil();
		return 0f;
	}
}
