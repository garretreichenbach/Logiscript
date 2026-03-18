# DisplayModule API

`DisplayModule` is a typed wrapper for text display modules.

## Reference

- `setText(text)`
Alias for base `setDisplayText`.

- `getText()`
Alias for base `getDisplayText`.

- `getTextBlockIndex()`
Returns internal text block index.

## Notes

- You can obtain this wrapper via `peripheral.wrap(block, "display")`.
