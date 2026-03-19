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

## Reference

- `getPos()`
Returns block position index as `LuaVec3i` (`x`, `y`, `z`) in segment-space coordinates.

- `getWorldPos()`
Returns block world position as `LuaVec3f` (`x`, `y`, `z`) using the block's world transform.

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

- `isDisplayModule()`
Returns true when the block is a text display module.

- `setDisplayText(text<String>)`
Updates text shown on display modules.

- `getDisplayText()`
Returns current display text or `nil` when not a display module.

Example:

```lua
local block = peripheral.getRelative("front")
if block ~= nil and block.hasInventory() then
  local items = block.getItems()
  print("Has inventory:", block.getInventoryName())
end
```
