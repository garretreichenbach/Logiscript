# EntityAI API

`EntityAI` controls high-level AI behavior for an `Entity`.

## Reference

- `setActive(active)`
Enables or disables AI control.

- `isActive()`
Returns whether AI is currently active.

- `moveToSector(sectorVec3i)`
Sets sector target for AI navigation.

- `getTargetSector()`
Gets current sector target.

- `setTarget(remoteEntity)`
Sets combat/aim target when valid and nearby.

- `getTarget()`
Returns current target as `RemoteEntity` or `nil`.

- `getTargetType()`
Returns current target preference.

- `setTargetType(type)`
Changes target preference.

- `moveToPos(posVec3i)`
Moves toward world position.

- `moveToEntity(remoteEntity)`
Moves toward a target entity in same sector.

- `navigateToPos(posVec3i, stopRadius)`
Moves toward position and automatically stops when within `stopRadius` blocks.

- `hasReachedPos(posVec3i, radius)`
Returns `true` if entity is currently within `radius` blocks of `pos`.

- `stop()` / `stopNavigation()`
Stops current AI movement.

## Heading and orientation

- `getHeading()`
Returns the ship's current forward direction snapped to the nearest cardinal axis as a `Vec3i` (e.g. `(0,0,1)` = facing +Z).

- `isAlignedWith(directionVec3i, threshold)`
Returns `true` if the ship's heading dot-product with `direction` is >= `threshold` (0.0–1.0). Use `0.9` for roughly aligned, `0.99` for very tight.

- `isFacingTowards(remoteEntity, threshold)`
Returns `true` if the ship is facing toward `remoteEntity` within the given dot-product threshold.

- `faceDirection(directionVec3i)`
Commands the AI to turn toward the given direction vector. Also causes the ship to begin moving in that direction — call `stopNavigation()` once aligned.

- `faceTowards(remoteEntity)`
Commands the AI to face toward another entity. Equivalent to `faceDirection` computed from the current position to the entity.

## Typical alignment workflow

```lua
local station = ...
ai:faceTowards(station)
while not ai:isFacingTowards(station, 0.95) do sleep(0.5) end
ai:stopNavigation()
```
