# WeaponStats API

`WeaponStats` exposes per-unit stats for an individual firing unit within a weapon group. Obtained from `WeaponGroup.getUnits()`.

## Reference

- `getType()`
Returns `"CANNON"`, `"BEAM"`, or `"MISSILE"`.

- `getDamage()`
Effective damage per shot/tick (includes effect bonuses).

- `getBaseDamage()`
Base damage before effect bonuses.

- `getSpeed()`
Projectile or missile speed. Returns `0` for beams.

- `getRange()`
Effective range in game units.

- `getReloadTime()`
Reload time in milliseconds. Returns `0` for beams.

- `getPowerConsumption()`
Power consumed per shot/activation (includes effect modifiers).

- `getBasePowerConsumption()`
Base power consumption before effect modifiers.

- `getPowerPerSecond()`
Power consumed per second while charging/firing.

- `getTickRate()`
Beam tick rate (beam-only). Returns `0` for cannons and missiles.

- `getImpactForce()`
Impact force applied on hit (cannon-only). Returns `0` for beams and missiles.

- `getRecoil()`
Recoil force applied to the firing entity (cannon-only). Returns `0` for beams and missiles.
