# DisplayModule API

`DisplayModule` is a typed wrapper for text display module blocks.

## Reference

- `setText(text: String)`
Sets the text shown on the display block.

- `getText()`
Returns the current display text as a `String`, or `nil` when the block has no text set.

- `getTextBlockIndex()`
Returns the internal text block render index as an `Integer`.

## Notes

- Obtain this wrapper via `peripheral.wrap(block, "display")`.
