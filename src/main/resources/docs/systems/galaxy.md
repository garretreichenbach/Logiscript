# Galaxy Map API

`galaxy` is a global for querying the galaxy map: star systems, ownership, sector
composition, and warp-gate / FTL routes.

It is backed by the server-authoritative universe. Because scripts run
server-side, these queries read persisted galaxy data directly and do **not**
require the target sectors to be loaded or nearby — you can inspect the far side
of the galaxy from a stationary computer.

## Coordinate spaces

Two grids are involved:

- **Sector** positions — the fine grid entities and ships live in. `entity.getSector()` returns one.
- **System** positions — the coarse galaxy-map grid. Each system spans `galaxy.getSystemSize()` sectors per axis (StarMade's fixed system size). `entity.getSystem()` returns one.

Use `galaxy.getSystemPos(sectorPos)` to convert a sector position to its
containing system position.

## Typical usage

```lua
-- What system am I in, and who owns it?
local sector = entity.getSector()
local system = galaxy.getSystem(sector)
if system ~= nil then
    print("System:", system.getName())
    print("Center:", system.getCenterSectorType())     -- e.g. "SUN", "BLACK_HOLE"
    local owner = system.getOwnerFaction()
    print("Owner:", system.isClaimed() and owner.getName() or "unclaimed")
end

-- Is this system friendly to my faction?
local rel = galaxy.getSystemOwnership(sector, entity.getFaction().getFactionId())
print("Ownership:", rel)   -- "BY_SELF" / "BY_ALLY" / "BY_ENEMY" / "BY_NEUTRAL" / "NONE"

-- Scan the sectors around the ship and tally what's out there.
for _, info in ipairs(galaxy.scanSectors(sector, 3)) do
    if info.isPopulated() then
        local p = info.getPos()
        print(p.getX() .. "," .. p.getY() .. "," .. p.getZ(),
              info.getSectorType(), info.getPlanetType() or info.getStationType() or "")
    end
end

-- What's physically in my current sector right now?
for _, e in ipairs(galaxy.getEntitiesInSector(sector)) do
    print("contact:", e.getName())
end

-- List every warp gate route in the galaxy. Vec3i has no tostring, so build
-- coordinate strings from components.
local function coords(v) return v.getX() .. "," .. v.getY() .. "," .. v.getZ() end
for _, gate in ipairs(galaxy.getWarpGates()) do
    local from = gate.getFrom()
    local types = gate.getTypes()
    for i, dest in ipairs(gate.getDestinations()) do
        print(coords(from) .. " -> " .. coords(dest) .. " (" .. (types[i] or "UNKNOWN") .. ")")
    end
end
```

A ready-made visual example ships as **`/bin/sectormap.lua`** — an in-world 3D
holographic sector map driven by `galaxy.scanSectors` and the `projector` / gfx3d
API.

## Reference

### `galaxy`

- `getName()`
Universe/galaxy display name as a `String` (empty if unavailable).

- `getSystemSize()`
Sectors-per-axis in a single star system, as an `Integer`.

- `getSystem(sectorPos: Vec3i)`
The `StarSystem` containing the given **sector** position, or `nil`.

- `getSystemAt(systemPos: Vec3i)`
The `StarSystem` at the given **system** position (galaxy-map grid), or `nil`.

- `getSystemPos(sectorPos: Vec3i)`
The **system** position that contains the given **sector** position, as a `Vec3i`. Pure coordinate math — never touches galaxy data.

- `getSystemOwner(sectorPos: Vec3i)`
Owning `Faction` of the system containing the given **sector** position, or `nil`. Faction id `0` means unclaimed.

- `getSystemOwnership(sectorPos: Vec3i, factionId: Integer)`
Ownership relationship of that system relative to a faction: `"NONE"`, `"BY_SELF"`, `"BY_ALLY"`, `"BY_ENEMY"`, `"BY_NEUTRAL"`, or `nil`.

- `getSectorType(sectorPos: Vec3i)`
Sector-type name for a specific **sector** position (`"SUN"`, `"PLANET"`, `"ASTEROID"`, `"VOID"`, `"SPACE_STATION"`, …), or `nil`.

- `getSectorInfo(sectorPos: Vec3i)`
Full composition of one **sector** as a `SectorInfo` (type + planet/station sub-type), or `nil`. Reads persisted data; never loads or generates the sector.

- `scanSectors(centerSectorPos: Vec3i, radius: Integer)`
Cube scan of sector composition out to `radius` sectors per axis (clamped to `[0, 3]` — the same local perception range as `entity.getNearbyEntities`). Returns a `SectorInfo[]` for every sector in the (2r+1)³ cube, in x→y→z order. Each star-system lookup is reused across the sectors it contains, so a scan touches only a handful of systems.

- `getEntitiesInSector(sectorPos: Vec3i)`
Loaded entities (ships, stations, asteroids, …) currently in the given **sector**, as a `RemoteEntity[]`. Only actively simulated sectors return results; cloaked / radar-jamming ships are omitted.

- `getWarpGates()`
Every warp gate / wormhole / race-way link known to the galaxy, as an `FtlConnection[]`. Empty when no server universe is available.

- `getGalaxySize()`
Systems-per-axis in one galaxy, as an `Integer` (StarMade uses `128`).

- `getGalaxyCenter(systemPos: Vec3i)`
Absolute **system** position of the core of the galaxy containing the given **system** position, as a `Vec3i`. Pure coordinate math.

- `getStars(systemPos: Vec3i, radius: Integer)`
Every star within `radius` systems (euclidean, clamped to the galaxy size) of the given **system** position, as a `StarInfo[]`. Read from galaxy generation data, so nothing is loaded, but a whole-galaxy query can return tens of thousands of entries — keep the radius modest when polling. Black holes are not listed; they appear as wormhole endpoints in `getWarpGates()`.

- `getClaimedSystems(systemPos: Vec3i, radius: Integer)`
Every faction-claimed system within `radius` systems (euclidean, clamped to the galaxy size) of the given **system** position, as a `SystemClaim[]`. One database query per faction — far cheaper than `getSystemAt()` per star, but poll it every few seconds rather than every frame.

## StarSystem

Read-only snapshot of one star system (a `getSystemSize()`³ cube of sectors).
Reflects persisted state and is safe to poll periodically.

- `getName()`
Display name, or an empty string if unnamed.

- `getSystemPos()`
System-grid coordinate of this system, as a `Vec3i`.

- `getOwnerFaction()` / `getOwnerFactionId()`
Owning `Faction` / raw faction id `Integer`. `0` = unclaimed.

- `getOwnerUID()`
Player UID that claimed the system, or an empty string.

- `isClaimed()`
`true` when a faction owns this system.

- `getCenterSectorType()`
Sector-type name of the system's center sector (`"SUN"`, `"BLACK_HOLE"`, `"GIANT"`, `"DOUBLE_STAR"`, `"MAIN"`, …), or `nil`.

- `getSectorType(sectorPos: Vec3i)`
Sector-type name for a specific absolute sector position inside this system, or `nil`.

- `getPlanetType(sectorPos: Vec3i)`
Planet-type config id (`"terrestrial"`, `"barren"`, `"crystalline"`, `"corrupted"`, or a server-defined type) for a sector that holds a planet, or `nil` otherwise.

- `getSunSector()`
Absolute sector position of the system's star, as a `Vec3i` — the sector every orbit is centred on. Legacy (non-galaxy) universes return the middle sector.

- `getSunColor()`
Star colour as `{r, g, b, a}` normalized floats, or `nil` when unavailable.

- `getAxis()`
Normal of the system's orbital plane (unit vector in galactic axes), as a `Vec3f`.

- `getOrbits()`
The system's populated orbit rings, innermost first, as an `Orbit[]` — the same rings the vanilla galaxy map draws. Empty for void / black-hole systems.

```lua
local sys  = galaxy.getSystem(entity.getSector())
local sun  = sys.getSunSector()
local axis = sys.getAxis()
for _, o in ipairs(sys.getOrbits()) do
    print(o.getType(), "orbit at", o.getRadius(), "sectors")  -- ring centred on `sun`, normal `axis`
end
```

## Orbit

One orbit ring of a star system. A ring is a circle of `getRadius()` sectors centred on the
centre of `StarSystem.getSunSector()`, lying in the plane whose normal is `StarSystem.getAxis()`.

- `getIndex()`
Orbit slot, `0` (innermost) to `7`.

- `getRadius()`
Ring radius in sectors (`index + 1.5`).

- `getType()`
`"PLANET"` for a planet orbit, `"BELT"` for an asteroid belt.

## SystemClaim

One faction-claimed system, from `galaxy.getClaimedSystems()`.

- `getSystemPos()`
Absolute system position, as a `Vec3i`.

- `getFactionId()` / `getFaction()`
Owning faction id `Integer` / `Faction`.

## StarInfo

Lightweight snapshot of one star from `galaxy.getStars()`. Use `galaxy.getSystemAt(info.getSystemPos())`
for name and ownership.

- `getSystemPos()`
Absolute system position, as a `Vec3i`.

- `getType()`
`"SUN"`, `"GIANT"` or `"DOUBLE_STAR"`.

- `getColor()`
Star colour as `{r, g, b, a}` normalized floats.

## SectorInfo

Read-only snapshot of a single sector's composition, decoded from persisted
star-system data (no sector load required). Produced by `galaxy.getSectorInfo()`
and `galaxy.scanSectors()`.

- `getPos()`
Absolute sector position, as a `Vec3i`.

- `getSystemPos()`
System-grid position of the containing system, as a `Vec3i`.

- `getSectorType()`
Sector-type name (`"SUN"`, `"PLANET"`, `"ASTEROID"`, `"VOID"`, `"SPACE_STATION"`, `"BLACK_HOLE"`, …), or `nil`.

- `getPlanetType()`
Planet-type name when this sector holds a planet, otherwise `nil`.

- `getStationType()`
Station-type name (`"PIRATE"`, `"TRADING_GUILD"`, …) when this sector holds a station, otherwise `nil`.

- `isPopulated()`
`true` when the sector is anything other than empty `VOID`.

- `isLoaded()`
`true` when the sector is currently loaded / actively simulated on the server.

- `getProtectionMode()`
Raw sector protection bitmask (spawn / attack / entry / exit locks) for a currently-loaded sector, or `nil` when the sector is not loaded.

## FtlConnection

Read-only wrapper over one warp gate, wormhole, or race-way link. Each link has
one source sector and one or more destination sectors, aligned by index with
`getTypes()`. Produced by `galaxy.getWarpGates()`.

- `getFrom()`
Source sector, as a `Vec3i`.

- `getDestinations()`
Destination sectors, as a `Vec3i[]`.

- `getDestinationLocals()`
Local landing positions inside each destination, aligned with `getDestinations()`.

- `getTypes()`
Connection type per destination, aligned with `getDestinations()`: `"WARP_GATE"`, `"WORM_HOLE"`, `"RACE_WAY"`, or `"UNKNOWN"`.

- `getDestinationUID()`
Destination entity/station UID, or an empty string.

- `getDestinationCount()`
Number of destinations, as an `Integer`.

## Notes

- Ownership and system data are persisted; polling every few seconds is cheap. Avoid per-frame polling across large galaxies.
- `getSectorType` / `StarSystem.getPlanetType` decode the system's stored sector layout — they work for unloaded sectors without generating them.
- Warp-gate data mirrors StarMade's galaxy-map FTL overlay; a link only appears once both endpoints are established in the galaxy.
