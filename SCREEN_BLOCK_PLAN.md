# Screen Block Implementation Plan

## Overview

Add a **Screen Block** to Logiscript that renders a scriptable text+graphics display in
worldspace — analogous to ComputerCraft monitors. Screens can be tiled together into a
larger display (multi-block extension), and are controlled via the Lua `peripheral` API
from any connected computer.

The `gfx` in-dialog canvas overlay system has been removed. This plan describes its
intended replacement.

---

## Design Goals

| Goal                 | Notes                                                                             |
|----------------------|-----------------------------------------------------------------------------------|
| Worldspace rendering | Uses `ModWorldDrawer` / `RegisterWorldDrawersEvent` API                           |
| Resizable            | Single block: up to **256×256** cells (max usable face texture); tiled: arbitrary |
| Multi-block tiling   | Adjacent Screen Blocks auto-merge into one logical display (like CC)              |
| Peripheral access    | Connected via `peripheral.wrap("screen_N")` in Lua                                |
| Server/client split  | Display content is synced server→client via `SystemModule` tag sync               |
| Persistent content   | Screen content saved/restored in module tag data (`onTagSerialize`)               |

---

## StarMade API Hooks Used

| API Class                                                | Usage                                                                   |
|----------------------------------------------------------|-------------------------------------------------------------------------|
| `api.utils.draw.ModWorldDrawer`                          | Renders the screen quads+text each frame in `postWorldDraw()`           |
| `api.listener.events.draw.RegisterWorldDrawersEvent`     | Registers the `ScreenWorldDrawer` singleton                             |
| `api.utils.game.module.util.SystemModule`                | Per-screen-block module; holds content state and syncs it               |
| `api.utils.textures.StarLoaderTexture`                   | Allocates a dynamic `BufferedImage`-backed texture for each screen face |
| `api.listener.fastevents.SegmentDrawListener`            | `postDrawSegment` hook to inject per-segment screen quads in-world      |
| `api.listener.events.block.SegmentPieceActivateByPlayer` | Detect right-click to show config dialog (optional)                     |
| `org.schema.schine.graphicsengine.forms.Sprite`          | Holds per-screen rendered texture; updated each dirty frame             |

---

## Architecture

```
ScreenBlockModule (per block, server+client)
├── ScreenBuffer          – 2D cell array: char + fg + bg color
├── onTagSerialize()      – PacketWriteBuffer: write width, height, cell data
├── onTagDeserialize()    – PacketReadBuffer:  read and apply to ScreenBuffer
└── flagUpdatedData()     – push changes to nearby clients

ScreenWorldDrawer (singleton, client-only, ModWorldDrawer)
├── postWorldDraw()       – iterate registered screens, render dirty ones
└── renderScreenFace()    – GL11 quad with texture mapped to block face in world transform

ScreenBlockContainer (per SegmentController, wraps SystemModule lifecycle)
└── extends ModManagerContainerModule

ScreenTileGroup (client-side)
├── detectAdjacentScreens()  – scan neighbours for same-facing Screen Blocks
├── computeGroupOrigin()     – top-left corner of tiled group
└── computeGroupSize()       – total columns × rows in unified display

LuaScreenApi (extends LuaMadeUserdata, exposed as peripheral)
├── write(text)
├── clear()
├── setCursorPos(x, y)
├── setTextColor(color)
├── setBackgroundColor(color)
├── getSize()              – returns { width, height } of logical display
├── scroll(n)
└── blit(text, fg, bg)     – CC-compatible bulk write
```

---

## Implementation Steps

### Step 1 — Screen Block Element

**File:** `src/main/java/luamade/element/block/Screen.java`

- Extend `luamade.element.block.Block`
- Set block name `"Screen"`, orientatable, activatable
- 256×256 max texture face resolution; 1 block = default 40×12 character grid
- Register in `ElementRegistry.java` as a new enum entry

### Step 2 — ScreenBuffer Data Model

**File:** `src/main/java/luamade/system/screen/ScreenBuffer.java`

