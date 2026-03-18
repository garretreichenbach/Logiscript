# Text Graphics API

`gfx` provides an off-screen text canvas for drawing ASCII/Unicode shapes.
By default, `render()` targets the in-dialog **canvas overlay** backend.

## Coordinate system

- Coordinates are **1-based**: top-left is `(1, 1)`.
- Drawing outside the canvas is safely clipped.
- Default canvas size is `64 x 24`.

## Reference

- `getWidth()`
Returns current canvas width.

- `getHeight()`
Returns current canvas height.

- `setSize(width, height)`
Resizes canvas (clamped to `1..240` by `1..120`). Existing pixels are preserved where possible.

- `clear()`
Clears the canvas with the current fill character (default space).

- `clear(fillChar)`
Sets the fill character and clears the canvas.

- `clear(fillChar, foreground, background)`
Clears using a fill character plus ANSI foreground/background color.

- `pixel(x, y, char)`
Draws one character at `(x, y)`.

- `pixel(x, y, char, foreground, background)`
Draws one character with explicit ANSI colors.

- `pixelScaled(x, y, char, scale)`
Draws one character and applies uniform per-pixel scale in one call.

- `pixelScaled(x, y, char, scaleX, scaleY)`
Draws one character and applies independent per-pixel X/Y scale.

- `pixelScaled(x, y, char, foreground, background, scaleX, scaleY)`
Draws one character with explicit colors and per-pixel X/Y scale.

- `line(x1, y1, x2, y2, char)`
Draws a line using Bresenham rasterization.

- `rect(x, y, width, height, char)`
Draws a rectangle outline.

- `fillRect(x, y, width, height, char)`
Draws a filled rectangle.

- `circle(cx, cy, radius, char)`
Draws a circle outline.

- `fillCircle(cx, cy, radius, char)`
Draws a filled circle.

- `text(x, y, content)`
Draws text horizontally starting at `(x, y)`.

- `setAnsiEnabled(enabled)`
Enables/disables ANSI output in `frame()` and `render()`. Disabled by default.

- `isAnsiEnabled()`
Returns whether ANSI output is enabled.

- `setForeground(color)`
Sets the current brush foreground color.

- `setBackground(color)`
Sets the current brush background color.

- `setColor(foreground)`
Sets brush foreground color.

- `setColor(foreground, background)`
Sets brush foreground and background colors.

- `resetColor()`
Resets brush colors back to terminal defaults.

- `frame()`
Returns the full canvas as a single newline-separated string.

- `render()`
  Pushes the canvas to the active backend (`canvas` by default).

- `setBackend(name)`
  Sets render backend. Supported values: `"canvas"` (default) and `"terminal"`.

- `getBackend()`
  Returns the current backend name.

- `setCellScale(scale)`
  Sets both X and Y cell scale for canvas rendering (clamped to `0.5..4.0`).

- `setCellScale(scaleX, scaleY)`
  Sets independent X/Y scale for canvas rendering (each clamped to `0.5..4.0`).

- `getCellScale()`
  Returns `{ scaleX, scaleY }`.

- `setPixelScale(x, y, scale)`
  Sets both X and Y scale for a single pixel (cell). This cell will be rendered via an overlay layer matching that scale.

- `setPixelScale(x, y, scaleX, scaleY)`
  Sets independent X/Y scale for a single pixel (cell).

- `getPixelScale(x, y)`
  Returns `{ scaleX, scaleY }` for a single pixel.

## Example

```lua
gfx.setSize(50, 18)
gfx.setAnsiEnabled(true)
gfx.clear(" ", "white", "blue")

gfx.setColor("bright_cyan", "blue")
gfx.rect(1, 1, 50, 18, "#")

gfx.setColor("yellow", "blue")
gfx.circle(25, 9, 5, "*")

gfx.setColor("bright_white", "blue")
gfx.text(16, 9, "LuaMade GFX")

gfx.pixel(3, 3, "@", "bright_red", "blue")
gfx.render()
```

## Color values

- Named colors: `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, `white`
- Bright variants: `bright_red`, `bright_blue`, etc.
- Extra aliases: `gray`/`grey` (same as `bright_black`)
- 256-color palette indexes: `0` to `255` as strings or numbers
- Reset/default: `default`, `none`, `reset`, or empty

## Notes

- Default backend is `canvas`, which draws on one or more overlay layers.
- Pixels with different scale values are grouped into separate overlay layers automatically.
- Use `setPixelScale(...)` to build mixed-density text UIs (fine text + chunky icon pixels in the same frame).
- Server/client config guard: when `gfx_canvas_backend_enabled=false`, canvas requests are forced to terminal rendering.
- Use `gfx.setBackend("terminal")` for legacy behavior that writes into terminal text contents.
- In `terminal` backend, `gfx.render()` replaces the terminal text buffer. If you want to keep a static screen, disable
  automatic prompt with `term.setAutoPrompt(false)`.
- For animation loops, redraw onto the canvas and call `gfx.render()` each frame.
- If ANSI is disabled, color metadata is stored but output is rendered as plain text.
