# GUI Library (`gui_lib`)

`gui_lib` is a component-based 2D GUI framework built on top of `gfx`.
It handles layout, mouse hit-testing, resize adaptation, and event-driven rendering so scripts can build interactive
terminal UIs without managing low-level drawing or event loops.

```lua
local GUI = require("gui_lib")
```

---

## Quick Start

```lua
local GUI = require("gui_lib")

local mgr = GUI.GUIManager.new()

local panel = GUI.Panel.new(10, 10, 180, 80, "My Panel")
mgr:addComponent(panel)

local btn = GUI.Button.new(20, 40, 60, 14, "Click Me", function()
    console.print("clicked!")
end)
panel:addChild(btn)

mgr:setLayoutCallback(function(m, w, h)
    panel:setSize(w - 20, h - 20)
end)

mgr:run()
```

---

## GUIManager

Main controller. Owns the event loop, canvas dimensions, and component list.

### Constructor

```lua
local mgr = GUI.GUIManager.new()
```

### Methods

| Method                                 | Description                                                                                                                                                           |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `mgr:addComponent(component)`          | Add a top-level component.                                                                                                                                            |
| `mgr:removeComponent(component)`       | Remove a previously added component.                                                                                                                                  |
| `mgr:draw()`                           | Render all visible components once. Usually called by `run()`.                                                                                                        |
| `mgr:update(deltaTime)`                | Propagate update tick to all components (seconds as float).                                                                                                           |
| `mgr:run(maxFrames)`                   | Start the event-driven loop. Blocks until `mgr:stop()` or `maxFrames` is reached. Owns the mouse (calls `input.consumeMouse()`). `maxFrames` defaults to `math.huge`. |
| `mgr:stop()`                           | Signal `run()` to exit after the current frame.                                                                                                                       |
| `mgr:setDataRefreshMs(ms)`             | Idle redraw interval in ms (default `500`). UI redraws even with no mouse events so data labels stay fresh.                                                           |
| `mgr:setMouseOffset(offsetX, offsetY)` | Global pixel offset applied to mouse `uiX`/`uiY` before hit-testing. Useful when the canvas starts at a non-zero position.                                            |
| `mgr:setBackgroundColor(r, g, b, a)`   | Background fill color for the full canvas (default dark navy).                                                                                                        |
| `mgr:setBorderColor(r, g, b, a)`       | 2px inner border color (default blue).                                                                                                                                |
| `mgr:setLayoutCallback(fn)`            | Callback `fn(manager, width, height)` called each frame when the canvas size may have changed. Use it to reposition/resize components dynamically.                    |

### Fields (read)

| Field            | Description                                      |
|------------------|--------------------------------------------------|
| `mgr.width`      | Current canvas width in pixels.                  |
| `mgr.height`     | Current canvas height in pixels.                 |
| `mgr.frameCount` | Number of frames rendered since `run()` started. |
| `mgr.running`    | `true` while inside `run()`.                     |
| `mgr.mouseState` | Latest mouse state table (see below).            |

**`mouseState` fields:**

```lua
{
  hasPosition  = bool,    -- true once any mouse move was received
  insideCanvas = bool,
  uiX          = number,  -- logical canvas X of last cursor position
  uiY          = number,  -- logical canvas Y of last cursor position
  rawX         = number,  -- uiX before mouseOffset is applied
  rawY         = number,
  leftDown     = bool,    -- true while left button is held
}
```

### Built-in layers

`GUIManager` creates these `gfx` layers automatically (lower order renders first):

| Layer name     | Order |
|----------------|-------|
| `"background"` | 0     |
| `"grid"`       | 2     |
| `"panels"`     | 4     |
| `"components"` | 6     |
| `"overlay"`    | 8     |
| `"effects"`    | 10    |

---

## Component (base class)

All GUI widgets extend `Component`. You can subclass it for custom widgets.

### Constructor

```lua
local comp = GUI.Component.new(x, y, width, height)
```

### Common methods (available on every widget)

| Method                                 | Description                                                                                                                 |
|----------------------------------------|-----------------------------------------------------------------------------------------------------------------------------|
| `comp:setPosition(x, y)`               | Move to absolute canvas pixel coordinates.                                                                                  |
| `comp:setSize(width, height)`          | Resize.                                                                                                                     |
| `comp:getPosition()`                   | Returns `x, y`.                                                                                                             |
| `comp:getSize()`                       | Returns `width, height`.                                                                                                    |
| `comp:setVisible(bool)`                | Show or hide without removing.                                                                                              |
| `comp:isVisible()`                     | Returns current visibility.                                                                                                 |
| `comp:setLayer(name)`                  | Which `gfx` layer this component draws on (default `"components"`).                                                         |
| `comp:setRelativeRect(rx, ry, rw, rh)` | Responsive layout in [0, 1] fractions of canvas size. Applied every frame before drawing. Overrides absolute position/size. |
| `comp:setLayoutCallback(fn)`           | Per-component callback `fn(self, canvasW, canvasH)` called each frame for custom responsive logic.                          |
| `comp:pointInBounds(px, py)`           | Returns `true` if `(px, py)` falls inside the component's bounding rect.                                                    |

