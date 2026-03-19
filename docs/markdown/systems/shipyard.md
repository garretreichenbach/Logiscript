# Shipyard API

`Shipyard` exposes build/undock state and shipyard command control.

## Reference

- `isFinished()`
Returns `true` when the current build or refit is complete.

- `getCompletion()`
Returns build progress as a `Double` (0.0–1.0).

- `isDocked()`
Returns `true` when a ship is physically docked in the shipyard.

- `isVirtualDocked()`
Returns `true` when a blueprint ship is virtually docked (pre-build state).

- `getDocked()`
Returns the currently docked ship as a `RemoteEntity`, or `nil`.

- `canUndock()`
Returns `true` when undocking is currently permitted.

- `undock()`
Releases the docked ship from the shipyard.

- `getRequired()`
Returns the resources required to complete the build as `ItemStack[]`.

- `getCurrent()`
Returns the resources currently loaded into the shipyard as `ItemStack[]`.

- `getNeeded()`
Returns the difference between required and current resources as `ItemStack[]`.

- `sendCommand(command: String, ...)`
Sends a shipyard server command when the build is finished. Valid commands include `"BUILD"`, `"CANCEL"`, and others defined by the server.
