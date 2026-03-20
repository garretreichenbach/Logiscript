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

| Enum name | Lua access | Notes |
| --- | --- | --- |
| `IDLE` | Allowed | Idle fleet state |
| `MOVE_FLEET` | Allowed | Move to a target sector |
| `PATROL_FLEET` | Allowed | Patrol through sector targets |
| `TRADE_FLEET_NPC` | Internal-only | NPC trade system state; rejected by `Fleet.setCurrentCommand` |
| `TRADE_FLEET_ACTIVE` | Internal-only | Active trade system state; rejected by `Fleet.setCurrentCommand` |
| `TRADE_FLEET_WAITING` | Allowed | Trade waiting state enum exposed by the client |
| `FLEET_ATTACK` | Allowed | Attack at target sector |
| `FLEET_DEFEND` | Allowed | Defend target sector |
| `ESCORT` | Allowed | Escort/formation behavior |
| `REPAIR` | Allowed | Repair behavior |
| `STANDOFF` | Allowed | Standoff combat behavior |
| `SENTRY_FORMATION` | Allowed | Formation sentry behavior |
| `SENTRY` | Allowed | Sentry behavior |
| `FLEET_IDLE_FORMATION` | Allowed | Idle formation behavior |
| `CALL_TO_CARRIER` | Allowed | Recall to carrier |
| `MINE_IN_SECTOR` | Allowed | Mining behavior |
| `CLOAK` | Allowed | Fleet cloaking command |
| `UNCLOAK` | Allowed | Fleet uncloaking command |
| `JAM` | Allowed | Fleet jamming command |
| `UNJAM` | Allowed | Fleet unjamming command |
| `ACTIVATE_REMOTE` | Allowed | Remote activation command |
| `INTERDICT` | Allowed | Interdiction command |
| `STOP_INTERDICT` | Allowed | Stop interdiction command |

`getCommand()` can still return internal-only names when the game places a fleet into one of those states. The restriction only applies to sending commands back through `Fleet.setCurrentCommand(...)`.
