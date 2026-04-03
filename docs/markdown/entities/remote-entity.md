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

- `getHeading()`
  Returns normalized forward heading as `LuaVec3f`.

- `getUp()`
  Returns normalized up vector as `LuaVec3f`.

- `getRoll()`
  Returns the current roll angle in radians relative to galactic up (world Y axis). Returns `0` when pointing straight up or down.

- `getMass()`
Returns total mass as a `Double`.

- `getPos()`
Returns current world position as `LuaVec3f`.

- `getBoundingBox()`
  Returns the entity's local-space `BoundingBox` snapshot from the game's internal `SegmentController.getBoundingBox()`
  method.

- `getSector()`
Returns current sector coordinates as `LuaVec3i`.

- `getSystem()`
Returns current solar system coordinates as `LuaVec3i`.

`BoundingBox` exposes:

- `getMin()`
  Returns the local-space minimum corner as `LuaVec3f`.

- `getMax()`
  Returns the local-space maximum corner as `LuaVec3f`.

- `getCenter()`
  Returns the midpoint between `min` and `max` as `LuaVec3f`.

- `getDimensions()`
  Returns the local-space size `(max - min)` as `LuaVec3f`.

- `getShieldSystem()`
Returns the `ShieldSystem` wrapper.

- `getEntityType()`
Returns the entity type string: `"SHIP"` or `"SPACE_STATION"`.

- `getPilot()`
Returns the pilot's player name, or `nil` when unoccupied.

## Notes

- Use this wrapper for scan/targeting/intel flows where full control is not needed.
