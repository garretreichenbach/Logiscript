# Inventory API

`Inventory` wraps named storage inventories.

## Typical usage

```lua
local entity = console.getBlock().getEntity()
local cargo = entity.getNamedInventory("Cargo")
if cargo ~= nil then
  print(cargo.getName(), cargo.getVolume())
  for _, stack in ipairs(cargo.getItems()) do
    if stack ~= nil then
      print(stack.getName(), stack.getCount())
    end
  end
end
```

## Reference

- `getName()`
- `getItems()`
Returns array of `ItemStack` values.

- `getVolume()`
Returns current volume used by stored items.

- `isEmpty()`
Returns `true` when no items are stored. Use to detect unloading completion.

- `isFull()`
Returns `true` when no free slots remain.

- `hasFreeSlot()`
Returns `true` if at least one slot is available.

- `getTotalItemCount()`
Returns the total number of items across all filled slots.

- `getItemCount(itemId)`
Returns the count of a specific item type by numeric ID.

- `transferTo(targetInventory, itemStacks)`
Moves requested stacks when source has items and target has free slots.

## Cargo transfer detection

Poll these methods in a loop after docking to know when a transfer is done:

```lua
-- Wait for unloading to complete (inventory drained)
while not cargo:isEmpty() do sleep(1) end

-- Wait for loading to complete (reached target count)
local CHARGE_ID = 512
local TARGET = 1000
while cargo:getItemCount(CHARGE_ID) < TARGET do sleep(1) end
```
