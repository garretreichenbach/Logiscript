# Text Graphics API

`gfx` provides an off-screen text canvas for drawing ASCII/Unicode shapes in the terminal.

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

- `pixel(x, y, char)`
Draws one character at `(x, y)`.

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

- `frame()`
Returns the full canvas as a single newline-separated string.

- `render()`
Pushes the canvas to the terminal output area (replaces current terminal text contents).

## Example

```lua
gfx.setSize(50, 18)
gfx.clear(" ")

gfx.rect(1, 1, 50, 18, "#")
gfx.line(2, 2, 49, 17, "/")
gfx.line(49, 2, 2, 17, "\\")
gfx.circle(25, 9, 5, "*")
gfx.text(16, 9, "LuaMade GFX")

gfx.render()
```

## Notes

- `gfx.render()` replaces the terminal text buffer. If you want to keep a static screen, disable automatic prompt with `term.setAutoPrompt(false)`.
- For animation loops, redraw onto the canvas and call `gfx.render()` each frame.
