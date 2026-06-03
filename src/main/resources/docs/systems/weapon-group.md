# WeaponGroup API

`WeaponGroup` represents a single weapon group (a set of linked weapon blocks sharing a controller). Obtained from `WeaponSystem.getCannons()`, `getBeams()`, `getMissiles()`, or `getAllWeapons()`.

## Typical usage

```lua
local cannons = entity.getWeaponSystem().getCannons()
for _, cannon in ipairs(cannons) do
    print("Blocks:", cannon.getBlockCount(), "DPS:", cannon.getDPS())
    print("Range:", cannon.getRange(), "Reload:", cannon.getReloadTime(), "ms")
end
```

## Reference

- `getType()`
Returns `"CANNON"`, `"BEAM"`, or `"MISSILE"`.

- `getBlockCount()`
Total number of weapon module blocks in this group.

- `getUnits()`
Returns `WeaponStats[]` for each individual firing unit in the group.

- `getDamage()`
Total damage output per volley across all units.

- `getSpeed()`
Projectile or missile speed. Returns the speed from the first unit. Not applicable to beams (returns `0`).

- `getRange()`
Effective range. For cannons and missiles this is the raw distance; for beams this is the full beam distance.

- `getReloadTime()`
Reload time in milliseconds. Not applicable to beams (returns `0`).

- `getPowerConsumption()`
Total power consumption across all units.

- `getDPS()`
Calculated damage per second (total damage divided by reload time). Returns `0` for beams.
