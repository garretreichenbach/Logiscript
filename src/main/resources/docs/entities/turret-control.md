# TurretControl API

`TurretControl` lets a script aim and fire a turret by hand ("free aim") instead of leaving it to the turret's AI. Get one with `console.getTurret(entity)`; the turret must be docked in the computer's own rail tree. For a two-axis turret (a base with a barrel docked on it) you can pass either part; the barrel, which aims both axes, is used.

Aiming switches the turret's AI on if needed, then overrides its targeting. The turret turns at its normal rail speed and fires its weapons at their normal rate. It returns to AI control when you call `release()`, or after **3 seconds** without any `aim`/`aimAt`/`fire` call, so a stopped or crashed script never leaves a turret stuck.

Directions and points are world coordinates in the turret's sector, the same space as `Entity.getPos()`.

## Typical usage

Pair it with [Remote Control controller mode](../io/input.md#remote-control) and a camera on the turret to steer it with the mouse:

```lua
local ship = console.getBlock().getEntity()
local turret = console.getTurret(ship.getTurrets()[1])
console.setCamera(turret.getEntity().getBlockAt(vec3i(16, 17, 18))) -- a Camera block on the barrel

local yaw, pitch = 0, 0
while true do
  local e = input.waitFor(100)
  if e and e.type == "mouse" then
    yaw = yaw - e.dx * 0.004
    pitch = math.max(-1.2, math.min(1.2, pitch - e.dy * 0.004))
    if e.button == "left" then turret.fire(e.pressed) end
  end
  -- keep aiming every loop, even with no input, so control doesn't time out
  turret.aim(vec3f(math.sin(yaw) * math.cos(pitch), math.sin(pitch), math.cos(yaw) * math.cos(pitch)))
end
```

## Reference

- `aim(direction: LuaVec3f)`
Points the turret along a world-space direction. It doesn't need to be normalized, but it must not be zero.

- `aimAt(point: LuaVec3f)`
Points the turret at a world-space point.

- `fire(firing: Boolean)`
Holds (`true`) or releases (`false`) the trigger. While held, the turret fires its weapons along its aim at their normal rate.

- `release()`
Hands the turret back to its AI.

- `isManual()`
Returns `true` while the script has control.

- `getFacing()`
Returns the turret's current world-space forward direction, which lags behind the aim while it turns.

- `getEntity()`
Returns the turret `Entity` being aimed.
