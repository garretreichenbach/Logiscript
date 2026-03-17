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

- `getAt(position<LuaVec3i>)`
Returns a `Block` at an absolute position, or `nil`.

- `getRelative(side<String>)`
Returns adjacent `Block` on that side, or `nil`.

- `hasRelative(side<String>)`
Returns `true` when a block exists on that side.

- `getSides()`
Returns supported side names.

## Notes

- Relative sides are resolved from the current computer block position.
- If no block exists at a location/side, methods return `nil` (or `false` for `hasRelative`).
- Display modules are returned as a specialized `DisplayModuleBlock` wrapper, so helper methods like `setText()`/`getText()` are available.
- Inventory-capable blocks are returned as a specialized `InventoryBlock` wrapper, with helper methods like `getItems()` and `getInventoryName()`.

```lua
local topBlock = peripheral.getRelative("top")
local display = topBlock ~= nil and topBlock.asDisplayModule() or nil
if display ~= nil then
  display.setText("Status: OK")
end

local inv = topBlock ~= nil and topBlock.asInventory() or nil
if inv ~= nil then
  print("Inventory volume:", inv.getInventoryVolume())
end
```
