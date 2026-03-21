# Graphics API

`gfx` is a per-computer, layered 2D drawing API for terminal overlays.

It renders directly over the terminal text UI and is constrained to the terminal panel bounds.

## Coordinate space

- Coordinates are pixel-based with origin at the terminal canvas top-left.
- `x` increases to the right, `y` increases downward.
- Any draw command outside bounds is clamped to current canvas size.

## Layers

Layers let scripts separate background, content, and interaction visuals.

- `gfx.setLayer(name: String)`
  Sets the active draw layer by name, creating it if it does not exist.
- `gfx.createLayer(name: String, order: Integer)`
  Creates or updates a layer with an explicit draw `order`. Lower values render first (behind higher layers).
- `gfx.removeLayer(name: String)`
  Removes the named layer and all its draw commands. The `"default"` layer cannot be removed.
- `gfx.getLayers()`
  Returns the names of all current layers as a `String[]`.
- `gfx.setLayerVisible(name: String, visible: Boolean)`
  Shows (`true`) or hides (`false`) a layer without removing its draw commands.
- `gfx.clearLayer(name: String)`
  Discards all draw commands in the named layer.
- `gfx.clear()`
  Discards all draw commands in every layer.
- `gfx.beginBatch()`
  Begins a batch. All subsequent `clearLayer`, `clear`, and draw commands are staged in a private
  pending buffer instead of being applied immediately.
- `gfx.commitBatch()`
  Atomically applies all staged commands to the live buffers in a single lock acquisition.
  The render thread will either see the previous complete frame or the new complete frame —
  never a partially-cleared or partially-filled intermediate state. Use this pair to eliminate
  flicker when redrawing a dynamic UI:
  ```lua
  gfx.beginBatch()
  gfx.clearLayer("widgets")
  gfx.setLayer("widgets")
  -- ... all draw calls ...
  gfx.commitBatch()
  ```

Lower `order` draws first (behind higher layers).

## Canvas

- `gfx.setCanvasSize(width, height)`
  Overrides internal canvas dimensions. Usually automatic from terminal UI.
- `gfx.getWidth()`
  Current canvas width in pixels.
- `gfx.getHeight()`
  Current canvas height in pixels.

## Drawing

- `gfx.point(x: Number, y: Number, r: Number, g: Number, b: Number, a: Number)`
  Draws a single pixel. Color channels (`r`, `g`, `b`, `a`) are normalized floats in `[0.0, 1.0]`.

- `gfx.line(x1: Number, y1: Number, x2: Number, y2: Number, r: Number, g: Number, b: Number, a: Number)`
  Draws a line from `(x1, y1)` to `(x2, y2)`.

- `gfx.line(x1, y1, x2, y2, r, g, b, a, thickness: Number)`
  Draws a line with configurable stroke thickness (`1..16`, default `1`).

- `gfx.rect(x: Number, y: Number, width: Number, height: Number, r: Number, g: Number, b: Number, a: Number, filled: Boolean)`
  Draws a rectangle. `filled = true` draws a solid filled rect; `false` draws only the outline.

- `gfx.circle(x: Number, y: Number, radius: Number, r: Number, g: Number, b: Number, a: Number, filled: Boolean, segments: Integer)`
  Draws a circle centered at `(x, y)`. `segments` controls smoothness (`8..128`, default `24`).

- `gfx.circle(x, y, radius, r, g, b, a, filled, segments, thickness: Number)`
  Draws a circle with configurable outline thickness when `filled = false`.

- `gfx.polygon(points: Number[], r: Number, g: Number, b: Number, a: Number, filled: Boolean)`
  Draws a polygon from a flat vertex list (`{x1, y1, x2, y2, ...}`). At least 3 points are required.

- `gfx.polygon(points, r, g, b, a, filled, thickness: Number)`
  Draws a polygon with configurable outline thickness when `filled = false`.

- `gfx.text(x: Number, y: Number, text: String, r: Number, g: Number, b: Number, a: Number, scale: Integer)`
  Draws text using a built-in pixel font. `scale` multiplies glyph size (`1..16`, default `1`). Supports `\n` line breaks.