### Overrideable methods (for custom components)

```lua
function MyComp:draw()         -- called each frame by manager
function MyComp:update(dt)     -- called each frame with delta time (seconds)
function MyComp:onMouseEvent(e) -- return true to consume (stop propagation)
```

---

## Panel

Rectangular container with optional title. Renders its children on top of itself.

### Constructor

```lua
local panel = GUI.Panel.new(x, y, width, height, title)
```

`title` is optional (defaults to `""`).

### Methods

| Method                                 | Description                                            |
|----------------------------------------|--------------------------------------------------------|
| `panel:setBackgroundColor(r, g, b, a)` | Fill color (default semi-transparent dark blue).       |
| `panel:setBorderColor(r, g, b, a)`     | Outline color (default bright blue).                   |
| `panel:setTitleScale(scale)`           | Integer text scale for the title (default `1`).        |
| `panel:addChild(component)`            | Add a child component. Inherits manager automatically. |
| `panel:removeChild(component)`         | Remove a child.                                        |

Children's `draw()`, `update()`, and `onMouseEvent()` are forwarded automatically.
Mouse events are dispatched back-to-front (topmost/last-added child gets priority).

### Example

```lua
local panel = GUI.Panel.new(8, 8, 200, 100, "Status")
panel:setBorderColor(0.3, 0.8, 0.4, 1.0)

local label = GUI.Text.new(12, 26, "All systems nominal")
panel:addChild(label)

mgr:addComponent(panel)
```

---

## Button

Clickable button with hover, pressed, and disabled visual states.

### Constructor

```lua
local btn = GUI.Button.new(x, y, width, height, label, onPress)
```

`onPress` is a callback `function()` called when the button is clicked.

### Methods

| Method                                   | Description                                                |
|------------------------------------------|------------------------------------------------------------|
| `btn:setLabel(text)`                     | Update button label. Long labels are clipped with `~`.     |
| `btn:setOnPress(fn)`                     | Replace the press callback.                                |
| `btn:setEnabled(bool)`                   | Disable to prevent interaction and apply greyed-out style. |
| `btn:setLabelScale(scale)`               | Integer text scale for the label (default `1`).            |
| `btn:setNormalColor(r, g, b, a)`         | Default background color.                                  |
| `btn:setHoverColor(r, g, b, a)`          | Hover background color.                                    |
| `btn:setPressedColor(r, g, b, a)`        | Mouse-held background color.                               |
| `btn:setDisabledColor(r, g, b, a)`       | Background when `enabled = false`.                         |
| `btn:setDisabledBorderColor(r, g, b, a)` | Border when disabled.                                      |
| `btn:setDisabledTextColor(r, g, b, a)`   | Label color when disabled.                                 |

### State fields (read)

| Field         | Type | Description                              |
|---------------|------|------------------------------------------|
| `btn.hovered` | bool | Cursor is currently inside the button.   |
| `btn.pressed` | bool | Left button is held down on this button. |
| `btn.enabled` | bool | Whether the button accepts input.        |

### Example

```lua
local btn = GUI.Button.new(20, 50, 80, 14, "Launch")
btn:setNormalColor(0.7, 0.1, 0.1, 1.0)
btn:setHoverColor(1.0, 0.2, 0.2, 1.0)
btn:setOnPress(function()
    console.print("Launched!")
end)
panel:addChild(btn)
```

---

## Text

Static text label. Supports multi-line, word-wrap, alignment, and clipping.

### Constructor

```lua
local lbl = GUI.Text.new(x, y, content)
```

`content` may contain `\n` for line breaks.

### Methods

| Method                                            | Description                                                                                                   |
|---------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| `lbl:setText(text)`                               | Update the displayed string.                                                                                  |
| `lbl:setColor(r, g, b, a)`                        | Text color (default white).                                                                                   |
| `lbl:setScale(scale)`                             | Integer glyph scale (1–16, default `1`).                                                                      |
| `lbl:setLayout(maxWidth, maxHeight, align, wrap)` | Optional clipping/wrapping. `align` is `"left"`, `"center"`, or `"right"`. `wrap = true` wraps to `maxWidth`. |

### Example

```lua
local status = GUI.Text.new(10, 30, "Ready")
status:setColor(0.4, 1.0, 0.5, 1.0)
status:setScale(2)
panel:addChild(status)

-- later:
status:setText("Charging...")
```

---

## HorizontalLayout

Arranges children left-to-right, separated by `spacing` pixels. Automatically repositions children when items are
added/removed.

### Constructor

```lua
local row = GUI.HorizontalLayout.new(x, y, height, spacing)
```

`height` sets the common height for children placed inside. `spacing` defaults to `2`.

### Methods

