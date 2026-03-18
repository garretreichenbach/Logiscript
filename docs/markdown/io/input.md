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
    cellX = 5, -- optional 1-based canvas cell X when gfx canvas frame is active
    cellY = 3, -- optional 1-based canvas cell Y when gfx canvas frame is active
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

## Common key codes

| Key   | Code | Key       | Code  |
|-------|------|-----------|-------|
| ESC   | 1    | ENTER     | 28    |
| SPACE | 57   | BACKSPACE | 14    |
| TAB   | 15   | UP        | 200   |
| DOWN  | 208  | LEFT      | 203   |
| RIGHT | 205  | F1-F12    | 59-70 |

> **Tip:** Print `e.key` to discover the code for any key.

## Example — interactive menu

```lua
term.setAutoPrompt(false)
gfx.setAnsiEnabled(true)
input.clear()

local items = { "Start Game", "Options", "Quit" }
local sel = 1

local function render()
    gfx.setSize(30, #items + 4)
    gfx.clear(" ", "white", "blue")
    gfx.setColor("bright_white", "blue")
    gfx.text(2, 1, "== Main Menu ==")
    for i, item in ipairs(items) do
        if i == sel then
            gfx.setColor("black", "cyan")
        else
            gfx.setColor("white", "blue")
        end
        gfx.text(3, i + 2, (i == sel and "> " or "  ") .. item)
    end
    gfx.render()
end

render()

while true do
    local e = input.waitFor(5000)
    if e == nil then
        break
    end  -- timeout / closed

    if e.type == "key" and e.down then
        if e.key == 200 then
            -- UP arrow
            sel = sel > 1 and sel - 1 or #items
            render()
        elseif e.key == 208 then
            -- DOWN arrow
            sel = sel < #items and sel + 1 or 1
            render()
        elseif e.key == 28 then
            -- ENTER
            console.print("Selected: " .. items[sel])
            break
        elseif e.key == 1 then
            -- ESC
            break
        end
    end
end

term.setAutoPrompt(true)
term.reboot()
```

## Notes

- `input.waitFor()` blocks the Lua script thread, not the game thread. Other scripts and the game continue running
  normally.
- The queue holds up to **256 events**. Older events are silently dropped if the script doesn't read fast enough.
- Key codes match LWJGL keyboard constants (same values as the GLFW wrapper in StarMade).
- Mouse `x`/`y` are pixel coordinates within the computer dialog window.
- Mouse `cellX`/`cellY` are provided only when a `gfx` canvas frame is active and the pointer is inside that frame.

