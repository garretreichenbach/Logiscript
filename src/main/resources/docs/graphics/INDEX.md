# Graphics Documentation

Graphics APIs for rendering 2D overlays inside the computer terminal UI.

## Contents

- **gfx2d.md** - Layered terminal graphics API (`gfx2d`) for primitives, text, polygons, circles, and bitmaps
- **gfx3d.md** - World-space projector graphics API (`gfx3d`): 3D vector primitives and 2D surfaces rendered in
  the world via a Projector block, server-synced to every nearby player
- **gui.md** - Component-based GUI framework (`gui`) built on `gfx2d`: panels, buttons, text labels, layouts, and
  modal dialogs

## Notes

- The `gfx2d` renderer draws in terminal UI space, not worldspace; draw commands are clipped to terminal bounds
  each frame.
- The `gfx3d` renderer draws in **world space** via a Projector block. It is server-authoritative and rendered
  identically for **every player near the block** (multi-viewer), unlike `gfx2d` which is per-viewer terminal
  overlay. Frames persist across restart and cost no per-frame network traffic (only `publish()` syncs).
- Layer count and per-layer command limits are controlled by server config.
