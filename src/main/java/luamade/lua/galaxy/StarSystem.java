package luamade.lua.galaxy;

import luamade.lua.data.Vec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SectorInformation;
import org.schema.game.common.data.world.StellarSystem;

/**
 * Read-only wrapper over a StarMade {@link StellarSystem}. Represents one star
 * system (a 16x16x16 cube of sectors) on the galaxy map: its name, owner, and
 * the sector/planet layout inside it.
 *
 * <p>Obtained via {@link Galaxy#getSystem(Vec3i)} / {@link Galaxy#getSystemAt(Vec3i)}.
 * Values reflect the system's persisted state and are safe to poll periodically;
 * they do not require the system's sectors to be loaded.
 */
public class StarSystem extends LuaMadeUserdata {

	private final StellarSystem system;

	public StarSystem(StellarSystem system) {
		this.system = system;
	}

	/** Display name of the system, or an empty string if unnamed. */
	@LuaMadeCallable
	public String getName() {
		String name = system.getName();
		return name == null ? "" : name;
	}

	/** System-grid coordinate of this system (the outer galaxy-map coordinate), as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getSystemPos() {
		return new Vec3i(system.getPos());
	}

	/** The owning faction. Faction id {@code 0} means unclaimed / neutral. */
	@LuaMadeCallable
	public Faction getOwnerFaction() {
		return new Faction(system.getOwnerFaction());
	}

	/** Owning faction id, {@code 0} when unclaimed. */
	@LuaMadeCallable
	public Integer getOwnerFactionId() {
		return system.getOwnerFaction();
	}

	/** Player UID that claimed the system, or an empty string when unclaimed. */
	@LuaMadeCallable
	public String getOwnerUID() {
		String uid = system.getOwnerUID();
		return uid == null ? "" : uid;
	}

	/** {@code true} when a faction owns this system. */
	@LuaMadeCallable
	public Boolean isClaimed() {
		return system.getOwnerFaction() != 0;
	}

	/**
	 * The {@link SectorInformation.SectorType} name of the system's center sector
	 * (e.g. {@code "SUN"}, {@code "BLACK_HOLE"}, {@code "GIANT"}, {@code "MAIN"}),
	 * or {@code nil} if unavailable.
	 */
	@LuaMadeCallable
	public String getCenterSectorType() {
		SectorInformation.SectorType type = system.getCenterSectorType();
		return type == null ? null : type.name();
	}

	/**
	 * The {@link SectorInformation.SectorType} name for a specific absolute sector
	 * position inside this system (e.g. {@code "PLANET"}, {@code "ASTEROID"},
	 * {@code "VOID"}, {@code "SPACE_STATION"}), or {@code nil} if unavailable.
	 */
	@LuaMadeCallable
	public String getSectorType(Vec3i sectorPos) {
		try {
			SectorInformation.SectorType type = system.getSectorType(toVector(sectorPos));
			return type == null ? null : type.name();
		} catch(Exception exception) {
			return null;
		}
	}

	/**
	 * The planet-type config id for a specific absolute sector position inside
	 * this system (e.g. {@code "terrestrial"}, {@code "barren"}), or {@code nil}
	 * when the sector holds no planet.
	 */
	@LuaMadeCallable
	public String getPlanetType(Vec3i sectorPos) {
		try {
			Vector3i pos = toVector(sectorPos);
			if(system.getSectorType(pos) != SectorInformation.SectorType.PLANET) return null;
			return system.getPlanetTypeId(pos);
		} catch(Exception exception) {
			return null;
		}
	}

	private static Vector3i toVector(Vec3i v) {
		return new Vector3i(v.x, v.y, v.z);
	}
}
