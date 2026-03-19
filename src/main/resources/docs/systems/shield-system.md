# ShieldSystem API

`ShieldSystem` exposes aggregate and per-bubble shield data for an entity.

## Reference

- `isShielded()`
Returns `true` when at least one shield bubble is active.

- `getCurrent()`
Returns total current HP across all shields.

- `getCapacity()`
Returns total maximum capacity across all shields.

- `getRegen()`
Returns total regeneration rate across all shields per game tick.

- `getAllShields()`
Returns `Shield[]` for every shield bubble, active or not.

- `getActiveShields()`
Returns `Shield[]` for only the currently active bubbles.

- `isShieldActive(index: Integer)`
Returns `true` when the shield bubble at `index` is active.
