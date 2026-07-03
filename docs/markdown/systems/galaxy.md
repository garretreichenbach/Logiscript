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

-- List every warp gate route in the galaxy.
for _, gate in ipairs(galaxy.getWarpGates()) do
    local from = gate.getFrom()
    local types = gate.getTypes()
    for i, dest in ipairs(gate.getDestinations()) do
        print(string.format("%s -> %s (%s)",
            tostring(from), tostring(dest), types[i] or "UNKNOWN"))
    end
end
```

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

- `getWarpGates()`
Every warp gate / wormhole / race-way link known to the galaxy, as an `FtlConnection[]`. Empty when no server universe is available.

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
Planet-type name (`"EARTH"`, `"MARS"`, `"DESERT"`, `"ICE"`, `"PURPLE"`) for a sector that holds a planet, or `nil` otherwise.

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
