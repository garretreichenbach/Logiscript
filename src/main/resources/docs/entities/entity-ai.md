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

- `stop()`
Stops current AI movement/program action.
