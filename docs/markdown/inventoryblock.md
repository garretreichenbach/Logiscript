# InventoryBlock API

`InventoryBlock` is a typed wrapper for blocks that own an inventory.

## Reference

- `getInventoryName()`
- `getItems()`
Returns `ItemStack[]` or `nil`.

- `getInventoryVolume()`
- `hasItems()`

## Notes

- You can obtain this wrapper via `peripheral.wrap(block, "inventory")`.