- Holds `char[]` `text`, `int[]` `fg`, `int[]` `bg`, each of size `width * height`
- `write(x, y, text, fg, bg)` — write a line of text at a cursor position
- `clear(bg)` — fill with spaces + background color
- `scroll(lines)` — shift content up/down
- `blit(x, y, text, fg, bg)` — bulk-write matching CC `blit()` semantics
- Colors: 16-color palette (matching CC), stored as 4-bit index
- Dirty flag: `markDirty()` / `isDirty()` / `clearDirty()`

### Step 3 — ScreenBlockModule (SystemModule)

**File:** `src/main/java/luamade/system/module/ScreenModule.java`

- Extends `api.utils.game.module.util.SystemModule`
- Fields: `ScreenBuffer buffer`, `int cols`, `int rows`
- `onTagSerialize`: write `cols`, `rows`, then flat cell arrays (char + color bytes)
- `onTagDeserialize`: read and reconstruct buffer; mark dirty for renderer
- `flagUpdatedData()`: inherited — syncs buffer from server to nearby clients
- Default size: 40 cols × 12 rows (configurable via Lua `setSize(w,h)`)

### Step 4 — ScreenModuleContainer

**File:** `src/main/java/luamade/system/module/ScreenModuleContainer.java`

- Extends `ModManagerContainerModule` (follows `ComputerModuleContainer` pattern)
- Holds a `ScreenModule` instance per block
- Registered via `ElementRegistry` block config

### Step 5 — ScreenWorldDrawer (Client-side Renderer)

**File:** `src/main/java/luamade/gui/ScreenWorldDrawer.java`

- Extends `api.utils.draw.ModWorldDrawer`
- Singleton registered in `postWorldDraw()` from `RegisterWorldDrawersEvent` listener
- Each frame, iterate all loaded `ScreenModule` instances:
    - Skip if buffer not dirty and texture already up-to-date
    - Render buffer to `BufferedImage` (256×256 max per block):
        - Background rects via `Graphics2D.fillRect()`
        - Text glyphs via `Graphics2D.drawString()` with monospace font
    - Upload image to `StarLoaderTexture` / `Sprite` via `TextureSwapper`
    - Draw `GL11` quad at the block's worldspace transform (from `SegmentPiece` position)

#### Tiled Rendering

- Before rendering, run `ScreenTileGroup.detect()` on adjacent Screen Blocks
- If part of a group, defer to the group's top-left corner block to render as one big quad
- Group's combined `BufferedImage` size = `groupCols * cellWidth × groupRows * cellHeight`
- Maximum recommended tiled display: no enforced limit, but practical cap ~2048×2048 px total

### Step 6 — LuaScreenApi (Peripheral)

**File:** `src/main/java/luamade/lua/peripheral/ScreenPeripheral.java`

- Extends `LuaMadeUserdata` (same pattern as other peripherals)
- Wraps `ScreenModule` reference found via `PeripheralsApi.wrap("screen_N")`
- Screen naming: `screen_<absIndex_hex>` (same UUID pattern as computers)

**Lua API surface:**

```lua
local s = peripheral.wrap("screen_00a3f9")

s.clear()                        -- clear to black
s.setTextColor("white")          -- set foreground
s.setBackgroundColor("blue")     -- set background
s.setCursorPos(1, 1)             -- 1-based, like CC
s.write("Hello, world!")         -- write at cursor, advance cursor
s.blit("Hello", "fffff", "00000") -- CC-compatible bulk write
local w, h = s.getSize()         -- returns logical cols, rows
s.setSize(80, 24)                -- resize (clamped to 256×256 cells)
s.scroll(1)                      -- scroll down 1 line
```

### Step 7 — PeripheralsApi Integration

**File:** `src/main/java/luamade/lua/peripheral/PeripheralsApi.java` (extend existing)

- `wrap("screen_<id>")` — look up `ScreenModule` by absIndex, return `ScreenPeripheral`
- `getNames()` — list all screen peripherals on the current entity
- Uses `SegmentPieceUtils` to scan the segment controller for Screen Block pieces

### Step 8 — EventManager Wiring

**File:** `src/main/java/luamade/manager/EventManager.java`

- Register `RegisterWorldDrawersEvent` listener → add `ScreenWorldDrawer` singleton to drawables list
- Register `SegmentPieceActivateByPlayer` for Screen Block ID → show optional screen config dialog

