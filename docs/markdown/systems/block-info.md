# BlockInfo API

`BlockInfo` is metadata for an element type. You usually get it from `block.getInfo()`, `itemStack.getInfo()`, or `shopStockEntry.getInfo()`.

## Typical usage

```lua
info = console.getBlock().getInfo()
print("Block:", info.getName())
print("Category:", info.getCategoryPath())
if info.isCapsule() then
    print("(resource capsule)")
end
```

## Reference

### Identity

- `getName()`
Returns the localized element name.

- `getFullName()`
Returns the fully-qualified element name (includes category prefix in some locales).

- `getDescription()`
Returns the element description text.

- `getId()`
Returns the numeric element ID as a `Short`.

### Categorization

- `getCategory()`
Leaf category name, e.g. `"Capsules"` or `"Cannon Barrels"`. Returns `nil` for elements without a category.

- `getCategoryPath()`
Full category path from root, e.g. `"Resources > Basic Capsules > Capsules"`.

- `getWikiCategory()`
The MediaWiki-style category used for in-game wiki documentation.

- `getInventoryGroup()`
The inventory-group string used by the stock UI for bucket sorting.

### Type flags

- `isCapsule()`
`true` for resource capsules (use this to filter stock to tradable raw materials).

- `isOre()`
`true` for raw ore blocks.

- `isShoppable()`
`true` when the element is allowed in shop trade.

- `isPlacable()`
`true` when the element can be placed as a block.

- `isVanilla()`
`true` for base-game elements (useful to distinguish mod-added items).

- `isDeprecated()`
`true` when the element is hidden from build/shop menus but still legal in data.

- `isDoor()` / `isLightSource()` / `isSignal()`
Convenience predicates for common special-purpose blocks.

- `hasInventory()`
`true` when this element carries its own inventory (cargo, factory, shop, etc.).

### Stats

- `getMaxHp()`
Returns the block's maximum hitpoints as an `Integer`.
