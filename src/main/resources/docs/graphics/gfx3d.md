# Projector Graphics API (`gfx3d`)

`gfx3d` is a **world-space** vector graphics API. Unlike `gfx2d` (which draws on the terminal UI of the
viewing player), a projector renders 2D/3D vector graphics **in the world**, anchored to a Projector block, and
those graphics are **server-authoritative and shown identically to every player near the block** — including
players who never opened the computer.

You obtain the API by wrapping a Projector block as a peripheral, build a frame, then publish it:

```lua
local proj  = peripherals.wrap(peripherals.getRelative("front"), "projector")
local frame = proj.newFrame()

frame.setLayer("grid")
for i = -5, 5 do frame.line3d(i,0,-5, i,0,5, 0.3,0.3,0.3,1.0) end

frame.setLayer("map")
local surf = frame.newSurface(0,3,0,  0,1,0,  0,0,1,  4.0,4.0,  128,128) -- 4x4-block plane, 3 above, 128px canvas
surf.rect(0,0,128,128, 0.02,0.05,0.1,0.8, true)
surf.circle(64,64,40, 0.2,0.9,0.5,1.0, false, 32)
surf.text(6,6,"SECTOR MAP", 1,1,1,1, 1)

proj.publish(frame)   -- now visible to every player near the ship
```

## Coordinate space

- Coordinates are **block-space floats relative to the projector block**. `1.0` = one block.
- The origin `(0,0,0)` is the projector block; `+x`/`+y`/`+z` follow the projector's own orientation, so the
  whole projection **rotates with the ship** — it is not world-axis-locked.
- Every coordinate is clamped to **±5 blocks per axis** (configurable down, never up — see Limits). Out-of-range
  values are clamped, not rejected, matching `gfx2d`'s clamp-don't-reject philosophy. The server re-clamps
  authoritatively.
- Color channels (`r`, `g`, `b`, `a`) are normalized floats in `[0.0, 1.0]`, exactly like `gfx2d`.

Query the effective clamp with `proj.getMaxOffset()`.

## Obtaining a projector and building frames

- `peripherals.wrap(block, "projector")`
  Wraps a Projector block as a peripheral.
- `proj.newFrame()`
  Returns a fresh, empty frame (a `gfx3d` object) to draw into.
- `proj.publish(frame)`
  Publishes the frame. It becomes visible to every nearby player and persists across restart. Publishing swaps
  the whole frame atomically, so bystanders never see a half-drawn projection.
- `proj.clear()`
  Clears the projector so nothing renders.
- `proj.getMaxOffset()`
  Returns the current per-axis offset clamp in blocks.

Because computer scripts execute **server-side**, `publish` is a local handoff — there is no blocking network
round-trip. Republish whenever the projection changes (this model suits maps and HUDs, not 60fps animation).

## Layers

Layers work like `gfx2d`: they group commands and control draw order (lower `order` draws first).

- `frame.setLayer(name)` — sets/creates the active layer.
- `frame.createLayer(name, order)` — creates or reorders a layer.
- `frame.removeLayer(name)` — removes a layer (the `"default"` layer cannot be removed).
- `frame.getLayers()` — returns all layer names as a `String[]`.
- `frame.setLayerVisible(name, visible)` — shows/hides a layer.
- `frame.clearLayer(name)` / `frame.clear()` — discards commands (and surfaces) in one/all layers.
- `frame.beginBatch()` / `frame.commitBatch()` — provided for `gfx2d` parity. A frame is invisible until
  `publish`, so publishing is already atomic; batching here only affects the internal revision counter.

## 3D primitives

Positions come first, then `r,g,b,a`, then shape args, with optional trailing `thickness` / `filled` overloads —
matching `gfx2d` conventions.

- `frame.point3d(ox,oy,oz, r,g,b,a)`
  A single point.

- `frame.line3d(ox1,oy1,oz1, ox2,oy2,oz2, r,g,b,a[, thickness])`
  A line between two points. `thickness` is the stroke width (`1..16`, default `1`).

- `frame.boxWire(ox,oy,oz, w,h,d, r,g,b,a[, thickness])`
  A wireframe box with corner `(ox,oy,oz)` and size `(w,h,d)`.

- `frame.boxFilled(ox,oy,oz, w,h,d, r,g,b,a)`
  A solid box.

- `frame.polyline3d(points[], r,g,b,a[, thickness, closed])`
  A connected line through a flat list of `{x1,y1,z1, x2,y2,z2, ...}` points. `closed = true` joins the last
  point back to the first. At least 2 points (6 values) are required.

