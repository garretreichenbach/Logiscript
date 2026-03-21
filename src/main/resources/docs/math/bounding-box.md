# BoundingBox API

`BoundingBox` is a snapshot of an entity's local block-space bounds.

You usually obtain it from `Entity.getBoundingBox()` or `RemoteEntity.getBoundingBox()`.

## Reference

- `getMin()`
  Returns the minimum local-space corner as `LuaVec3f`.

- `getMax()`
  Returns the maximum local-space corner as `LuaVec3f`.

- `getCenter()`
  Returns the midpoint between `min` and `max` as `LuaVec3f`.

- `getDimensions()`
  Returns the local-space size `(max - min)` as `LuaVec3f`.

## Notes

- These values come from the game's internal `SegmentController.getBoundingBox()` method.
- Coordinates are local to the entity, not world-transformed.
- Combine this with `getPos()` / `getHeading()` if you need world-space logic around the entity.
