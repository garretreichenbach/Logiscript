-- /bin/gfx_demo.lua
-- Demo + smoke test for the 2D text graphics API (`gfx`).
-- Usage:
--   run /bin/gfx_demo.lua          -- quick run
--   run /bin/gfx_demo.lua 200      -- 200ms pause between scenes

local delayMs = tonumber(args and args[1] or "") or 0

local results = {}

local function sleep_ms(ms)
    if ms <= 0 then
        return
    end

    if util and util.sleep then
        util.sleep(ms)
    end
end

local function remember(name, ok, err)
    results[#results + 1] = {
        name = name,
        ok = ok,
        err = err,
    }
end

local function check(name, fn)
    local ok, err = pcall(fn)
    remember(name, ok, err)
    return ok
end

local function draw_basic_scene()
    gfx.setSize(64, 24)
    gfx.clear(".", "bright_black", "default")

    gfx.setColor("bright_white", "default")
    gfx.text(2, 2, "LuaMade gfx demo")

    gfx.setForeground("bright_cyan")
    gfx.rect(1, 1, 64, 24, "#")

    gfx.setColor("yellow", "default")
    gfx.line(3, 5, 40, 12, "*")

    gfx.setColor("green", "default")
    gfx.fillRect(45, 4, 16, 6, "+")

    gfx.setColor("bright_magenta", "default")
    gfx.circle(16, 16, 5, "o")

    gfx.setColor("red", "default")
    gfx.fillCircle(30, 17, 4, "@")

    gfx.setColor("white", "blue")
    gfx.pixel(50, 14, "X")
    gfx.pixel(51, 14, "Y", "black", "bright_yellow")

    gfx.pixelScaled(53, 14, "S", 1.5)
    gfx.pixelScaled(55, 14, "T", 1.0, 2.0)
    gfx.pixelScaled(57, 14, "U", "bright_white", "red", 1.2, 1.2)

    gfx.setPixelScale(50, 14, 2.0)
    gfx.setPixelScale(51, 14, 1.0, 1.5)

    gfx.setColor("bright_white", "default")
    local pscale = gfx.getPixelScale(50, 14)
    gfx.text(3, 22, "pixel scale @50,14 = " .. tostring(pscale[1]) .. "," .. tostring(pscale[2]))

    gfx.render()
end

local function draw_layer_scene()
    gfx.setSize(64, 24)
    gfx.clear(" ")

    gfx.createLayer("bg")
    gfx.createLayer("mid")
    gfx.createLayer("fg")

    gfx.setLayer("bg")
    gfx.setColor("blue", "default")
    gfx.fillRect(1, 1, 64, 24, "~")
    gfx.setLayerScale("bg", 1.0)

    gfx.setLayer("mid")
    gfx.setColor("bright_black", "default")
    gfx.fillRect(4, 4, 56, 16, ".")
    gfx.setLayerScale("mid", 1.3, 1.0)

    gfx.setLayer("fg")
    gfx.setColor("bright_white", "default")
    gfx.rect(3, 3, 58, 18, "#")
    gfx.text(8, 8, "Layered Canvas Demo")
    gfx.text(8, 10, "active layer: " .. gfx.getActiveLayer())

    -- Reorder layer stack and clear one layer to exercise management methods.
    gfx.moveLayerDown("fg")
    gfx.moveLayerUp("fg")
    gfx.setLayerZ("fg", 2)
    local fgScale = gfx.getLayerScale("fg")
    local fgZ = gfx.getLayerZ("fg")

    gfx.clearLayer("mid")
    gfx.setLayer("mid")
    gfx.setColor("bright_black", "default")
    gfx.fillRect(6, 6, 52, 12, ":")

    gfx.clearActiveLayer()
    gfx.setColor("bright_white", "default")
    gfx.text(2, 2, "layers: " .. table.concat(gfx.getLayerNames(), ", "))
    gfx.text(2, 3, "fg scale=" .. tostring(fgScale[1]) .. "," .. tostring(fgScale[2]) .. " z=" .. tostring(fgZ))

    gfx.render()

    -- Cleanup API coverage
    gfx.removeLayer("mid")
end

check("getWidth/getHeight", function()
    local _w = gfx.getWidth()
    local _h = gfx.getHeight()
end)

check("setBackend('terminal') + getBackend", function()
    gfx.setBackend("terminal")
    local _b = gfx.getBackend()
end)

check("setAnsiEnabled + isAnsiEnabled", function()
    gfx.setAnsiEnabled(true)
    local _a = gfx.isAnsiEnabled()
    gfx.setAnsiEnabled(false)
end)

check("setColor(fg)", function()
    gfx.setColor("cyan")
end)

check("setColor(fg,bg)", function()
    gfx.setColor("white", "black")
end)

check("setForeground/setBackground/resetColor", function()
    gfx.setForeground("yellow")
    gfx.setBackground("blue")
    gfx.resetColor()
end)

check("setCellScale(scale)", function()
    gfx.setCellScale(1.1)
end)

check("setCellScale(scaleX,scaleY) + getCellScale", function()
    gfx.setCellScale(1.0, 1.0)
    local _s = gfx.getCellScale()
end)

check("clear()", function()
    gfx.clear()
end)

check("clear(fill)", function()
    gfx.clear(" ")
end)

check("clear(fill,fg,bg)", function()
    gfx.clear(".", "bright_black", "default")
end)

check("draw primitives + pixel scales + text + render", function()
    draw_basic_scene()
end)

sleep_ms(delayMs)

check("frame()", function()
    local _frame = gfx.frame()
end)

check("layer management + render", function()
    gfx.setBackend("canvas")
    draw_layer_scene()
end)

sleep_ms(delayMs)

check("hasLayer/getLayerNames/getActiveLayer", function()
    local _hasBg = gfx.hasLayer("bg")
    local _active = gfx.getActiveLayer()
    local _names = gfx.getLayerNames()
end)

check("setLayer('') + clearActiveLayer", function()
    gfx.setLayer("")
    gfx.clearActiveLayer()
end)

-- Final report in terminal backend for easy reading.
gfx.setBackend("terminal")
gfx.setSize(80, 24)
gfx.clear(" ")
gfx.setColor("bright_white", "default")
gfx.text(1, 1, "gfx_demo.lua - 2D text graphics API report")
gfx.text(1, 2, "delayMs=" .. tostring(delayMs) .. "  backend=" .. tostring(gfx.getBackend()))

local row = 4
local failures = 0
for i, r in ipairs(results) do
    local status = r.ok and "OK" or "FAIL"
    if not r.ok then
        failures = failures + 1
    end

    local line = string.format("[%s] %s", status, r.name)
    if #line > 78 then
        line = string.sub(line, 1, 78)
    end
    gfx.text(1, row, line)
    row = row + 1

    if row <= 23 and (not r.ok) and r.err then
        local errLine = "  -> " .. tostring(r.err)
        if #errLine > 78 then
            errLine = string.sub(errLine, 1, 78)
        end
        gfx.text(1, row, errLine)
        row = row + 1
    end

    if row > 23 then
        break
    end
end

gfx.text(1, 24, failures == 0 and "All demo checks passed." or ("Failures: " .. tostring(failures) .. " (rerun and inspect output)."))
gfx.render()

print("gfx_demo complete. failures=" .. tostring(failures))

