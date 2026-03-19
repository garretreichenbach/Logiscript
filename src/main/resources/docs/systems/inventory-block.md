# InventoryBlock API

`InventoryBlock` is a typed wrapper for blocks that own an inventory.

## Reference

- `getInventoryName()`
Returns the inventory's custom label as a `String`.

- `getItems()`
Returns `ItemStack[]` for all stored stacks, or `nil` when the inventory is empty.

- `getInventoryVolume()`
Returns the current volume used by stored items as a `Double`.

- `hasItems()`
Returns `true` when at least one item stack is present.

## Notes

- Obtain this wrapper via `peripheral.wrap(block, "inventory")`.
