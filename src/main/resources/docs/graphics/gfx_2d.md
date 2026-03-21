# Graphics API

`gfx_2d` is a per-computer, layered 2D drawing API for terminal overlays.

It renders directly over the terminal text UI and is constrained to the terminal panel bounds.

## Coordinate space

- Coordinates are pixel-based with origin at the terminal canvas top-left.
- `x` increases to the right, `y` increases downward.
- Any draw command outside bounds is clamped to current canvas size.
- By default, commands are authored in a logical canvas and automatically scaled to the current terminal viewport.

## Automatic UI scaling

- Auto scaling is enabled by default.
- On first render, the logical canvas is initialized from the current viewport size.
- If the terminal viewport later changes (window resize/UI scale changes), existing draw coordinates are scaled
  automatically.
- Mouse `input` `uiX/uiY` values are mapped back into the logical canvas for consistent hit-testing.

Use these methods to control behavior:

- `gfx_2d.setAutoScale(enabled: Boolean)`
  Enables/disables automatic render scaling.
- `gfx_2d.isAutoScaleEnabled()`
  Returns current auto-scaling state.
- `gfx_2d.resetCanvasSize()`
  Clears a custom logical canvas size set by script.
- `gfx_2d.getViewportWidth()` / `gfx_2d.getViewportHeight()`
  Returns current physical viewport size in pixels.
- `gfx_2d.getScaleX()` / `gfx_2d.getScaleY()`
  Returns logical-to-viewport scaling factors.

## Layers

Layers let scripts separate background, content, and interaction visuals.

- `gfx_2d.setLayer(name: String)`
  Sets the active draw layer by name, creating it if it does not exist.
- `gfx_2d.createLayer(name: String, order: Integer)`
  Creates or updates a layer with an explicit draw `order`. Lower values render first (behind higher layers).
- `gfx_2d.removeLayer(name: String)`
  Removes the named layer and all its draw commands. The `"default"` layer cannot be removed.
- `gfx_2d.getLayers()`
  Returns the names of all current layers as a `String[]`.
- `gfx_2d.setLayerVisible(name: String, visible: Boolean)`
  Shows (`true`) or hides (`false`) a layer without removing its draw commands.
- `gfx_2d.clearLayer(name: String)`
  Discards all draw commands in the named layer.
- `gfx_2d.clear()`
  Discards all draw commands in every layer.
- `gfx_2d.beginBatch()`
  Begins a batch. All subsequent `clearLayer`, `clear`, and draw commands are staged in a private
  pending buffer instead of being applied immediately.
- `gfx_2d.commitBatch()`
  Atomically applies all staged commands to the live buffers in a single lock acquisition.
  The render thread will either see the previous complete frame or the new complete frame —
  never a partially-cleared or partially-filled intermediate state. Use this pair to eliminate
  flicker when redrawing a dynamic UI:
  ```lua
  gfx_2d.beginBatch()
  gfx_2d.clearLayer("widgets")
  gfx_2d.setLayer("widgets")
  -- ... all draw calls ...
  gfx_2d.commitBatch()
  ```

Lower `order` draws first (behind higher layers).

## Canvas

- `gfx_2d.setCanvasSize(width, height)`
  Overrides internal canvas dimensions. Usually automatic from terminal UI.
- `gfx_2d.getWidth()`
  Current canvas width in pixels.
- `gfx_2d.getHeight()`
  Current canvas height in pixels.

## Drawing

- `gfx_2d.point(x: Number, y: Number, r: Number, g: Number, b: Number, a: Number)`
  Draws a single pixel. Color channels (`r`, `g`, `b`, `a`) are normalized floats in `[0.0, 1.0]`.

- `gfx_2d.line(x1: Number, y1: Number, x2: Number, y2: Number, r: Number, g: Number, b: Number, a: Number)`
  Draws a line from `(x1, y1)` to `(x2, y2)`.

- `gfx_2d.line(x1, y1, x2, y2, r, g, b, a, thickness: Number)`
  Draws a line with configurable stroke thickness (`1..16`, default `1`).

-
`gfx_2d.rect(x: Number, y: Number, width: Number, height: Number, r: Number, g: Number, b: Number, a: Number, filled: Boolean)`
Draws a rectangle. `filled = true` draws a solid filled rect; `false` draws only the outline.

