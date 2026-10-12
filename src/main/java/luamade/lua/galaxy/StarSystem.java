package luamade.lua.galaxy;

import api.common.GameServer;
import luamade.lua.data.Vec3f;
import luamade.lua.data.Vec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.SectorInformation;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.common.data.world.VoidSystem;

import javax.vecmath.Vector3f;
import java.util.ArrayList;

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

	/**
	 * Absolute sector position of the system's star — the sector every orbit is centred on. Without
	 * the galaxy generator (legacy universes) this is the system's middle sector.
	 */
	@LuaMadeCallable
	public Vec3i getSunSector() {
		Vector3i sys = system.getPos();
		Vector3i offset = new Vector3i();
		org.schema.game.server.data.Galaxy galaxy = galaxy();
		if(galaxy != null) galaxy.getSunPositionOffset(Galaxy.relPos(sys), offset);
		int half = VoidSystem.SYSTEM_SIZE / 2;
		return new Vec3i(new Vector3i(
				sys.x * VoidSystem.SYSTEM_SIZE + half + offset.x,
				sys.y * VoidSystem.SYSTEM_SIZE + half + offset.y,
				sys.z * VoidSystem.SYSTEM_SIZE + half + offset.z));
	}

	/** Normal of the system's orbital plane (unit vector, galactic axes), as a {@code Vec3f}. */
	@LuaMadeCallable
	public Vec3f getAxis() {
		org.schema.game.server.data.Galaxy galaxy = galaxy();
		if(galaxy == null) return new Vec3f(0, 1, 0);
		return new Vec3f(galaxy.getSystemAxis(Galaxy.relPos(system.getPos()), new Vector3f()));
	}

	/** Star colour as {@code {r, g, b, a}} normalized floats, or {@code nil} without a galaxy. */
	@LuaMadeCallable
	public Double[] getSunColor() {
		org.schema.game.server.data.Galaxy galaxy = galaxy();
		return galaxy == null ? null : Galaxy.color(galaxy.getSunColor(Galaxy.relPos(system.getPos())));
	}

	/**
	 * The system's populated orbit rings as an {@code Orbit[]}, innermost first. Empty for void /
	 * black-hole systems and legacy universes, matching what the vanilla galaxy map draws.
	 */
	@LuaMadeCallable
	public Orbit[] getOrbits() {
		org.schema.game.server.data.Galaxy galaxy = galaxy();
		if(galaxy == null || !org.schema.game.server.data.Galaxy.USE_GALAXY) return new Orbit[0];
		Vector3i rel = Galaxy.relPos(system.getPos());
		if(!galaxy.isStellarSystem(rel)) return new Orbit[0];
		int[] slots = galaxy.getSystemOrbits(rel, new int[8]);
		ArrayList<Orbit> out = new ArrayList<>();
		for(int i = 0; i < slots.length; i++) {
			if(slots[i] > 0) out.add(new Orbit(i, org.schema.game.server.data.Galaxy.isPlanetOrbit(slots[i])));
		}
		return out.toArray(new Orbit[0]);
	}

	private org.schema.game.server.data.Galaxy galaxy() {
		try {
			return GameServer.getUniverse().getGalaxyFromSystemPos(system.getPos());
		} catch(Exception exception) {
			return null;
		}
	}

	private static Vector3i toVector(Vec3i v) {
		return new Vector3i(v.x, v.y, v.z);
	}
}
