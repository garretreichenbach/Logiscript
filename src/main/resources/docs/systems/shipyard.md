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
Returns the currently docked ship as an `Entity`, or `nil`.

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
Sends a shipyard server command when the build is finished.

## Command names

These are the `ShipyardCommandType` enum names present in the current StarMade build used by Logiscript:

| Command name | Notes |
| --- | --- |
| `CREATE_NEW_DESIGN` | Create a new shipyard design |
| `UNLOAD_DESIGN` | Unload the current design |
| `LOAD_DESIGN` | Load an existing design |
| `DECONSTRUCT` | Deconstruct the docked ship |
| `DECONSTRUCT_RECYCLE` | Deconstruct and recycle materials |
| `SPAWN_DESIGN` | Spawn the active design |
| `CATALOG_TO_DESIGN` | Load from catalog into design state |
| `BLUEPRINT_TO_DESIGN` | Load from blueprint into design state |
| `DESIGN_TO_BLUEPRINT` | Save the current design as a blueprint |
| `TEST_DESIGN` | Spawn/test the current design |
| `REPAIR_FROM_DESIGN` | Repair from the active design |

`sendCommand(command, ...)` expects one of these exact enum names.
