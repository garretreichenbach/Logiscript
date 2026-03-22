# Input API

`input` provides a per-computer keyboard and mouse event queue, enabling scripts to build interactive UIs.

Events are captured only while the computer's terminal dialog is open and are automatically discarded when it closes.

## Event types

### Key event

```lua
{
    type = "key", -- always "key"
    key = 65, -- GLFW key code (e.g. 65 = A, 256 = ESC)
    char = "a", -- printable character string (may be empty for non-printable keys)
    down = true, -- true on key-press, false on key-release
    shift = false, -- Shift held
    ctrl = false, -- Ctrl held
    alt = false, -- Alt held
}
```

### Mouse event

```lua
{
    type = "mouse", -- always "mouse"
    button = "left", -- "left" | "right" | "middle" | "none"
    pressed = true, -- true on button-down
    released = false, -- true on button-up
    x = 120, -- absolute x position in dialog pixels
    y = 45, -- absolute y position in dialog pixels
  windowX = 100, -- dialog top-left X in UI pixels, nil when unavailable
  windowY = 80, -- dialog top-left Y in UI pixels, nil when unavailable
  canvasX = 120, -- terminal gfx2d canvas top-left X in UI pixels, nil when unavailable
  canvasY = 115, -- terminal gfx2d canvas top-left Y in UI pixels, nil when unavailable
  uiX = 28, -- optional x inside gfx2d canvas (0-based), nil when outside
  uiY = 12, -- optional y inside gfx2d canvas (0-based), nil when outside
  insideCanvas = true, -- true when pointer is currently inside terminal gfx2d bounds
  dragging = false, -- true while any mouse button is held
  dragButton = "none", -- active drag button: left|right|middle|none
  cellX = nil, -- reserved for future text-cell mapping
  cellY = nil, -- reserved for future text-cell mapping
    dx = 0, -- delta x since last event
    dy = 0, -- delta y since last event
    wheel = 0, -- scroll wheel delta (positive = up)
}
```

## Reference

- `input.poll()`
  Returns the next event from the queue immediately, or `nil` if none are pending.

- `input.waitFor(timeoutMs)`
  Blocks the script thread up to `timeoutMs` milliseconds waiting for an event. Returns the event table, or `nil` on
  timeout. Pass `0` to behave like `poll()`.

- `input.clear()`
  Discards all pending events.

- `input.setEnabled(bool)`
  Enables or disables event capture. New events are not enqueued while disabled; already-queued events remain.

- `input.isEnabled()`
  Returns whether event capture is currently active.

- `input.pending()`
  Returns the number of events currently in the queue (max 256).

- `input.getUiLayout()`
  Returns a table with the current dialog/canvas bounds:

  ```lua
  {
    windowX = 100, -- dialog top-left X in UI pixels, nil when unavailable
    windowY = 80, -- dialog top-left Y in UI pixels, nil when unavailable
    windowWidth = 850,
    windowHeight = 650,
    canvasX = 120, -- terminal gfx2d canvas top-left X in UI pixels, nil when unavailable
    canvasY = 115, -- terminal gfx2d canvas top-left Y in UI pixels, nil when unavailable
    canvasWidth = 820,
    canvasHeight = 500
  }
  ```

### Keyboard capture

- `input.consumeKeyboard()`
  Claims exclusive keyboard control. While active, StarMade key events are
  cancelled so the terminal text bar never receives keystrokes. All key events
  are still forwarded to the Lua input queue so your overlay can handle them.

- `input.releaseKeyboard()`
  Releases exclusive keyboard control and restores normal terminal input
  behaviour.

- `input.isKeyboardConsumed()`
  Returns `true` while a script holds exclusive keyboard control.

### Mouse capture

- `input.consumeMouse()`
  Signals that your script is handling mouse input exclusively. Mouse events
  continue to be delivered to the queue; this flag is intended as a
  coordination signal you can query from other parts of your script.

- `input.releaseMouse()`
  Clears the exclusive mouse signal.

- `input.isMouseConsumed()`
  Returns `true` while a script has signalled exclusive mouse ownership.

## Remote sessions

When a player links a `Remote Control` item to a bound remote access point and activates that access point, key and mouse button input is forwarded into the target computer's `input` queue even while its UI is closed.

Remote sessions also forward mouse movement deltas (`dx`/`dy`) for cursor-driven overlays.

Players receive immediate status feedback when a remote session links or disconnects.

`Escape` is always reserved for safety: pressing it disconnects the active remote session instead of forwarding the key to Lua.

## Common key codes

| Key   | Code | Key       | Code  |
|-------|------|-----------|-------|
| ESC   | 1    | ENTER     | 28    |
| SPACE | 57   | BACKSPACE | 14    |
| TAB   | 15   | UP        | 200   |
| DOWN  | 208  | LEFT      | 203   |
| RIGHT | 205  | F1-F12    | 59-70 |

> **Tip:** Print `e.key` to discover the code for any key.

## Example — draggable panel

```lua
input.clear()
input.consumeMouse()

gfx2d.createLayer("widgets", 10)
gfx2d.setLayer("widgets")

local panel = { x = 30, y = 24, w = 150, h = 80 }
local drag = nil

local function hit(rect, x, y)
  return x and y and x >= rect.x and y >= rect.y and x < (rect.x + rect.w) and y < (rect.y + rect.h)
end

local function render()
  gfx2d.clearLayer("widgets")
  gfx2d.setLayer("widgets")
  gfx2d.rect(panel.x, panel.y, panel.w, panel.h, 0.1, 0.6, 1.0, 0.9, true)
  gfx2d.rect(panel.x, panel.y, panel.w, 16, 0.05, 0.3, 0.8, 1.0, true)
end

render()

while true do
  local e = input.waitFor(33)
  if e and e.type == "mouse" then
    if e.pressed and e.button == "left" and e.insideCanvas and hit(panel, e.uiX, e.uiY) then
      drag = { ox = e.uiX - panel.x, oy = e.uiY - panel.y }
    elseif e.released and e.button == "left" then
      drag = nil
    elseif drag and e.dragging and e.uiX and e.uiY then
      panel.x = e.uiX - drag.ox
      panel.y = e.uiY - drag.oy
        end

    render()
    end
end
```

## Notes

- `input.waitFor()` blocks the Lua script thread, not the game thread. Other scripts and the game continue running
  normally.
- The queue holds up to **256 events**. Older events are silently dropped if the script doesn't read fast enough.
- Key codes match LWJGL keyboard constants (same values as the GLFW wrapper in StarMade).
- Mouse `x`/`y` are absolute dialog coordinates.
- Mouse `uiX`/`uiY` are local coordinates within the terminal gfx2d canvas when inside bounds.
- Use `input.getUiLayout()` when you need explicit window/canvas origins for custom offset math.
- Mouse move events are queued (not only click/release), enabling drag interactions.

