# Thrust API

`Thrust` exposes movement capability stats.

## Reference

- `getTMR()`
Thrust-to-mass ratio.

- `getThrust()`
Current actual thrust output.

- `getMaxSpeed()`
Absolute max speed from thrust setup.

- `getSize()`
Total thruster block size.

- `startMovement()`
Starts moving the ship forward along its current heading.

- `startMovement(direction: LuaVec3f)`
Starts moving the ship in the provided world-space direction.

- `stopMovement()`
Stops AI-driven movement started through the thrust helper.