-
`gfx_2d.circle(x: Number, y: Number, radius: Number, r: Number, g: Number, b: Number, a: Number, filled: Boolean, segments: Integer)`
Draws a circle centered at `(x, y)`. `segments` controls smoothness (`8..128`, default `24`).

- `gfx_2d.circle(x, y, radius, r, g, b, a, filled, segments, thickness: Number)`
  Draws a circle with configurable outline thickness when `filled = false`.

- `gfx_2d.polygon(points: Number[], r: Number, g: Number, b: Number, a: Number, filled: Boolean)`
  Draws a polygon from a flat vertex list (`{x1, y1, x2, y2, ...}`). At least 3 points are required.

- `gfx_2d.polygon(points, r, g, b, a, filled, thickness: Number)`
  Draws a polygon with configurable outline thickness when `filled = false`.

- `gfx_2d.text(x: Number, y: Number, text: String, r: Number, g: Number, b: Number, a: Number, scale: Integer)`
  Draws text using a built-in pixel font. `scale` multiplies glyph size (`1..16`, default `1`). Supports `\n` line
  breaks.

- `gfx_2d.text(x, y, text, r, g, b, a, scale, maxWidth: Integer, maxHeight: Integer, align: String, wrap: Boolean)`
  Extended text rendering with optional clipping and layout:
  - `maxWidth` / `maxHeight`: clip bounds in pixels (`nil` disables bound)
  - `align`: `"left"` (default), `"center"`, or `"right"`
  - `wrap`: when `true`, wraps text to fit `maxWidth`

- `gfx_2d.bitmap(x: Number, y: Number, width: Integer, height: Integer, rgbaPixels: Integer[])`
  Draws packed bitmap data. `rgbaPixels` is row-major and uses `0xRRGGBBAA` per pixel.

## Limits and config

Server config controls safety caps:

- `gfx_2d_max_layers`
- `gfx_2d_max_commands_per_layer`

When limits are reached, additional commands/layers return `false` instead of growing unbounded.

## Example: layered terminal HUD

```lua
gfx_2d.clear()
gfx_2d.createLayer("bg", 0)
gfx_2d.createLayer("widgets", 10)

local w = gfx_2d.getWidth()
local h = gfx_2d.getHeight()

gfx_2d.setLayer("bg")
gfx_2d.rect(0, 0, w, h, 0.05, 0.05, 0.08, 0.85, true)
gfx_2d.rect(8, 8, w - 16, h - 16, 0.3, 0.5, 1.0, 0.9, false)

gfx_2d.setLayer("widgets")
gfx_2d.line(16, 32, w - 16, 32, 0.2, 0.9, 0.8, 1.0, 2)
gfx_2d.circle(w * 0.5, h * 0.5, 28, 1.0, 0.5, 0.2, 0.9, false, 32, 3)
gfx_2d.text(20, h - 24, "gfx_2d HUD", 1.0, 1.0, 1.0, 1.0, 2, w - 40, 24, "center", true)
```

## Bitmap Helper Library (`gfx_2dlib`)

`gfx_2dlib` is a Lua helper module for creating bitmap payloads for `gfx_2d.bitmap`.

```lua
local gfx_2dlib = require("gfx_2dlib")

local bmp = gfx_2dlib.checkerBitmap(16, 16, 0xFFAA22FF, 0x223344CC, 2)
gfx_2d.bitmap(8, 8, bmp.width, bmp.height, bmp.pixels)
```

Useful helpers:

- `gfx_2dlib.pack(r, g, b, a)` / `gfx_2dlib.unpack(color)`
- `gfx_2dlib.newBitmap(width, height, fillColor)`
- `gfx_2dlib.setPixel(bitmap, x, y, color)` / `gfx_2dlib.getPixel(bitmap, x, y)`
- `gfx_2dlib.checkerBitmap(width, height, colorA, colorB, cellSize)`
- `gfx_2dlib.grayscaleBitmap(width, height, values, alpha)`
- `gfx_2dlib.textMaskBitmap(rows, onColor, offColor, onChars)`
- `gfx_2dlib.draw(gfx_2d, x, y, bitmap)`

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
  gfx_2d.clearLayer("widgets")
  gfx_2d.setLayer("widgets")
  gfx_2d.rect(box.x, box.y, box.w, box.h, 0.15, 0.75, 1.0, 0.95, true)

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
