# FleetCommand API

`FleetCommand` is the active command payload from a fleet.

## Reference

- `getCommand()`
Returns the command enum name as a `String` (e.g. `"MOVE_FLEET"`, `"PATROL_FLEET"`).

- `getTarget()`
Attempts to parse command args into a sector target. Returns `LuaVec3i`, or `nil` when no target is present.

- `getArgs()`
Returns raw command arguments as a `String[]`.
