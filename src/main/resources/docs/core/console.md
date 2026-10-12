# Console API

`console` is your script's output and context entry point.

## Typical usage

```lua
console.print("Booting...")

block = console.getBlock()
info = block.getInfo()
console.print("Running on " .. info.getName())

console.print("Now: " .. tostring(console.getTime()))
```

## Reference

- `getTime()`
Returns current time in milliseconds.

- `getBlock()`
Returns the computer `Block` currently running the script.

- `setCamera(cameraBlock)`
Shows players in Remote Control controller mode the view from a vanilla Camera block (ID 47), with a CCTV overlay. The camera must be on this computer's entity or on something docked in the same rail tree, such as a turret; other entities are refused. Pass `nil` to clear. See [Remote Control](../io/input.md#remote-control).

- `clearCamera()`
Stops showing a camera feed; controller mode goes back to the normal view.

- `getCamera()`
Returns the current camera `Block`, or `nil`.

- `getTurret(entity)`
Returns a [TurretControl](../entities/turret-control.md) for free-aiming a turret docked in this computer's rail tree. Turrets anywhere else are refused.

- `print(value)`
Prints a value to terminal output (appends newline).

## Notes

- `print(...)` is also globally available and mapped to `console.print(...)`.
- Convert non-string values with `tostring(...)` when you need exact formatting.
