-- /bin/gfx_demo.lua
-- Layered graphics showcase for the newer gfx API.
-- Usage: run /bin/gfx_demo.lua [seconds]

local seconds = tonumber(args[1]) or 10
local gfxlib = require("gfxlib")
if seconds < 1 then
    seconds = 1
end

local frameDelayMs = 33
local totalFrames = math.floor((seconds * 1000) / frameDelayMs)
local step = 20

local function clamp(v, minV, maxV)
    if v < minV then
        return minV
    end
    if v > maxV then
        return maxV
    end
    return v
end

gfx.clear()
gfx.createLayer("bg", 0)
gfx.createLayer("grid", 2)
gfx.createLayer("shapes", 6)
gfx.createLayer("fx", 12)
gfx.createLayer("text", 16)

local w = gfx.getWidth()
local h = gfx.getHeight()

local function redrawStaticLayers(width, height)
    gfx.clearLayer("bg")
    gfx.clearLayer("grid")

    gfx.setLayer("bg")
    gfx.rect(0, 0, width, height, 0.03, 0.04, 0.07, 0.88, true)
    gfx.rect(2, 2, width - 4, height - 4, 0.22, 0.4, 0.95, 0.85, false)

    gfx.setLayer("grid")
    for x = 0, width, step do
        gfx.line(x, 0, x, height - 1, 0.1, 0.25, 0.45, 0.25)
    end
    for y = 0, height, step do
        gfx.line(0, y, width - 1, y, 0.1, 0.25, 0.45, 0.25)
    end
end

print("gfx demo started for " .. seconds .. "s")
print("canvas: " .. w .. "x" .. h)

redrawStaticLayers(w, h)

for i = 0, totalFrames do
    local currentW = gfx.getWidth()
    local currentH = gfx.getHeight()
    if currentW ~= w or currentH ~= h then
        w = currentW
        h = currentH
        redrawStaticLayers(w, h)
    end

    local t = i / 30.0

    gfx.clearLayer("shapes")
    gfx.clearLayer("fx")
    gfx.clearLayer("text")

    local cx = math.floor(w * 0.5)
    local cy = math.floor(h * 0.5)

    local boxW = math.floor(30 + (math.sin(t * 1.7) + 1) * 18)
    local boxH = math.floor(20 + (math.cos(t * 1.3) + 1) * 12)

    local ox = math.floor(math.sin(t * 1.1) * (w * 0.25))
    local oy = math.floor(math.cos(t * 0.9) * (h * 0.2))

    local x = clamp(cx + ox - math.floor(boxW * 0.5), 0, math.max(0, w - boxW - 1))
    local y = clamp(cy + oy - math.floor(boxH * 0.5), 0, math.max(0, h - boxH - 1))

    -- Dynamic rectangles and crosshair.
    gfx.setLayer("shapes")
    gfx.rect(x, y, boxW, boxH, 0.1, 0.8, 0.95, 0.55, true)
    gfx.rect(x, y, boxW, boxH, 0.9, 0.98, 1.0, 1.0, false)
    gfx.circle(cx, cy, 22 + math.floor((math.sin(t * 2.2) + 1) * 6), 1.0, 0.45, 0.2, 0.8, false, 36, 3)
    gfx.polygon({
        cx, clamp(cy - 26, 0, h - 1),
        clamp(cx + 22, 0, w - 1), clamp(cy + 20, 0, h - 1),
        clamp(cx - 22, 0, w - 1), clamp(cy + 20, 0, h - 1)
    }, 0.95, 0.8, 0.25, 0.85, true)
    gfx.line(0, cy, w - 1, cy, 0.9, 0.2, 0.35, 0.7, 2)
    gfx.line(cx, 0, cx, h - 1, 0.9, 0.2, 0.35, 0.7, 2)

    -- Accent points and a blinking layer visibility toggle.
    gfx.setLayer("fx")
    local p1x = clamp(cx + math.floor(math.cos(t * 2.0) * (w * 0.35)), 0, w - 1)
    local p1y = clamp(cy + math.floor(math.sin(t * 2.4) * (h * 0.35)), 0, h - 1)
    local p2x = clamp(cx + math.floor(math.sin(t * 2.8) * (w * 0.28)), 0, w - 1)
    local p2y = clamp(cy + math.floor(math.cos(t * 1.9) * (h * 0.28)), 0, h - 1)
    gfx.point(p1x, p1y, 1.0, 0.9, 0.2, 1.0)
    gfx.point(p2x, p2y, 0.3, 1.0, 0.35, 1.0)

    local checker = gfxlib.checkerBitmap(8, 8, 0xFF9933FF, 0x223344CC, 1)
    gfxlib.draw(gfx, 10, 10, checker)

    local mask = gfxlib.textMaskBitmap({
        "##..##",
        ".####.",
        ".####.",
        "##..##",
    }, 0x66DDFFFF, 0x00000000, "#")
    gfxlib.draw(gfx, w - 20, 10, mask)

    gfx.setLayer("text")
    gfx.text(24, 10, "GFX+", 0.95, 0.98, 1.0, 1.0, 2)
    gfx.text(24, 26, "circle polygon bitmap", 0.5, 0.9, 1.0, 0.95, 1)
    gfx.text(24, h - 28, "thick lines + wrapped center text layout", 0.92, 0.96, 1.0, 1.0, 1, w - 48, 20, "center", true)

    if (i % 24) < 12 then
        gfx.setLayerVisible("fx", true)
    else
        gfx.setLayerVisible("fx", false)
    end

    util.sleep(frameDelayMs)
end

gfx.setLayerVisible("fx", true)
gfx.removeLayer("fx")
gfx.clearLayer("shapes")
print("gfx demo complete")

