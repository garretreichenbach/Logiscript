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

- `gfx.rect(x: Number, y: Number, width: Number, height: Number, r: Number, g: Number, b: Number, a: Number, filled: Boolean)`
  Draws a rectangle. `filled = true` draws a solid filled rect; `false` draws only the outline.

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
gfx.line(16, 32, w - 16, 32, 0.2, 0.9, 0.8, 1.0)
```

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
