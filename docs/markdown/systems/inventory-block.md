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

- `addItems(itemStacks)`
Adds requested stacks into the block inventory.

- `removeItems(itemStacks)`
Removes requested stacks from the block inventory when enough items are present.

- `transferItemsTo(targetInventory, itemStacks)`
Moves requested stacks from this block inventory into `targetInventory`.

- `transferItemsFrom(sourceInventory, itemStacks)`
Pulls requested stacks from `sourceInventory` into this block inventory.

- `clearItems()`
Removes all items from the block inventory.

## Notes

- Obtain this wrapper via `peripheral.wrap(block, "inventory")`.