### Step 9 — Documentation

**Files:**

- `docs/markdown/graphics/screen-block.md` — Lua API reference + tiling guide + examples
- `docs/markdown/graphics/INDEX.md` — update contents list
- `docs/index.md` — re-add Graphics toctree pointing to `screen-block.md`
- `src/main/resources/docs/docs.index` — add `graphics/screen-block.md`

---

## Color Palette

16-color palette matching ComputerCraft for cross-familiarity:

| Index | Name       | Hex     |
|-------|------------|---------|
| 0     | white      | #F0F0F0 |
| 1     | orange     | #F2B233 |
| 2     | magenta    | #E57FD8 |
| 3     | light_blue | #99B2F2 |
| 4     | yellow     | #DEDE6C |
| 5     | lime       | #7FCC19 |
| 6     | pink       | #F2B2CC |
| 7     | gray       | #4C4C4C |
| 8     | light_gray | #999999 |
| 9     | cyan       | #4C99B2 |
| 10    | purple     | #B266E5 |
| 11    | blue       | #3366CC |
| 12    | brown      | #7F664C |
| 13    | green      | #57A64E |
| 14    | red        | #CC4C4C |
| 15    | black      | #111111 |

---

## Multi-Block Tiling Rules

1. Screen Blocks tile when they share the **same facing direction** (all north, all east, etc.)
   and form a connected **rectangular** region.
2. The **top-left** block (minimum X then Y in local coords) is the **origin** of the group.
3. Only the origin renders; other blocks in the group render nothing (their faces are covered).
4. Lua `getSize()` returns the combined logical size of the full group.
5. If a block is removed, the group splits; remaining blocks re-detect their groups next frame.

---

## Size / Limits

| Parameter                                    | Value                                                   |
|----------------------------------------------|---------------------------------------------------------|
| Single block max texture                     | 256×256 px                                              |
| Default character grid (single block)        | 40 cols × 12 rows                                       |
| Max character grid (single block, tiny font) | ~170 cols × ~85 rows                                    |
| Tiled max (practical)                        | No hard limit; GPU texture atlas constrains large grids |
| Colors                                       | 16 (4-bit palette, see above)                           |
| CC `blit()` compatibility                    | Yes — same `char`/`fg`/`bg` strings                     |

---

## File Summary

| File                                       | Action                                                        |
|--------------------------------------------|---------------------------------------------------------------|
| `element/block/Screen.java`                | **New** — block element definition                            |
| `element/ElementRegistry.java`             | **Modify** — add `SCREEN` entry                               |
| `system/screen/ScreenBuffer.java`          | **New** — cell buffer model                                   |
| `system/module/ScreenModule.java`          | **New** — SystemModule with tag sync                          |
| `system/module/ScreenModuleContainer.java` | **New** — module container                                    |
| `gui/ScreenWorldDrawer.java`               | **New** — ModWorldDrawer client renderer                      |
| `lua/peripheral/ScreenPeripheral.java`     | **New** — Lua peripheral API                                  |
| `lua/peripheral/PeripheralsApi.java`       | **Modify** — add screen wrap support                          |
| `manager/EventManager.java`                | **Modify** — register world drawer + screen activate listener |
| `docs/markdown/graphics/screen-block.md`   | **New** — Lua API reference                                   |
| `docs/markdown/graphics/INDEX.md`          | **Modify** — contents list                                    |
| `docs/index.md`                            | **Modify** — re-add Graphics toctree                          |
| `src/main/resources/docs/docs.index`       | **Modify** — add screen-block.md                              |

---

## Open Questions

1. **Activation interaction**: Should right-clicking a Screen Block show a configuration
   dialog (resolution, label), or should all config be Lua-side only?
2. **Font choice**: Use StarMade's built-in `FontLibrary` via `Graphics2D` font, or ship
   a bundled monospace bitmap font for crisp character rendering at small sizes?
3. **Multi-block sync granularity**: Sync the entire buffer on any change, or diff-patch
   only changed cells? (Start simple: full sync; optimize later with dirty-region tracking.)
4. **Tiling detection**: Run detection server-side (authoritative) or client-side only?
   Server-side is safer for Lua `getSize()` to return correct values.

