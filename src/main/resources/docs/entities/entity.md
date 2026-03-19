# Entity API

`Entity` represents the owning structure for a block (ship, station, etc).

## Typical usage

```lua
local entity = console.getBlock().getEntity()
print(entity.getName(), entity.getEntityType())

local reactor = entity.getReactor()
print("Reactor HP:", reactor.getHP(), "/", reactor.getMaxHP())
```

## Identity and location

- `getId()`
Returns the numeric entity ID.

- `getName()`
Returns the entity's display name.

- `setName(name: String)`
Sets the entity's display name.

- `getPos()`
Returns current world position as `LuaVec3i`.

- `getSector()`
Returns current sector coordinates as `LuaVec3i`.

- `getSystem()`
Returns current solar system coordinates as `LuaVec3i`.

- `getEntityType()`
Returns the entity type string: `"SHIP"` or `"SPACE_STATION"`.

## World access and nearby entities

- `getBlockAt(position: LuaVec3i)`
Returns the `Block` at the given local block position, or `nil`.

- `getNearbyEntities()`
Returns `RemoteEntity[]` for all non-cloaked, non-jammed entities in adjacent sectors (within 1 sector radius).

- `getNearbyEntities(radius: Integer)`
Returns `RemoteEntity[]` within `radius` sectors. `radius` is clamped to a maximum of 3.

## Faction and diplomacy

- `getFaction()`
Returns the `Faction` this entity belongs to.

- `getSystemOwner()`
Returns the `Faction` that owns the current solar system, or `nil` when unowned.

## Systems and modules

- `hasReactor()`
Returns `true` when a reactor is installed.

- `getReactor()`
Returns the `Reactor` wrapper.

- `getMaxReactorHP()`
Returns the reactor's maximum HP.

- `getReactorHP()`
Returns the reactor's current HP.

- `getThrust()`
Returns the `Thrust` wrapper.

- `getShieldSystem()`
Returns the `ShieldSystem` wrapper.

- `getShipyards()`
Returns all `Shipyard[]` wrappers on this entity.

## Docking and rails

- `getTurrets()`
Returns `Entity[]` of turrets currently docked to this entity.

- `getDocked()`
Returns `Entity[]` of all entities currently docked to this entity.

- `isEntityDocked(entity: RemoteEntity)`
Returns `true` when `entity` is docked to this entity.

- `undockEntity(entity: RemoteEntity)`
Disconnects `entity` from this entity's rail system.

- `undockAll()`
Releases all docked entities.

- `dockTo(station: RemoteEntity, railDockerBlock: Block)`
Automatically finds the nearest compatible rail block on `station` and connects `railDockerBlock` to it. Both entities must be in the same sector with a friendly faction relationship.

- `dockTo(station: RemoteEntity, railDockerBlock: Block, dockPos: LuaVec3i)`
Docks `railDockerBlock` to the specific rail block at local position `dockPos` on `station`.

- `dockToNearestLoadDock(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `RAIL_LOAD` block on `station`. For cargo pickup.

- `dockToNearestUnloadDock(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `RAIL_UNLOAD` block on `station`. For cargo delivery.

- `dockToNearestBasicRail(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `RAIL_BLOCK_BASIC` block on `station`.

- `dockToNearestDockerRail(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `RAIL_BLOCK_DOCKER` block on `station`.

- `dockToNearestPickupArea(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `PICKUP_AREA` block on `station`. Used for carrier launch/recovery.

- `dockToNearestPickupRail(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `PICKUP_RAIL` block on `station`.

- `dockToNearestExitShootRail(station: RemoteEntity, railDockerBlock: Block)`
Dock to the nearest `EXIT_SHOOT_RAIL` block on `station`.

## AI / status

- `getAI()`
Returns the `EntityAI` wrapper for this entity.

- `getSpeed()`
Returns current speed in blocks/s as a `Double`.

- `getMass()`
Returns total mass as a `Double`.

- `isJamming()` / `canJam()` / `activateJamming(active: Boolean)`
Jamming state query and control.

- `isCloaking()` / `canCloak()` / `activateCloaking(active: Boolean)`
Cloaking state query and control.

## Inventory / fleet

- `getNamedInventory(name: String)`
Returns the `Inventory` whose custom name matches `name`, or `nil` when not found.

- `getPilot()`
Returns the pilot's player name, or `nil` when unoccupied.

- `isInFleet()`
Returns `true` when this entity is assigned to a fleet.

- `getFleet()`
Returns the `Fleet` wrapper, or `nil` when not in a fleet.
