# Shipyard API

`Shipyard` exposes build/undock state and shipyard command control.

## Reference

- `isFinished()`
- `getCompletion()`
- `isDocked()`
- `isVirtualDocked()`
- `getDocked()`
Returns docked ship as `RemoteEntity`.

- `canUndock()`
- `undock()`
- `getRequired()`
Required resources as `ItemStack[]`.

- `getCurrent()`
Current loaded resources as `ItemStack[]`.

- `getNeeded()`
Difference between required and current resources.

- `sendCommand(command, ...)`
Sends shipyard server command when valid and finished.
