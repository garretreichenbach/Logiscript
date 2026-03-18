# BlockInfo API

`BlockInfo` is metadata for an element type. You usually get it from `block.getInfo()` or `itemStack.getInfo()`.

## Typical usage

```lua
info = console.getBlock().getInfo()
print("Block:", info.getName())
print("ID:", info.getId())
print(info.getDescription())
```

## Reference

- `getName()`
Returns the localized element name.

- `getDescription()`
Returns the element description text.

- `getId()`
Returns the numeric element ID.
