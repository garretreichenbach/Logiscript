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
- `dockTo(remoteEntity, railDockerBlock, dockPos)`

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
