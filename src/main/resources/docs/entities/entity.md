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
- `getName()` / `setName(name)`
- `getPos()`
- `getSector()`
- `getSystem()`
- `getEntityType()`

## World access and nearby entities

- `getBlockAt(position)`
- `getNearbyEntities()`
- `getNearbyEntities(radius)`

## Faction and diplomacy

- `getFaction()`
- `getSystemOwner()`

## Systems and modules

- `hasReactor()`
- `getReactor()`
- `getMaxReactorHP()`
- `getReactorHP()`
- `getThrust()`
- `getShieldSystem()`
- `getShipyards()`

## Docking and rails

- `getTurrets()`
- `getDocked()`
- `isEntityDocked(remoteEntity)`
- `undockEntity(remoteEntity)`
- `undockAll()`
- `dockTo(remoteEntity, railDockerBlock)`
Dock to the nearest rail block on `remoteEntity` within range.

- `dockTo(remoteEntity, railDockerBlock, dockPos)`
Dock to a specific rail block position on `remoteEntity`.

- `dockToNearestLoadDock(remoteEntity, railDockerBlock)`
Dock to the nearest `RAIL_LOAD` block. For cargo pickup.

- `dockToNearestUnloadDock(remoteEntity, railDockerBlock)`
Dock to the nearest `RAIL_UNLOAD` block. For cargo delivery.

- `dockToNearestBasicRail(remoteEntity, railDockerBlock)`
Dock to the nearest `RAIL_BLOCK_BASIC` block.

- `dockToNearestDockerRail(remoteEntity, railDockerBlock)`
Dock to the nearest `RAIL_BLOCK_DOCKER` block.

- `dockToNearestPickupArea(remoteEntity, railDockerBlock)`
Dock to the nearest `PICKUP_AREA` block. Used for carrier launch/recovery.

- `dockToNearestPickupRail(remoteEntity, railDockerBlock)`
Dock to the nearest `PICKUP_RAIL` block.

- `dockToNearestExitShootRail(remoteEntity, railDockerBlock)`
Dock to the nearest `EXIT_SHOOT_RAIL` block.

## AI / status

- `getAI()`
- `getSpeed()`
- `getMass()`
- `isJamming()` / `canJam()` / `activateJamming(active)`
- `isCloaking()` / `canCloak()` / `activateCloaking(active)`

## Inventory / fleet

- `getNamedInventory(name)`
- `getPilot()`
- `isInFleet()`
- `getFleet()`
