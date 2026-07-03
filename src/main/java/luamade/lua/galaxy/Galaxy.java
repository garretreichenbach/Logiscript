package luamade.lua.galaxy;

import api.common.GameServer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import luamade.lua.data.Vec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.FTLConnection;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.common.data.world.Universe;
import org.schema.game.common.data.world.VoidSystem;

import java.util.ArrayList;

/**
 * Galaxy map queries: star-system lookup, ownership, sector composition, and
 * warp-gate / FTL routing. Bound as the global {@code galaxy}.
 *
 * <p>Backed by the server-authoritative {@link Universe}. Because scripts run
 * server-side, these queries hit persisted galaxy data directly and do not
 * require the target sectors to be loaded. Coordinates come in two flavours:
 * <b>sector</b> positions (the fine grid entities live in) and <b>system</b>
 * positions (the coarse galaxy-map grid; each system spans
 * {@code getSystemSize()} sectors per axis).
 */
public class Galaxy extends LuaMadeUserdata {

	/** The universe/galaxy display name, or an empty string if unavailable. */
	@LuaMadeCallable
	public String getName() {
		Universe universe = universe();
		if(universe == null) return "";
		String name = universe.getName();
		return name == null ? "" : name;
	}

	/** Sectors-per-axis in a single star system (StarMade's fixed system size). */
	@LuaMadeCallable
	public Integer getSystemSize() {
		return VoidSystem.SYSTEM_SIZE;
	}

	/**
	 * The star system containing the given <b>sector</b> position, or {@code nil}
	 * if it cannot be resolved.
	 */
	@LuaMadeCallable
	public StarSystem getSystem(Vec3i sectorPos) {
		Universe universe = universe();
		if(universe == null) return null;
		try {
			StellarSystem system = universe.getStellarSystemFromSecPos(toVector(sectorPos));
			return system == null ? null : new StarSystem(system);
		} catch(Exception exception) {
			return null;
		}
	}

	/**
	 * The star system at the given <b>system</b> position (galaxy-map grid), or
	 * {@code nil} if it cannot be resolved.
	 */
	@LuaMadeCallable
	public StarSystem getSystemAt(Vec3i systemPos) {
		Universe universe = universe();
		if(universe == null) return null;
		try {
			StellarSystem system = universe.getStellarSystemFromStellarPos(toVector(systemPos));
			return system == null ? null : new StarSystem(system);
		} catch(Exception exception) {
			return null;
		}
	}

	/**
	 * The system-grid position that contains the given <b>sector</b> position.
	 * Pure coordinate math — does not touch galaxy data.
	 */
	@LuaMadeCallable
	public Vec3i getSystemPos(Vec3i sectorPos) {
		Vector3i out = new Vector3i();
		VoidSystem.getContainingSystem(toVector(sectorPos), out);
		return new Vec3i(out);
	}

	/**
	 * Owning faction of the system containing the given <b>sector</b> position, or
	 * {@code nil} if it cannot be resolved. Faction id {@code 0} means unclaimed.
	 */
	@LuaMadeCallable
	public Faction getSystemOwner(Vec3i sectorPos) {
		StarSystem system = getSystem(sectorPos);
		return system == null ? null : system.getOwnerFaction();
	}

	/**
	 * Ownership relationship of the system at the given <b>sector</b> position
	 * relative to a faction. Returns one of {@code "NONE"}, {@code "BY_SELF"},
	 * {@code "BY_ALLY"}, {@code "BY_ENEMY"}, {@code "BY_NEUTRAL"}, or {@code nil}
	 * if it cannot be resolved.
	 */
	@LuaMadeCallable
	public String getSystemOwnership(Vec3i sectorPos, Integer factionId) {
		Universe universe = universe();
		if(universe == null || factionId == null) return null;
		try {
			StellarSystem system = universe.getStellarSystemFromSecPos(toVector(sectorPos));
			if(system == null) return null;
			Universe.SystemOwnershipType type = universe.getSystemOwnerShipType(system, factionId);
			return type == null ? null : type.name();
		} catch(Exception exception) {
			return null;
		}
	}

	/**
	 * {@link org.schema.game.common.data.world.SectorInformation.SectorType} name
	 * for a specific <b>sector</b> position (e.g. {@code "SUN"}, {@code "PLANET"},
	 * {@code "ASTEROID"}, {@code "VOID"}), or {@code nil} if it cannot be resolved.
	 */
	@LuaMadeCallable
	public String getSectorType(Vec3i sectorPos) {
		StarSystem system = getSystem(sectorPos);
		return system == null ? null : system.getSectorType(sectorPos);
	}

	/**
	 * Every warp gate / wormhole / race-way link currently known to the galaxy,
	 * as an {@code FtlConnection[]}. Empty when no server universe is available.
	 */
	@LuaMadeCallable
	public FtlConnection[] getWarpGates() {
		Universe universe = universe();
		if(universe == null) return new FtlConnection[0];
		try {
			Object2ObjectOpenHashMap<Vector3i, FTLConnection> data = universe.getGalaxyManager().getFtlData();
			if(data == null) return new FtlConnection[0];
			ArrayList<FtlConnection> out = new ArrayList<>(data.size());
			for(FTLConnection connection : data.values()) {
				if(connection != null) out.add(new FtlConnection(connection));
			}
			return out.toArray(new FtlConnection[0]);
		} catch(Exception exception) {
			return new FtlConnection[0];
		}
	}

	private static Universe universe() {
		try {
			return GameServer.getUniverse();
		} catch(Exception exception) {
			return null;
		}
	}

	private static Vector3i toVector(Vec3i v) {
		return new Vector3i(v.x, v.y, v.z);
	}
}
