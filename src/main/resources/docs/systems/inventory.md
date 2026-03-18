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
- `transferTo(targetInventory, itemStacks)`
Moves requested stacks when source has items and target has free slots.
