# DisplayModule API

`DisplayModule` is a typed wrapper for text display module blocks.

## Reference

- `setText(text: String)`
Sets the text shown on the display block.

- `appendText(text: String)`
Appends `text` to the current display contents.

- `clearText()`
Clears the display text.

- `setLines(lines: String[])`
Sets the display text from multiple lines joined with newline separators.

- `getText()`
Returns the current display text as a `String`, or `nil` when the block has no text set.

- `getTextBlockIndex()`
Returns the internal text block render index as an `Integer`.

## Notes

- Obtain this wrapper via `peripheral.wrap(block, "display")`.
