# Reactor API

`Reactor` exposes reactor health, power flow, and chamber access.

## Reference

- `getRecharge()`
Returns the energy recharge rate per game tick.

- `getConsumption()`
Returns the current power consumption per game tick.

- `getChamberCapacity()`
Returns the total number of chamber slots available on this reactor.

- `getChamber(name: String)`
Returns the `Chamber` with the given name, or `nil` when not found.

- `getChambers()`
Returns all `Chamber[]` installed on this reactor.

- `getActiveChambers()`
Returns only the currently active `Chamber[]`.

- `getMaxHP()`
Returns the reactor's maximum HP.

- `getHP()`
Returns the reactor's current HP.
