-- /bin/gfx_demo.lua
-- Layered graphics showcase for the newer gfx2d API.
-- Usage: run /bin/gfx_demo.lua [seconds]

local seconds = tonumber(args[1]) or 10
local gfx2d = require("gfx2d")
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

gfx2d.clear()
gfx2d.createLayer("bg", 0)
gfx2d.createLayer("grid", 2)
gfx2d.createLayer("shapes", 6)
gfx2d.createLayer("fx", 12)
gfx2d.createLayer("text", 16)

local w = gfx2d.getWidth()
local h = gfx2d.getHeight()

local function redrawStaticLayers(width, height)
    gfx2d.clearLayer("bg")
    gfx2d.clearLayer("grid")

    gfx2d.setLayer("bg")
    gfx2d.rect(0, 0, width, height, 0.03, 0.04, 0.07, 0.88, true)
    gfx2d.rect(2, 2, width - 4, height - 4, 0.22, 0.4, 0.95, 0.85, false)

    gfx2d.setLayer("grid")
    for x = 0, width, step do
        gfx2d.line(x, 0, x, height - 1, 0.1, 0.25, 0.45, 0.25)
    end
    for y = 0, height, step do
        gfx2d.line(0, y, width - 1, y, 0.1, 0.25, 0.45, 0.25)
    end
end

print("gfx2d demo started for " .. seconds .. "s")
print("canvas: " .. w .. "x" .. h)

redrawStaticLayers(w, h)

for i = 0, totalFrames do
    local currentW = gfx2d.getWidth()
    local currentH = gfx2d.getHeight()
    if currentW ~= w or currentH ~= h then
        w = currentW
        h = currentH
        redrawStaticLayers(w, h)
    end

    local t = i / 30.0

    gfx2d.clearLayer("shapes")
    gfx2d.clearLayer("fx")
    gfx2d.clearLayer("text")

    local cx = math.floor(w * 0.5)
    local cy = math.floor(h * 0.5)

    local boxW = math.floor(30 + (math.sin(t * 1.7) + 1) * 18)
    local boxH = math.floor(20 + (math.cos(t * 1.3) + 1) * 12)

    local ox = math.floor(math.sin(t * 1.1) * (w * 0.25))
    local oy = math.floor(math.cos(t * 0.9) * (h * 0.2))

    local x = clamp(cx + ox - math.floor(boxW * 0.5), 0, math.max(0, w - boxW - 1))
    local y = clamp(cy + oy - math.floor(boxH * 0.5), 0, math.max(0, h - boxH - 1))

    -- Dynamic rectangles and crosshair.
    gfx2d.setLayer("shapes")
    gfx2d.rect(x, y, boxW, boxH, 0.1, 0.8, 0.95, 0.55, true)
    gfx2d.rect(x, y, boxW, boxH, 0.9, 0.98, 1.0, 1.0, false)
    gfx2d.circle(cx, cy, 22 + math.floor((math.sin(t * 2.2) + 1) * 6), 1.0, 0.45, 0.2, 0.8, false, 36, 3)
    gfx2d.polygon({
        cx, clamp(cy - 26, 0, h - 1),
        clamp(cx + 22, 0, w - 1), clamp(cy + 20, 0, h - 1),
        clamp(cx - 22, 0, w - 1), clamp(cy + 20, 0, h - 1)
    }, 0.95, 0.8, 0.25, 0.85, true)
    gfx2d.line(0, cy, w - 1, cy, 0.9, 0.2, 0.35, 0.7, 2)
    gfx2d.line(cx, 0, cx, h - 1, 0.9, 0.2, 0.35, 0.7, 2)

    -- Accent points and a blinking layer visibility toggle.
    gfx2d.setLayer("fx")
    local p1x = clamp(cx + math.floor(math.cos(t * 2.0) * (w * 0.35)), 0, w - 1)
    local p1y = clamp(cy + math.floor(math.sin(t * 2.4) * (h * 0.35)), 0, h - 1)
    local p2x = clamp(cx + math.floor(math.sin(t * 2.8) * (w * 0.28)), 0, w - 1)
    local p2y = clamp(cy + math.floor(math.cos(t * 1.9) * (h * 0.28)), 0, h - 1)
    gfx2d.point(p1x, p1y, 1.0, 0.9, 0.2, 1.0)
    gfx2d.point(p2x, p2y, 0.3, 1.0, 0.35, 1.0)

    local checker = gfx2d.checkerBitmap(8, 8, 0xFF9933FF, 0x223344CC, 1)
    gfx2d.draw(gfx2d, 10, 10, checker)

    local mask = gfx2d.textMaskBitmap({
        "##..##",
        ".####.",
        ".####.",
        "##..##",
    }, 0x66DDFFFF, 0x00000000, "#")
    gfx2d.draw(gfx2d, w - 20, 10, mask)

    gfx2d.setLayer("text")
    gfx2d.text(24, 10, "GFX+", 0.95, 0.98, 1.0, 1.0, 2)
    gfx2d.text(24, 26, "circle polygon bitmap", 0.5, 0.9, 1.0, 0.95, 1)
    gfx2d.text(24, h - 28, "thick lines + wrapped center text layout", 0.92, 0.96, 1.0, 1.0, 1, w - 48, 20, "center", true)

    if (i % 24) < 12 then
        gfx2d.setLayerVisible("fx", true)
    else
        gfx2d.setLayerVisible("fx", false)
    end

    util.sleep(frameDelayMs)
end

gfx2d.setLayerVisible("fx", true)
gfx2d.removeLayer("fx")
gfx2d.clearLayer("shapes")
print("gfx2d demo complete")

