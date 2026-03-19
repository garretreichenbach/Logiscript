# Fleet API

`Fleet` provides member management and command dispatch.

## Reference

- `getId()`
- `getFaction()`
- `getName()`
- `getSector()`
- `getFlagship()`
- `getMembers()`
- `addMember(remoteEntity)`
- `removeMember(remoteEntity)`
- `getCurrentCommand()`
- `setCurrentCommand(command, ...)`
Send a raw string command matching a StarMade fleet command enum name.

## Movement commands

- `moveToSector(sectorVec3i)`
Move the entire fleet to the given sector.

- `patrolSectors(sectorArray)`
Patrol between a list of at least 2 sectors.

- `attackSector(sectorVec3i)`
Move to sector and engage enemy ships.

- `defendSector(sectorVec3i)`
Move to sector and engage enemies within proximity.

## Tactical commands

- `escort()`
Follow flagship and attack nearby enemies.

- `repair()`
Disengage and begin fleet repairs.

- `idle()`
Stop all movement and combat actions.

## Notes

- `setCurrentCommand` expects command names matching StarMade fleet command enums.
- Typed movement methods (`moveToSector`, etc.) are preferred over `setCurrentCommand` for clarity and safety.