- `gfx.text(x, y, text, r, g, b, a, scale, maxWidth: Integer, maxHeight: Integer, align: String, wrap: Boolean)`
  Extended text rendering with optional clipping and layout:
  - `maxWidth` / `maxHeight`: clip bounds in pixels (`nil` disables bound)
  - `align`: `"left"` (default), `"center"`, or `"right"`
  - `wrap`: when `true`, wraps text to fit `maxWidth`

- `gfx.bitmap(x: Number, y: Number, width: Integer, height: Integer, rgbaPixels: Integer[])`
  Draws packed bitmap data. `rgbaPixels` is row-major and uses `0xRRGGBBAA` per pixel.

## Limits and config

Server config controls safety caps:

- `gfx_max_layers`
- `gfx_max_commands_per_layer`

When limits are reached, additional commands/layers return `false` instead of growing unbounded.

## Example: layered terminal HUD

```lua
gfx.clear()
gfx.createLayer("bg", 0)
gfx.createLayer("widgets", 10)

local w = gfx.getWidth()
local h = gfx.getHeight()

gfx.setLayer("bg")
gfx.rect(0, 0, w, h, 0.05, 0.05, 0.08, 0.85, true)
gfx.rect(8, 8, w - 16, h - 16, 0.3, 0.5, 1.0, 0.9, false)

gfx.setLayer("widgets")
gfx.line(16, 32, w - 16, 32, 0.2, 0.9, 0.8, 1.0, 2)
gfx.circle(w * 0.5, h * 0.5, 28, 1.0, 0.5, 0.2, 0.9, false, 32, 3)
gfx.text(20, h - 24, "GFX HUD", 1.0, 1.0, 1.0, 1.0, 2, w - 40, 24, "center", true)
```

## Bitmap Helper Library (`gfxlib`)

`gfxlib` is a Lua helper module for creating bitmap payloads for `gfx.bitmap`.

```lua
local gfxlib = require("gfxlib")

local bmp = gfxlib.checkerBitmap(16, 16, 0xFFAA22FF, 0x223344CC, 2)
gfx.bitmap(8, 8, bmp.width, bmp.height, bmp.pixels)
```

Useful helpers:

- `gfxlib.pack(r, g, b, a)` / `gfxlib.unpack(color)`
- `gfxlib.newBitmap(width, height, fillColor)`
- `gfxlib.setPixel(bitmap, x, y, color)` / `gfxlib.getPixel(bitmap, x, y)`
- `gfxlib.checkerBitmap(width, height, colorA, colorB, cellSize)`
- `gfxlib.grayscaleBitmap(width, height, values, alpha)`
- `gfxlib.textMaskBitmap(rows, onColor, offColor, onChars)`
- `gfxlib.draw(gfx, x, y, bitmap)`

## Interactive UI pattern

Use `input` mouse events with `uiX`/`uiY` and `insideCanvas` for hit-testing and dragging.

```lua
input.clear()
input.consumeMouse()

local box = { x = 50, y = 40, w = 120, h = 70 }
local drag = nil

local function inside(x, y, r)
  return x and y and x >= r.x and y >= r.y and x < (r.x + r.w) and y < (r.y + r.h)
end

while true do
  gfx.clearLayer("widgets")
  gfx.setLayer("widgets")
  gfx.rect(box.x, box.y, box.w, box.h, 0.15, 0.75, 1.0, 0.95, true)

  local e = input.waitFor(33)
  if e and e.type == "mouse" then
    if e.pressed and e.button == "left" and e.insideCanvas and inside(e.uiX, e.uiY, box) then
      drag = { ox = e.uiX - box.x, oy = e.uiY - box.y }
    elseif e.released and e.button == "left" then
      drag = nil
    elseif e.dragging and drag and e.uiX and e.uiY then
      box.x = e.uiX - drag.ox
      box.y = e.uiY - drag.oy
    end
  end
end
```
