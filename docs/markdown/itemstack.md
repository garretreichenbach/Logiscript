# ItemStack API

`ItemStack` represents one stack in an inventory slot.

## Typical usage

```lua
inv = console.getBlock().getInventory()
if inv ~= nil then
  stacks = inv.getStacks()
  for i, stack in ipairs(stacks) do
    if stack ~= nil then
      info = stack.getInfo()
      print(i, info.getName(), stack.getCount())
    end
  end
end
```

## Reference

- `getId()`
Returns the item element ID.

- `getInfo()`
Returns `BlockInfo` metadata for the item type.

- `getCount()`
Returns stack quantity.
