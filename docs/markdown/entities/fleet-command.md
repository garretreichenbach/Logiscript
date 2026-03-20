# FleetCommand API

`FleetCommand` is the active command payload from a fleet.

## Reference

- `getCommand()`
Returns the exact StarMade `FleetCommandTypes` enum name as a `String`.

- `getTarget()`
Attempts to parse command args into a sector target. Returns `LuaVec3i`, or `nil` when no target is present.

- `getArgs()`
Returns raw command arguments as a `String[]`.

## Enum names

These are the enum names present in the current StarMade build used by Logiscript:

| Enum name | Expected args | Lua access | Notes |
| --- | --- | --- | --- |
| `IDLE` | `none` | Allowed | Idle fleet state |
| `MOVE_FLEET` | `LuaVec3i` | Allowed | Move to a target sector |
| `PATROL_FLEET` | `LuaVec3i[]` | Allowed | Patrol through sector targets |
| `TRADE_FLEET_NPC` | `Long` | Internal-only | NPC trade system state; rejected by `Fleet.setCurrentCommand` |
| `TRADE_FLEET_ACTIVE` | `Long, Long, Long` | Internal-only | Active trade system state; rejected by `Fleet.setCurrentCommand` |
| `TRADE_FLEET_WAITING` | `none` | Allowed | Trade waiting state enum exposed by the client |
| `FLEET_ATTACK` | `LuaVec3i` | Allowed | Attack at target sector |
| `FLEET_DEFEND` | `LuaVec3i` | Allowed | Defend target sector |
| `ESCORT` | `none` | Allowed | Escort/formation behavior |
| `REPAIR` | `none` | Allowed | Repair behavior |
| `STANDOFF` | `none` | Allowed | Standoff combat behavior |
| `SENTRY_FORMATION` | `none` | Allowed | Formation sentry behavior |
| `SENTRY` | `none` | Allowed | Sentry behavior |
| `FLEET_IDLE_FORMATION` | `none` | Allowed | Idle formation behavior |
| `CALL_TO_CARRIER` | `none` | Allowed | Recall to carrier |
| `MINE_IN_SECTOR` | `none` | Allowed | Mining behavior |
| `CLOAK` | `none` | Allowed | Fleet cloaking command |
| `UNCLOAK` | `none` | Allowed | Fleet uncloaking command |
| `JAM` | `none` | Allowed | Fleet jamming command |
| `UNJAM` | `none` | Allowed | Fleet unjamming command |
| `ACTIVATE_REMOTE` | `String, Boolean` | Allowed | Remote activation command |
| `INTERDICT` | `none` | Allowed | Interdiction command |
| `STOP_INTERDICT` | `none` | Allowed | Stop interdiction command |

Argument types are the current upstream StarMade command signatures. `LuaVec3i` and `LuaVec3i[]` correspond to StarMade's internal `Vector3i` and `Vector3i[]` types.

`getCommand()` can still return internal-only names when the game places a fleet into one of those states. The restriction only applies to sending commands back through `Fleet.setCurrentCommand(...)`.
