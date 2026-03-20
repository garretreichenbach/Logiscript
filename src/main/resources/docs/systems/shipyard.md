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

Invalid command names, wrong argument counts, or calls made before the shipyard is ready now raise clear errors instead of failing silently.

Common argument shapes:

- `CREATE_NEW_DESIGN(command, name)` expects `name: String`.
- `LOAD_DESIGN(command, slot)` expects `slot: Integer`.
- `SPAWN_DESIGN(command, name)` expects `name: String`.
- `CATALOG_TO_DESIGN(command, owner, design)` expects `owner: String` and `design: String`.
- `BLUEPRINT_TO_DESIGN(command, owner, blueprint)` expects `owner: String` and `blueprint: String`.
- `DESIGN_TO_BLUEPRINT(command, name, factionId, style)` expects `name: String`, `factionId: Integer`, and `style: Integer`.
- `TEST_DESIGN(command, slot)` and `REPAIR_FROM_DESIGN(command, slot)` each expect `slot: Integer`.
- `UNLOAD_DESIGN` and `DECONSTRUCT_RECYCLE` take no extra arguments.

See the command table below for the full signature list.

## Command names

These are the `ShipyardCommandType` enum names present in the current StarMade build used by Logiscript:

| Command name | Expected args | Notes |
| --- | --- | --- |
| `CREATE_NEW_DESIGN` | `String` | Create a new shipyard design |
| `UNLOAD_DESIGN` | `none` | Unload the current design |
| `LOAD_DESIGN` | `Integer` | Load an existing design |
| `DECONSTRUCT` | `String` | Deconstruct the docked ship |
| `DECONSTRUCT_RECYCLE` | `none` | Deconstruct and recycle materials |
| `SPAWN_DESIGN` | `String` | Spawn the active design |
| `CATALOG_TO_DESIGN` | `String, String` | Load from catalog into design state |
| `BLUEPRINT_TO_DESIGN` | `String, String` | Load from blueprint into design state |
| `DESIGN_TO_BLUEPRINT` | `String, Integer, Integer` | Save the current design as a blueprint |
| `TEST_DESIGN` | `Integer` | Spawn/test the current design |
| `REPAIR_FROM_DESIGN` | `Integer` | Repair from the active design |

Argument types are the current upstream StarMade command signatures exposed through `sendCommand(command, ...)`.

`sendCommand(command, ...)` expects one of these exact enum names.