- `frame.triangle3d(ox1,oy1,oz1, ox2,oy2,oz2, ox3,oy3,oz3, r,g,b,a[, filled])`
  A triangle. `filled = true` draws a solid triangle; otherwise the outline.

## 2D surfaces in 3D

A **surface** is a flat 2D vector canvas floating in 3D — the projector's version of drawing on a panel, ideal
for in-world maps and HUD readouts.

- `frame.newSurface(ox,oy,oz, nx,ny,nz, ux,uy,uz, worldW,worldH[, canvasW,canvasH])`
  Creates a surface attached to the active layer and returns it.
  - `(ox,oy,oz)` — the surface's top-left corner, in block-space relative to the projector.
  - `(nx,ny,nz)` — the plane normal (which way the surface faces).
  - `(ux,uy,uz)` — the up vector; combined with the normal it fixes the plane's orientation. Must **not** be
    parallel to the normal (a degenerate surface is skipped).
  - `worldW,worldH` — the size of the plane in blocks.
  - `canvasW,canvasH` — the logical canvas size that 2D commands are authored against (default `128 × 128`).

Canvas `(x, y)` maps to the plane with **`x` running right and `y` running down** from the origin corner — the
same top-left origin, y-down convention as `gfx2d`.

Surfaces expose the familiar 2D primitives (identical signatures to `gfx2d`), in canvas coordinates:

- `surf.point(x,y, r,g,b,a)`
- `surf.line(x1,y1,x2,y2, r,g,b,a[, thickness])`
- `surf.rect(x,y,w,h, r,g,b,a, filled)`
- `surf.circle(x,y,radius, r,g,b,a, filled, segments[, thickness])`
- `surf.polygon(points[], r,g,b,a, filled[, thickness])`
- `surf.text(x,y,str, r,g,b,a, scale[, maxWidth,maxHeight,align,wrap])`
- `surf.getCanvasWidth()` / `surf.getCanvasHeight()`

Surface text uses a built-in 5×7 vector font (uppercase letters, digits, and common punctuation; lowercase is
drawn as uppercase). `scale` multiplies glyph size. In v1 the extended text arguments
(`maxWidth`/`maxHeight`/`align`/`wrap`) are accepted for `gfx2d` parity but only `scale` affects the output —
text is left-aligned and does not wrap.

## Limits and config

Server config controls safety caps. When a limit is reached, additional commands/surfaces return `false` instead
of growing unbounded (creating a surface past the cap raises an error).

- `projector_max_commands_per_frame` — 3D commands per frame across all layers (default `2048`).
- `projector_max_surfaces_per_frame` — surfaces per frame (default `32`).
- `projector_max_commands_per_surface` — 2D commands per surface (default `1024`).
- `projector_max_layers` — layers per frame (default `16`).
- `projector_max_offset_blocks` — per-axis offset clamp in blocks (default and hard cap `5.0`; config may only
  lower it).

## Persistence and visibility

- The projection is **server-authoritative**: the script runs on the server, so the frame it builds is the source
  of truth. Publishing syncs it to every nearby client and saves it with the world.
- If the publishing player disconnects, the projection **persists for everyone else** — it lives in the
  server-side projector, not the publisher's client.
- Frames survive server restarts. A script can also simply republish on boot.

## Example: sector map hologram

```lua
local proj  = peripherals.wrap(peripherals.getRelative("front"), "projector")
local frame = proj.newFrame()

-- A ground grid drawn with 3D lines.
frame.setLayer("grid")
for i = -5, 5 do
  frame.line3d(i, 0, -5, i, 0, 5, 0.3, 0.3, 0.3, 1.0)
  frame.line3d(-5, 0, i, 5, 0, i, 0.3, 0.3, 0.3, 1.0)
end

-- A wireframe marker box hovering above the projector.
frame.setLayer("markers")
frame.boxWire(-0.5, 2, -0.5, 1, 1, 1, 0.2, 0.9, 0.5, 1.0, 2)

-- A floating map panel.
frame.setLayer("map")
local surf = frame.newSurface(-2, 3, 0,  0,0,1,  0,1,0,  4.0, 4.0,  128, 128)
surf.rect(0, 0, 128, 128, 0.02, 0.05, 0.10, 0.8, true)
surf.rect(0, 0, 128, 128, 0.20, 0.50, 1.00, 0.9, false)
surf.circle(64, 64, 40, 0.2, 0.9, 0.5, 1.0, false, 32)
surf.text(6, 6, "SECTOR MAP", 1, 1, 1, 1, 1)

proj.publish(frame)   -- visible to every player near the ship
```
