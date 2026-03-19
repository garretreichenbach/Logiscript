# RemoteEntity API

`RemoteEntity` is a restricted, mostly read-only wrapper for nearby entities returned by scans and targeting methods.

## Reference

- `getId()`
Returns the numeric entity ID.

- `getName()`
Returns the entity's display name.

- `getFaction()`
Returns the `Faction` this entity belongs to.

- `getSpeed()`
Returns current speed in blocks/s as a `Double`.

- `getMass()`
Returns total mass as a `Double`.

- `getPos()`
Returns current world position as `LuaVec3f`.

- `getSector()`
Returns current sector coordinates as `LuaVec3i`.

- `getSystem()`
Returns current solar system coordinates as `LuaVec3i`.

- `getShieldSystem()`
Returns the `ShieldSystem` wrapper.

- `getEntityType()`
Returns the entity type string: `"SHIP"` or `"SPACE_STATION"`.

- `getPilot()`
Returns the pilot's player name, or `nil` when unoccupied.

## Notes

- Use this wrapper for scan/targeting/intel flows where full control is not needed.
