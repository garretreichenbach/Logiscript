# Fleet API

`Fleet` provides member management and command dispatch.

## Reference

- `getId()`
Returns the numeric fleet ID.

- `getFaction()`
Returns the `Faction` this fleet belongs to.

- `getName()`
Returns the fleet's display name.

- `getSector()`
Returns the fleet's current sector as `LuaVec3i`.

- `getFlagship()`
Returns the flagship as a `RemoteEntity`.

- `getMembers()`
Returns all fleet members as `RemoteEntity[]`.

- `addMember(entity: RemoteEntity)`
Adds `entity` to this fleet.

- `removeMember(entity: RemoteEntity)`
Removes `entity` from this fleet.

- `getCurrentCommand()`
Returns the active `FleetCommand` payload.

- `setCurrentCommand(command: String, ...)`
Sends a raw command by enum name with optional extra arguments. Only client-accessible StarMade fleet enums are accepted. Internal-only enums are rejected. Typed command methods below are preferred.

## Movement commands

- `moveToSector(sector: LuaVec3i)`
Move the entire fleet to `sector`.

- `patrolSectors(sectors: LuaVec3i[])`
Patrol between a list of at least 2 sector coordinates in order.

- `attackSector(sector: LuaVec3i)`
Move to `sector` and engage enemy ships on arrival.

- `defendSector(sector: LuaVec3i)`
Move to `sector` and engage enemies within proximity.

## Tactical commands

- `escort()`
Follow the flagship and attack nearby enemies.

- `repair()`
Disengage and begin fleet repairs.

- `idle()`
Stop all movement and combat actions.

## Notes

- `setCurrentCommand` accepts these upstream enum names: `IDLE`, `MOVE_FLEET`, `PATROL_FLEET`, `TRADE_FLEET_WAITING`, `FLEET_ATTACK`, `FLEET_DEFEND`, `ESCORT`, `REPAIR`, `STANDOFF`, `SENTRY_FORMATION`, `SENTRY`, `FLEET_IDLE_FORMATION`, `CALL_TO_CARRIER`, `MINE_IN_SECTOR`, `CLOAK`, `UNCLOAK`, `JAM`, `UNJAM`, `ACTIVATE_REMOTE`, `INTERDICT`, `STOP_INTERDICT`.
- Internal-only StarMade enums are rejected by `setCurrentCommand`: `TRADE_FLEET_NPC`, `TRADE_FLEET_ACTIVE`.
- Typed movement methods (`moveToSector`, etc.) are preferred over `setCurrentCommand` for clarity and safety.
