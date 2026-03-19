# Graphics Documentation

Graphics APIs for rendering 2D overlays inside the computer terminal UI.

## Contents

- **gfx.md** - Layered terminal graphics API (`gfx`) for lines, rectangles, and points

## Notes

- The current `gfx` renderer draws in terminal UI space, not worldspace.
- Draw commands are clipped to terminal bounds each frame.
- Layer count and per-layer command limits are controlled by server config.
