# Graphics Documentation

Graphics APIs for rendering 2D overlays inside the computer terminal UI.

## Contents

- **gfx2d.md** - Layered terminal graphics API (`gfx2d`) for primitives, text, polygons, circles, and bitmaps
- **gui.md** - Component-based GUI framework (`gui`) built on `gfx2d`: panels, buttons, text labels, layouts, and
  modal dialogs

## Notes

- The current `gfx2d` renderer draws in terminal UI space, not worldspace.
- Draw commands are clipped to terminal bounds each frame.
- Layer count and per-layer command limits are controlled by server config.
