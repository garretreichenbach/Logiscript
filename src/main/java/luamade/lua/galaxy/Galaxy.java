package luamade.lua.galaxy;

import api.common.GameServer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import luamade.lua.data.Vec3i;
import luamade.lua.entity.RemoteEntity;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.data.world.FTLConnection;
import org.schema.game.common.data.world.SectorInformation;
import org.schema.game.common.data.world.StellarSystem;
import org.schema.game.common.data.world.Universe;
import org.schema.game.common.data.world.VoidSystem;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.objects.Sendable;

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

	/**
	 * Hard cap on {@link #scanSectors} radius, bounding the returned array to
	 * (2r+1)^3. Matches the {@code Entity.getNearbyEntities} perception limit of
	 * 3 sectors — roughly the cluster loaded/simulated around a ship — so scripts
	 * can't read the composition of far-off space they couldn't otherwise reach.
	 */
	private static final int MAX_SCAN_RADIUS = 3;

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
	 * Composition of a single sector — type, plus planet/station sub-type — as a
	 * {@link SectorInfo}, or {@code nil} if it cannot be resolved. Reads persisted
	 * star-system data; does not load or generate the sector.
	 */
	@LuaMadeCallable
	public SectorInfo getSectorInfo(Vec3i sectorPos) {
		Universe universe = universe();
		if(universe == null) return null;
		Vector3i pos = toVector(sectorPos);
		try {
			StellarSystem system = universe.getStellarSystemFromSecPos(pos);
			return system == null ? null : describe(system, pos);
		} catch(Exception exception) {
			return null;
		}
	}

	/**
	 * Cube scan of sector composition centred on {@code centerSectorPos}, out to
	 * {@code radius} sectors on each axis (clamped to {@code [0, 3]}). Returns a
	 * {@code SectorInfo[]} of every sector in the (2r+1)³ cube in x→y→z order, or
	 * an empty array if unavailable.
	 *
	 * <p>Reads persisted star-system data without loading sectors, and reuses each
	 * star-system lookup across the sectors it contains, so a scan touches at most
	 * a handful of systems rather than one lookup per sector.
	 */
	@LuaMadeCallable
	public SectorInfo[] scanSectors(Vec3i centerSectorPos, Integer radius) {
		Universe universe = universe();
		if(universe == null || centerSectorPos == null) return new SectorInfo[0];
		int r = radius == null ? 0 : Math.clamp(radius, 0, MAX_SCAN_RADIUS);
		int cx = centerSectorPos.x, cy = centerSectorPos.y, cz = centerSectorPos.z;
		ArrayList<SectorInfo> out = new ArrayList<>();

		// Cache the current star system so neighbouring sectors in the same system
		// don't each trigger a fresh (potentially DB-backed) lookup.
		Vector3i containing = new Vector3i();
		Vector3i lastSystem = null;
		StellarSystem system = null;
		Vector3i pos = new Vector3i();

		for(int z = cz - r; z <= cz + r; z++) {
			for(int y = cy - r; y <= cy + r; y++) {
				for(int x = cx - r; x <= cx + r; x++) {
					pos.set(x, y, z);
					VoidSystem.getContainingSystem(pos, containing);
					if(!containing.equals(lastSystem)) {
						try {
							system = universe.getStellarSystemFromSecPos(pos);
						} catch(Exception exception) {
							system = null;
						}
						lastSystem = new Vector3i(containing);
					}
					if(system != null) {
						out.add(describe(system, pos));
					}
				}
			}
		}
		return out.toArray(new SectorInfo[0]);
	}

	/**
	 * Loaded entities (ships, stations, asteroids, …) currently in the given
	 * <b>sector</b>, as a {@code RemoteEntity[]}. Only actively simulated sectors
	 * return results; cloaked / radar-jamming ships are omitted. Empty when no
	 * server universe is available.
	 */
	@LuaMadeCallable
	public RemoteEntity[] getEntitiesInSector(Vec3i sectorPos) {
		GameServerState state;
		try {
			state = GameServer.getServerState();
		} catch(Exception exception) {
			return new RemoteEntity[0];
		}
		if(state == null || sectorPos == null) return new RemoteEntity[0];
		Vector3i target = toVector(sectorPos);
		ArrayList<RemoteEntity> out = new ArrayList<>();
		Vector3i scratch = new Vector3i();
		try {
			for(Sendable sendable : state.getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
				if(!(sendable instanceof SegmentController controller)) continue;
				if(controller instanceof Ship ship) {
					if(ship.getManagerContainer().isJamming() || ship.getManagerContainer().isCloaked()) continue;
				}
				if(controller.getSector(scratch).equals(target)) {
					out.add(new RemoteEntity(controller));
				}
			}
		} catch(Exception exception) {
			return out.toArray(new RemoteEntity[0]);
		}
		return out.toArray(new RemoteEntity[0]);
	}

	/** Builds a {@link SectorInfo} for an absolute sector position within a resolved system. */
	private static SectorInfo describe(StellarSystem system, Vector3i pos) {
		SectorInformation.SectorType type = system.getSectorType(pos);
		String planetType = null;
		org.schema.game.common.controller.SpaceStation.SpaceStationType stationType = null;
		if(type == SectorInformation.SectorType.PLANET) {
			planetType = system.getPlanetTypeId(pos);
		} else if(type == SectorInformation.SectorType.SPACE_STATION) {
			stationType = system.getSpaceStationTypeType(pos);
		}
		return new SectorInfo(pos, type, planetType, stationType);
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
