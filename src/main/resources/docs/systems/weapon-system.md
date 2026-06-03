# WeaponSystem API

`WeaponSystem` provides access to all weapon groups (cannons, beams, missiles) and ammo capacity on an entity.

## Typical usage

```lua
local entity = console.getBlock().getEntity()
local weapons = entity.getWeaponSystem()

print("Cannons:", weapons.getCannonCount())
print("Beams:", weapons.getBeamCount())
print("Missiles:", weapons.getMissileCount())
print("Missile ammo:", weapons.getMissileCapacity(), "/", weapons.getMissileCapacityMax())

for _, group in ipairs(weapons.getAllWeapons()) do
    print(group.getType(), "- DPS:", group.getDPS(), "Range:", group.getRange())
end
```

## Weapon groups

- `getCannons()`
Returns `WeaponGroup[]` for all cannon groups on the entity.

- `getBeams()`
Returns `WeaponGroup[]` for all damage beam groups on the entity.

- `getMissiles()`
Returns `WeaponGroup[]` for all missile groups on the entity.

- `getAllWeapons()`
Returns `WeaponGroup[]` combining all cannons, beams, and missiles.

## Weapon counts

- `getCannonCount()`
Number of cannon groups.

- `getBeamCount()`
Number of damage beam groups.

- `getMissileCount()`
Number of missile groups.

## Missile ammo

- `getMissileCapacity()`
Current missile ammo stored.

- `getMissileCapacityMax()`
Maximum missile ammo capacity.

- `getMissileReloadTime()`
Time in milliseconds to fully reload missile ammo.