| Method                       | Description                                             |
|------------------------------|---------------------------------------------------------|
| `row:addChild(component)`    | Add a child; triggers `recalculateLayout()`.            |
| `row:removeChild(component)` | Remove a child; triggers `recalculateLayout()`.         |
| `row:recalculateLayout()`    | Re-position all children. Called automatically.         |
| `row:setPosition(x, y)`      | Move the layout origin; triggers `recalculateLayout()`. |

After all children are added, `row.width` reflects the total occupied width.

### Example

```lua
local btnRow = GUI.HorizontalLayout.new(10, 80, 14, 4)
btnRow:addChild(GUI.Button.new(0, 0, 50, 14, "OK",     function() end))
btnRow:addChild(GUI.Button.new(0, 0, 50, 14, "Cancel", function() end))
panel:addChild(btnRow)
```

---

## VerticalLayout

Arranges children top-to-bottom, separated by `spacing` pixels.

### Constructor

```lua
local col = GUI.VerticalLayout.new(x, y, width, spacing)
```

`spacing` defaults to `1`.

### Methods

| Method                       | Description                                             |
|------------------------------|---------------------------------------------------------|
| `col:addChild(component)`    | Add a child; triggers `recalculateLayout()`.            |
| `col:removeChild(component)` | Remove a child; triggers `recalculateLayout()`.         |
| `col:recalculateLayout()`    | Re-position all children. Called automatically.         |
| `col:setPosition(x, y)`      | Move the layout origin; triggers `recalculateLayout()`. |

After all children are added, `col.height` reflects the total occupied height.

### Example

```lua
local col = GUI.VerticalLayout.new(10, 20, 120, 3)
for i = 1, 5 do
    col:addChild(GUI.Text.new(0, 0, "Row " .. i))
end
panel:addChild(col)
```

---

## ModalDialog

Centered overlay dialog with a message, configurable buttons, and automatic layout.
Renders on the `"overlay"` layer so it appears above everything else.
Always consumes mouse events (modal behavior) while visible.

### Constructor

```lua
local dialog = GUI.ModalDialog.new(title, message)
```

### Methods

| Method                              | Description                                              |
|-------------------------------------|----------------------------------------------------------|
| `dialog:setMessage(text)`           | Update the body text. Supports `\n` and word-wrap.       |
| `dialog:addButton(label, callback)` | Add a button to the dialog footer. Returns the `Button`. |

Buttons are arranged automatically in a `HorizontalLayout`. The dialog positions and sizes itself relative to the canvas
via `applyLayout` — no manual sizing needed.

### Example

```lua
local dialog = GUI.ModalDialog.new("Confirm", "Delete this route?")

dialog:addButton("Yes", function()
    deleteRoute()
    dialog:setVisible(false)
end)
dialog:addButton("No", function()
    dialog:setVisible(false)
end)

dialog:setVisible(false)  -- hidden initially
mgr:addComponent(dialog)

-- show it later:
dialog:setVisible(true)
```

---

## Responsive layouts

Two mechanisms work together for resize-aware UIs:

**1. `setRelativeRect`** — fractional positioning on the component itself:

```lua
-- Panel fills 80% of canvas width, 60% of height, centered
panel:setRelativeRect(0.1, 0.2, 0.8, 0.6)
```

**2. `setLayoutCallback`** — arbitrary Lua logic on resize:

```lua
-- on GUIManager (called for every component too)
mgr:setLayoutCallback(function(m, w, h)
    titleLabel:setPosition(math.floor(w * 0.5) - 40, 6)
    btnRow:setPosition(8, h - 20)
end)

-- on a single component
panel:setLayoutCallback(function(self, w, h)
    self:setSize(w - 20, h - 40)
end)
```

Both are applied every frame before drawing, so resizing the terminal window automatically reflows the UI.

---

## Full example

```lua
local GUI = require("gui_lib")

local mgr = GUI.GUIManager.new()

local panel = GUI.Panel.new(0, 0, 0, 0, "Control Panel")
panel:setRelativeRect(0.05, 0.05, 0.9, 0.9)
mgr:addComponent(panel)

local header = GUI.Text.new(8, 8, "Ship Status")
header:setColor(0.4, 0.85, 1.0, 1.0)
header:setScale(2)
panel:addChild(header)

local statusText = GUI.Text.new(8, 30, "Idle")
panel:addChild(statusText)

local row = GUI.HorizontalLayout.new(8, 0, 14, 6)

local startBtn = GUI.Button.new(0, 0, 60, 14, "Start", function()
    statusText:setText("Running")
    startBtn:setEnabled(false)
    stopBtn:setEnabled(true)
end)

stopBtn = GUI.Button.new(0, 0, 60, 14, "Stop", function()
    statusText:setText("Idle")
    startBtn:setEnabled(true)
    stopBtn:setEnabled(false)
end)
stopBtn:setEnabled(false)

row:addChild(startBtn)
row:addChild(stopBtn)
panel:addChild(row)

mgr:setLayoutCallback(function(m, w, h)
    row:setPosition(8, h - 30)
end)

mgr:run()
```

