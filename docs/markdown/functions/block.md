# Block API

The `Block` wrapper gives scripts access to the active computer block and nearby block state.

## Typical usage

```lua
block = console.getBlock()

pos = block.getPos()
print("Computer block at:", pos.x, pos.y, pos.z)

if block.isDisplayModule() then
  block.setDisplayText("LuaMade online")
end
```

## Common patterns

- Read metadata once with `getInfo()` and cache values used often.
- Use `hasInventory()` before `getInventory()` to avoid nil checks everywhere.
- Check `isDisplayModule()` before writing display text.
- Use `asInventory()` to access inventory-specific helper methods.

## Reference

- `getPos()`
Returns block world position as `LuaVec3i` (`x`, `y`, `z`).

- `getId()`
Returns the element ID for this block.

- `getInfo()`
Returns `BlockInfo` (name, description, ID metadata).

- `isActive()`
Returns whether the block is active.

- `setActive(active<Boolean>)`
Sets the block active state and syncs it to the world/network state.

- `getEntity()`
Returns the owning `Entity` wrapper.

- `getEntityInfo()`
Compatibility alias that returns the same `Entity` wrapper as `getEntity()`.

- `hasInventory()`
Returns true if an inventory exists at this block position.

- `getInventory()`
Returns `Inventory` or `nil` when no inventory is present.

- `asInventory()`
Returns an `InventoryBlock` wrapper when this block has an inventory, otherwise `nil`.

- `isDisplayModule()`
Returns true when the block is a text display module.

- `setDisplayText(text<String>)`
Updates text shown on display modules.

- `getDisplayText()`
Returns current display text or `nil` when not a display module.

Example:

```lua
local block = peripheral.getRelative("front")
local inv = block ~= nil and block.asInventory() or nil
if inv ~= nil then
  local items = inv.getItems()
  print("Has inventory:", inv.getInventoryName())
end
```
