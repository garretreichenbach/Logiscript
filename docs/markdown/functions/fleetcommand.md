# FleetCommand API

`FleetCommand` is the active command payload from a fleet.

## Reference

- `getCommand()`
Returns command enum name.

- `getTarget()`
Attempts to parse command args into a sector target (`LuaVec3i`).

- `getArgs()`
Returns raw command arguments as strings.
