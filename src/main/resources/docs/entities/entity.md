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
Returns current world position as `LuaVec3f`.

- `getBoundingBox()`
  Returns the entity's local-space `BoundingBox` snapshot from the game's internal `SegmentController.getBoundingBox()`
  method.

- `getSector()`
Returns current sector coordinates as `LuaVec3i`.

- `getSystem()`
Returns current solar system coordinates as `LuaVec3i`.

`BoundingBox` exposes:

- `getMin()`
  Returns the local-space minimum corner as `LuaVec3f`.

- `getMax()`
  Returns the local-space maximum corner as `LuaVec3f`.

- `getCenter()`
  Returns the midpoint between `min` and `max` as `LuaVec3f`.

- `getDimensions()`
  Returns the local-space size `(max - min)` as `LuaVec3f`.

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

- `getDockedEntities()`
Alias of `getDocked()`. Returns `Entity[]` of all entities currently docked to this entity.

- `getParent()`
Returns the parent `Entity` this entity is docked to, or `nil` if this entity has no parent.

- `getRoot()`
Returns the root `Entity` for this entity's current rail docking chain.

- `isEntityDocked(entity: RemoteEntity)`
Returns `true` when `entity` is docked to this entity.

- `undockEntity(entity: RemoteEntity)`
Disconnects `entity` from this entity's rail system.

- `undockAll()`
Releases all docked entities.

- `dockTo(station: RemoteEntity, railDockerBlock: Block)`
Automatically finds the nearest compatible rail block on `station` and connects `railDockerBlock` to it. Docking checks sector validity and server-configured docking permissions, and uses a server-configured snap radius.

- `dockTo(station: RemoteEntity, railDockerBlock: Block, dockPos: LuaVec3i)`
Docks `railDockerBlock` to the specific rail block at local position `dockPos` on `station` when within the server-configured snap radius and permission rules.

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

- `getHeading()`
  Returns normalized forward heading as `LuaVec3f`.

- `getUp()`
  Returns normalized up vector as `LuaVec3f`.

- `getRoll()`
  Returns the current roll angle in radians relative to galactic up (world Y axis). Returns `0` when pointing straight up or down.

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
