# Text Graphics API

`gfx` provides an off-screen text canvas for drawing ASCII/Unicode shapes.
By default, `render()` targets the in-dialog **canvas overlay** backend.
The canvas backend supports per-cell foreground/background colors, mixed cell scaling,
and named overlay layers.

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
  Clears using a fill character plus stored color values. In `canvas` backend these
  colors render directly; in `terminal` backend they only appear in the generated text
  when ANSI is enabled.

- `pixel(x, y, char)`
Draws one character at `(x, y)`.

- `pixel(x, y, char, foreground, background)`
  Draws one character with explicit stored foreground/background colors.

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
  Enables/disables ANSI escape generation in `frame()` and in `terminal` backend output.
  This does **not** disable canvas colors.

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
  Returns the full canvas as a single newline-separated string. When ANSI is enabled,
  this string includes ANSI color escape sequences.

- `render()`
  Pushes the canvas to the active backend (`canvas` by default). In `canvas` backend,
  colors and layers render through the dialog overlay system. In `terminal` backend,
  the rendered frame replaces terminal text contents.

- `setBackend(name)`
  Sets render backend. Supported values: `"canvas"` (default) and `"terminal"`.
  If client/server config disables canvas overlays, `"canvas"` requests are forced
  to `"terminal"` at render time.

- `getBackend()`
  Returns the current backend name.

- `setCellScale(scale)`
  Sets both X and Y cell scale for the flat canvas buffer (clamped to `0.5..4.0`).

- `setCellScale(scaleX, scaleY)`
  Sets independent X/Y scale for the flat canvas buffer (each clamped to `0.5..4.0`).

- `getCellScale()`
  Returns `{ scaleX, scaleY }`.

- `setPixelScale(x, y, scale)`
  Sets both X and Y scale for a single pixel (cell) in the flat canvas buffer.
  Cells that share a scale are grouped into the same canvas overlay layer.

- `setPixelScale(x, y, scaleX, scaleY)`
  Sets independent X/Y scale for a single pixel (cell) in the flat canvas buffer.

- `getPixelScale(x, y)`
  Returns `{ scaleX, scaleY }` for a single pixel.

## Layer Management

Explicit layers let you stack multiple independent drawing surfaces on top of the flat canvas buffer. Each layer has its own pixel buffers, scale (density), and z-index.
The flat canvas buffer always remains the bottom layer.

- `createLayer(name)`
  Creates a named layer. If it already exists nothing changes.

- `removeLayer(name)`
  Deletes a named layer.

- `setLayer(name)`
  Activates a layer so all subsequent draw calls (`pixel`, `text`, `rect`, etc.) target it. Call `setLayer("")` or `clearActiveLayer()` to return to the flat buffer.

- `clearActiveLayer()`
  Deactivates any explicit layer and returns to the flat buffer.

- `clearLayer(name)`
  Clears a specific named layer to spaces without affecting the flat buffer.

- `setLayerScale(name, scale)`
  Sets uniform pixel density for a named layer.

- `setLayerScale(name, scaleX, scaleY)`
  Sets independent X/Y pixel density for a named layer.

- `getLayerScale(name)`
  Returns `{ scaleX, scaleY }` for a named layer.

- `setLayerZ(name, z)`
  Sets the z-index of a named layer (higher = on top).

- `getLayerZ(name)`
  Returns the current z-index of a named layer.

- `moveLayerUp(name)`
  Moves a layer one step toward the top of the stack.

- `moveLayerDown(name)`
  Moves a layer one step toward the bottom of the stack.

- `getLayerNames()`
  Returns an array of layer names in z-order (bottom to top).

- `hasLayer(name)`
  Returns true if a layer with that name exists.

- `getActiveLayer()`
  Returns the name of the currently active layer, or `""` when on the flat buffer.

## Example

```lua
gfx.setSize(50, 18)
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

If you want to capture the frame as ANSI-colored text instead, enable ANSI before calling
`frame()` or using the `terminal` backend:

```lua
gfx.setAnsiEnabled(true)
local text = gfx.frame()
```

## Color values

- Named colors: `black`, `red`, `green`, `yellow`, `blue`, `magenta`, `cyan`, `white`
- Bright variants: `bright_red`, `bright_blue`, etc.
- Extra aliases: `gray`/`grey` (same as `bright_black`)
- 256-color palette indexes: `0` to `255` as strings or numbers
- Reset/default: `default`, `none`, `reset`, or empty

## Backend behavior

- Default backend is `canvas`, which draws on one or more GUI overlay textures.
- `canvas` backend renders stored colors directly, even when ANSI is disabled.
- `terminal` backend writes the rendered frame into terminal text contents.
- When `gfx_canvas_backend_enabled=false`, `canvas` requests automatically fall back to
  `terminal` during `render()`.

## Limits & caveats

- Canvas size is clamped to `1..240` columns by `1..120` rows.
- Cell scale and layer scale are clamped to `0.5..4.0`.
- The canvas renderer supports up to `64` scale buckets / named layers per frame.
  Extra scale combinations are merged into a fallback layer.
- Default background is transparent. A space character with a background color still
  occupies a visible/clickable cell in the canvas UI.
- `setPixelScale(...)` only affects the flat canvas buffer. Named layers use
  `setLayerScale(...)` instead.
- In explicit-layer mode, the flat canvas buffer remains underneath all named layers.

## Notes

- Pixels with different scale values are grouped into separate overlay layers automatically.
- Use `setPixelScale(...)` to build mixed-density text UIs (fine text + chunky icon pixels in the same frame).
- Use `createLayer` / `setLayer` for fully independent named layers with per-layer pixel density.
- Named layers are rendered in z-order above the flat buffer; each gets its own GUI overlay.
- In `terminal` backend, `gfx.render()` replaces the terminal text buffer. If you want to keep a static screen, disable
  automatic prompt with `term.setAutoPrompt(false)`.
- For animation loops, redraw onto the canvas and call `gfx.render()` each frame.
- If ANSI is disabled, `frame()` returns plain text even though canvas mode still keeps and renders color metadata.
