# RemoteEntity API

`RemoteEntity` is a restricted, mostly read-only wrapper for nearby entities.

## Reference

- `getId()`
- `getName()`
- `getFaction()`
- `getSpeed()`
- `getMass()`
- `getPos()`
- `getSector()`
- `getSystem()`
- `getShieldSystem()`
- `getEntityType()`
- `getNamedInventory(name)`
- `getPilot()`

## Notes

- Use this wrapper for scan/targeting/intel flows where full control is not needed.
- `getNamedInventory` may return `nil` depending on faction relationship and target type.
