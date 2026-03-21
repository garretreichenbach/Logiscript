# Graphics Documentation

Graphics APIs for rendering 2D overlays inside the computer terminal UI.

## Contents

- **gfx_2d.md** - Layered terminal graphics API (`gfx_2d`) for primitives, text, polygons, circles, and bitmaps
- **gui_lib.md** - Component-based GUI framework (`gui_lib`) built on `gfx`: panels, buttons, text labels, layouts, and
  modal dialogs

## Notes

- The current `gfx_2d` renderer draws in terminal UI space, not worldspace.
- Draw commands are clipped to terminal bounds each frame.
- Layer count and per-layer command limits are controlled by server config.
