package luamade.lua.galaxy;

import api.common.GameServer;
import luamade.lua.data.Vec3i;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.data.world.Sector;
import org.schema.game.common.data.world.SectorInformation;
import org.schema.game.common.data.world.Universe;
import org.schema.game.common.data.world.VoidSystem;

/**
 * Read-only snapshot of a single sector's composition, decoded from the galaxy's
 * persisted star-system data. Produced by {@link Galaxy#getSectorInfo(Vec3i)} and
 * {@link Galaxy#scanSectors(Vec3i, Integer)}.
 *
 * <p>The sector type / planet type / station type are read straight from the
 * containing {@link org.schema.game.common.data.world.StellarSystem} and do
 * <b>not</b> require the sector to be loaded or generated. Live details that only
 * exist for an actively simulated sector ({@link #isLoaded()},
 * {@link #getProtectionMode()}) are resolved lazily against the server universe.
 */
public class SectorInfo extends LuaMadeUserdata {

	private final Vector3i pos;
	private final SectorInformation.SectorType type;
	private final String planetType;
	private final SpaceStation.SpaceStationType stationType;

	public SectorInfo(Vector3i pos, SectorInformation.SectorType type,
	                  String planetType, SpaceStation.SpaceStationType stationType) {
		this.pos = new Vector3i(pos);
		this.type = type;
		this.planetType = planetType;
		this.stationType = stationType;
	}

	/** Absolute sector position, as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getPos() {
		return new Vec3i(pos);
	}

	/** System-grid position of the system that contains this sector, as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getSystemPos() {
		Vector3i out = new Vector3i();
		VoidSystem.getContainingSystem(pos, out);
		return new Vec3i(out);
	}

	/**
	 * Sector-type name (e.g. {@code "SUN"}, {@code "PLANET"}, {@code "ASTEROID"},
	 * {@code "VOID"}, {@code "SPACE_STATION"}, {@code "BLACK_HOLE"}), or {@code nil}.
	 */
	@LuaMadeCallable
	public String getSectorType() {
		return type == null ? null : type.name();
	}

	/**
	 * Planet-type config id ({@code "terrestrial"}, {@code "barren"}, …) when this sector holds
	 * a planet, otherwise {@code nil}.
	 */
	@LuaMadeCallable
	public String getPlanetType() {
		if(type != SectorInformation.SectorType.PLANET || planetType == null) return null;
		return planetType;
	}

	/**
	 * Space-station type name ({@code "PIRATE"}, {@code "TRADING_GUILD"}, …) when
	 * this sector holds a station, otherwise {@code nil}.
	 */
	@LuaMadeCallable
	public String getStationType() {
		if(type != SectorInformation.SectorType.SPACE_STATION || stationType == null) return null;
		return stationType.name();
	}

	/** {@code true} when this sector is anything other than empty {@code VOID}. */
	@LuaMadeCallable
	public Boolean isPopulated() {
		return type != null && type != SectorInformation.SectorType.VOID;
	}

	/** {@code true} when this sector is currently loaded / actively simulated on the server. */
	@LuaMadeCallable
	public Boolean isLoaded() {
		Universe universe = universe();
		if(universe == null) return false;
		try {
			return universe.isSectorLoaded(pos);
		} catch(Exception exception) {
			return false;
		}
	}

	/**
	 * Raw sector protection bitmask (spawn / attack / entry / exit locks) for a
	 * currently-loaded sector, or {@code nil} when the sector is not loaded.
	 */
	@LuaMadeCallable
	public Integer getProtectionMode() {
		Universe universe = universe();
		if(universe == null) return null;
		try {
			Sector sector = universe.getSectorWithoutLoading(pos);
			return sector == null ? null : sector.getProtectionMode();
		} catch(Exception exception) {
			return null;
		}
	}

	@Override
	public String toString() {
		return String.format("SectorInfo(%d, %d, %d, %s)", pos.x, pos.y, pos.z, type == null ? "?" : type.name());
	}

	private static Universe universe() {
		try {
			return GameServer.getUniverse();
		} catch(Exception exception) {
			return null;
		}
	}
}
