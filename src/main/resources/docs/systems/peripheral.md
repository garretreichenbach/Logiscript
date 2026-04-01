# Peripheral API

`peripheral` provides convenient access to nearby blocks using relative sides, plus absolute lookups by position.

## Typical usage

```lua
local topBlock = peripheral.getRelative("top")
if topBlock ~= nil then
  print("Top block id:", topBlock.getId())
end

local frontBlock = peripheral.getRelative("front")
if frontBlock ~= nil and frontBlock.isDisplayModule() then
  frontBlock.setDisplayText("Connected")
end

local wrappedDisplay = peripheral.wrap(frontBlock, "display")
if wrappedDisplay ~= nil then
  wrappedDisplay.setText("Wrapped display")
end
```

## Side names

Accepted side names:

- `front`
- `back`
- `left`
- `right`
- `top` (alias: `up`)
- `bottom` (alias: `down`)

## Reference

- `getCurrentBlock()`
Returns the current computer `Block`.

- `wrap(block<Block>, asType<String>)`
Returns a typed wrapper for `block` based on `asType`, or `nil` when incompatible.

- `wrapCurrent(asType<String>)`
Typed wrapper for the current computer block.

- `getAt(position<LuaVec3i>)`
Returns a `Block` at an absolute position, or `nil`.

- `getRelative(side<String>)`
Returns adjacent `Block` on that side, or `nil`.

- `wrapRelative(side<String>, asType<String>)`
Equivalent to `wrap(getRelative(side), asType)`.

- `wrapAt(position<LuaVec3i>, asType<String>)`
Equivalent to `wrap(getAt(position), asType)`.

- `hasRelative(side<String>)`
Returns `true` when a block exists on that side.

- `getSides()`
Returns supported side names.

## Notes

- Relative sides are resolved from the current computer block position.
- `front`/`back`/`left`/`right`/`top`/`bottom` are resolved relative to the computer block's facing/orientation.
- If no block exists at a location/side, methods return `nil` (or `false` for `hasRelative`).
- `wrap(..., asType)` supports: `display`, `inventory`, `diskdrive`, `accesspoint`, `block`/`base`, and `auto`. Other mods may register additional type names.
- `display` exposes display helpers like `setText()`/`getText()`.
- `inventory` exposes helpers like `getItems()` and `getInventoryName()`.
- `diskdrive` exposes disk methods like `saveProgram()`, `installProgram()`, and `listPrograms()`.
- `accesspoint` exposes remote access methods described below.

## Remote access point wrapper

When a remote access point block is wrapped from a computer script, it can be bound to that computer:

- `connect()`
Binds the access point to the current computer. Returns `true` on success.

- `disconnect()`
Removes the computer binding from the access point.

- `isConnected()`
Returns `true` when the access point is currently bound to a computer.

- `getLinkedComputerUUID()`
Returns the UUID of the linked computer, or `nil` when unbound.

- `getLinkedComputerName()`
Returns the linked computer display name, or `nil` when unavailable.

- `isSessionActive()`
Returns `true` when this access point currently has an active remote control session.

```lua
local access = peripheral.wrapRelative("front", "accesspoint")
if access ~= nil then
  access:connect()
end

local topBlock = peripheral.getRelative("top")

local display = peripheral.wrap(topBlock, "display")
if display ~= nil then
  display.setText("Status: OK")
end

local inv = peripheral.wrap(topBlock, "inventory")
if inv ~= nil then
  print("Inventory volume:", inv.getInventoryVolume())
end
```
