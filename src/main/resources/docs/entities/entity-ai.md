# EntityAI API

`EntityAI` controls high-level AI behavior for an `Entity`.

## Reference

- `setActive(active: Boolean)`
Enables (`true`) or disables (`false`) AI control for this entity.

- `isActive()`
Returns `true` when AI is currently active.

- `moveToSector(sector: LuaVec3i)`
Sets the AI's sector navigation target.

- `getTargetSector()`
Returns the current sector target as `LuaVec3i`. Returns the entity's current sector when no target is set.

- `setTarget(entity: RemoteEntity)`
Sets the combat/aim target. The target must be in the same or an adjacent sector.

- `getTarget()`
Returns the current target as `RemoteEntity`, or `nil` when no target is set.

- `getTargetType()`
Returns the exact current AI target preference name as a string.

- `setTargetType(type: String)`
Changes the AI target preference to `type`.

## Target type names

These built-in target type names are present in the current StarMade build used by Logiscript:

| Target type | Notes |
| --- | --- |
| `Any` | Default catch-all target preference |
| `Selected Target` | Use the currently selected target |
| `Ships` | Prefer ship targets |
| `Stations` | Prefer station targets |
| `Missiles` | Prefer missile targets |
| `Astronauts` | Prefer astronaut targets |
| `Asteroids` | Prefer asteroid targets |

StarMade also appends any custom AI target program names registered through `CustomAITargetUtil`, so `getTargetType()` may return additional names and `setTargetType(type)` can accept them when they are present in the running build.

- `moveToPos(pos: LuaVec3i)`
Commands the AI to move toward world block position `pos`.

- `moveToEntity(entity: RemoteEntity)`
Commands the AI to move toward `entity`. The target must be in the same sector.

- `navigateToPos(pos: LuaVec3i, stopRadius: Number)`
Moves toward `pos` and automatically stops when the entity is within `stopRadius` blocks.

- `hasReachedPos(pos: LuaVec3i, radius: Number)`
Returns `true` if the entity is currently within `radius` blocks of `pos`.

- `stop()` / `stopNavigation()`
Stops current AI movement.

## Heading and orientation

- `getHeading()`
Returns the ship's normalized forward direction as a `LuaVec3f`.

- `isAlignedWith(direction: LuaVec3f, threshold: Number)`
Returns `true` if the ship's heading dot-product with `direction` is >= `threshold` (range 0.0–1.0). Use `0.9` for roughly aligned, `0.99` for very tight.

- `isFacingTowards(entity: RemoteEntity, threshold: Number)`
Returns `true` if the ship is facing toward `entity` within the given dot-product threshold.

- `faceDirection(direction: LuaVec3f)`
Commands the AI to turn toward `direction`. Also causes the ship to begin moving in that direction — call `stopNavigation()` once aligned.

- `faceTowards(entity: RemoteEntity)`
Commands the AI to face toward `entity`. Computes direction from current position to target automatically.

## Typical alignment workflow

```lua
local station = ...
ai:faceTowards(station)
while not ai:isFacingTowards(station, 0.95) do sleep(0.5) end
ai:stopNavigation()
```
