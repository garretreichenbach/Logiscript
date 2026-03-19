-- /bin/gfx_interactive.lua
-- Interactive 2D text graphics API demo.
-- Usage:
--   run /bin/gfx_interactive.lua
--   run /bin/gfx_interactive.lua all 250
--   run /bin/gfx_interactive.lua layers
--   run /bin/gfx_interactive.lua loop 250 3500
--
-- Modes:
--   help, list, shapes, colors, scales, layers, backend, all, loop

local mode = (args and args[1] or "all")
local delayMs = tonumber(args and args[2] or "") or 220
local durationMs = tonumber(args and args[3] or "") or 3200
local canvasUnavailable = false

if mode == nil or mode == "" then
    mode = "all"
end

mode = string.lower(tostring(mode))
if durationMs < 600 then
    durationMs = 600
end

local function sleep_ms(ms)
    if ms <= 0 then
        return
    end
    if util and util.sleep then
        util.sleep(ms)
    end
end

local function title(text)
    gfx.setColor("bright_white", "default")
    gfx.text(2, 2, text)
end

local function footer(text)
    gfx.setColor("bright_black", "default")
    gfx.text(2, gfx.getHeight() - 1, text)
end

local function beginScene(name)
    gfx.setBackend("canvas")
    if gfx.getBackend() ~= "canvas" then
        if not canvasUnavailable then
            canvasUnavailable = true
            gfx.setBackend("terminal")
            gfx.setAnsiEnabled(false)
            gfx.setSize(80, 24)
            gfx.clear(" ")
            gfx.setColor("bright_yellow", "default")
            gfx.text(1, 1, "Canvas backend is currently disabled.")
            gfx.setColor("white", "default")
            gfx.text(1, 3, "Enable 'gfx_canvas_backend_enabled: true' in:")
            gfx.text(1, 4, "moddata/LuaMade/config.yml")
            gfx.text(1, 6, "Then restart StarMade and rerun this script.")
            gfx.text(1, 8, "Tip: run /bin/gfx_interactive.lua backend")
            gfx.render()
            print("gfx_interactive: canvas backend unavailable; using terminal fallback.")
        end
        return false
    end

    gfx.setSize(72, 26)
    gfx.setCellScale(1.0)
    gfx.clear(" ")
    title("gfx interactive demo: " .. name)
    return true
end

local function finishScene(note)
    footer(note)
    gfx.render()
end

local function section_list()
    if not beginScene("list") then
        return
    end

    local y = 5
    local lines = {
        "This script is argument-driven.",
        "",
        "Run one mode:",
        "  run /bin/gfx_interactive.lua shapes",
        "  run /bin/gfx_interactive.lua colors",
        "  run /bin/gfx_interactive.lua layers",
        "",
        "Run all scenes with delay:",
        "  run /bin/gfx_interactive.lua all 250",
        "",
        "Tip: run /bin/gfx_demo.lua for smoke-test coverage.",
    }

    gfx.setColor("cyan", "default")
    for i = 1, #lines do
        gfx.text(3, y, lines[i])
        y = y + 1
    end

    finishScene("mode=list")
end

local function section_shapes()
    if not beginScene("shapes") then
        return
    end

    gfx.setColor("bright_black", "default")
    gfx.clear(".")

    gfx.setColor("bright_cyan", "default")
    gfx.rect(2, 4, 68, 20, "#")

    gfx.setColor("green", "default")
    gfx.fillRect(6, 7, 16, 8, "+")

    gfx.setColor("yellow", "default")
    gfx.line(5, 20, 40, 8, "*")

    gfx.setColor("magenta", "default")
    gfx.circle(34, 14, 6, "o")

    gfx.setColor("red", "default")
    gfx.fillCircle(52, 14, 5, "@")

    gfx.setColor("white", "default")
    gfx.text(4, 24, "rect, fillRect, line, circle, fillCircle, text")

    finishScene("mode=shapes")
end

local function section_colors()
    if not beginScene("colors") then
        return
    end

    gfx.setAnsiEnabled(true)

    local names = {
        "black", "red", "green", "yellow",
        "blue", "magenta", "cyan", "white",
        "bright_black", "bright_red", "bright_green", "bright_yellow",
        "bright_blue", "bright_magenta", "bright_cyan", "bright_white"
    }

    local y = 5
    for i = 1, #names do
        local c = names[i]
        gfx.setColor(c, "default")
        gfx.text(4, y, string.format("%-15s sample text", c))
        y = y + 1
    end

    gfx.setColor("white", "blue")
    gfx.text(42, 7, "setColor(fg,bg)")

    gfx.setForeground("bright_yellow")
    gfx.setBackground("red")
    gfx.text(42, 9, "setForeground + setBackground")

    gfx.resetColor()
    gfx.text(42, 11, "resetColor()")

    finishScene("mode=colors ansi=true")
end

local function section_scales()
    if not beginScene("scales") then
        return
    end

    gfx.setColor("bright_white", "default")
    gfx.text(4, 5, "Global cell scale")

    gfx.setCellScale(1.2)
    local s1 = gfx.getCellScale()
    gfx.text(4, 7, "setCellScale(1.2) => " .. tostring(s1[1]) .. "," .. tostring(s1[2]))

    gfx.setCellScale(1.0, 1.6)
    local s2 = gfx.getCellScale()
    gfx.text(4, 9, "setCellScale(1.0, 1.6) => " .. tostring(s2[1]) .. "," .. tostring(s2[2]))

    gfx.setCellScale(1.0)
    gfx.setColor("cyan", "default")
    gfx.pixelScaled(7, 13, "A", 1.5)
    gfx.pixelScaled(11, 13, "B", 2.0, 1.0)
    gfx.pixelScaled(15, 13, "C", "white", "blue", 1.25, 1.25)

    gfx.setPixelScale(19, 13, 2.0)
    gfx.pixel(19, 13, "D")

    gfx.setPixelScale(23, 13, 1.0, 2.0)
    gfx.pixel(23, 13, "E")

    local ps = gfx.getPixelScale(23, 13)
    gfx.setColor("bright_white", "default")
    gfx.text(4, 16, "getPixelScale(23,13) => " .. tostring(ps[1]) .. "," .. tostring(ps[2]))

    finishScene("mode=scales")
end

local function section_layers()
    if not beginScene("layers") then
        return
    end

    gfx.createLayer("bg")
    gfx.createLayer("mid")
    gfx.createLayer("fg")

    gfx.setLayer("bg")
    gfx.setColor("blue", "default")
    gfx.fillRect(1, 1, 72, 26, "~")
    gfx.setLayerScale("bg", 1.0)

    gfx.setLayer("mid")
    gfx.setColor("bright_black", "default")
    gfx.fillRect(5, 6, 62, 14, ":")
    gfx.setLayerScale("mid", 1.25, 1.0)

    gfx.setLayer("fg")
    gfx.setColor("bright_white", "default")
    gfx.rect(4, 5, 64, 16, "#")
    gfx.text(8, 10, "Layer API demo")
    gfx.text(8, 12, "active=" .. gfx.getActiveLayer())

    gfx.moveLayerDown("fg")
    gfx.moveLayerUp("fg")
    gfx.setLayerZ("fg", 2)

    local z = gfx.getLayerZ("fg")
    local sc = gfx.getLayerScale("mid")

    gfx.clearActiveLayer()
    gfx.setColor("bright_white", "default")
    gfx.text(3, 3, "layers: " .. table.concat(gfx.getLayerNames(), ", "))
    gfx.text(3, 4, "hasLayer(mid)=" .. tostring(gfx.hasLayer("mid")) .. " z(fg)=" .. tostring(z))
    gfx.text(3, 5, "scale(mid)=" .. tostring(sc[1]) .. "," .. tostring(sc[2]))

    finishScene("mode=layers")

    -- Cleanup layer state so future runs start clean.
    gfx.removeLayer("bg")
    gfx.removeLayer("mid")
    gfx.removeLayer("fg")
    gfx.clearActiveLayer()
end

local function section_backend()
    gfx.setSize(72, 26)
    gfx.setCellScale(1.0)
    gfx.clear(" ")

    gfx.setBackend("terminal")
    gfx.setAnsiEnabled(false)
    gfx.setColor("bright_green", "default")
    gfx.text(2, 2, "Backend demo: terminal")
    gfx.setColor("white", "default")
    gfx.text(2, 4, "Current backend=" .. tostring(gfx.getBackend()))
    gfx.text(2, 6, "This scene renders into normal terminal text output.")
    gfx.render()

    sleep_ms(math.max(120, delayMs))

    gfx.setBackend("canvas")
    if not beginScene("backend") then
        return
    end
    gfx.setColor("white", "default")
    gfx.text(3, 6, "Switched back to canvas backend.")
    gfx.text(3, 8, "Current backend=" .. tostring(gfx.getBackend()))
    finishScene("mode=backend")
end

local function show_help()
    gfx.setBackend("terminal")
    gfx.setSize(80, 24)
    gfx.clear(" ")
    gfx.setColor("bright_white", "default")
    gfx.text(1, 1, "gfx_interactive.lua")
    gfx.setColor("cyan", "default")
    gfx.text(1, 3, "Modes: help, list, shapes, colors, scales, layers, backend, all, loop")
    gfx.text(1, 5, "Examples:")
    gfx.text(3, 6, "run /bin/gfx_interactive.lua all 250")
    gfx.text(3, 7, "run /bin/gfx_interactive.lua layers")
    gfx.text(3, 8, "run /bin/gfx_interactive.lua backend")
    gfx.text(3, 9, "run /bin/gfx_interactive.lua loop 250 3500")
    gfx.render()
end

local function run_all_once()
    section_list()
    sleep_ms(delayMs)
    section_shapes()
    sleep_ms(delayMs)
    section_colors()
    sleep_ms(delayMs)
    section_scales()
    sleep_ms(delayMs)
    section_layers()
    sleep_ms(delayMs)
    section_backend()
end

local function run_loop_mode()
    -- Keep loop bounded so it stays inside server script time budgets.
    local startMs = (util and util.now) and util.now() or 0
    local cycles = 0

    repeat
        section_shapes()
        sleep_ms(delayMs)
        section_colors()
        sleep_ms(delayMs)
        section_scales()
        sleep_ms(delayMs)
        section_layers()
        sleep_ms(delayMs)
        cycles = cycles + 1

        if startMs <= 0 or not (util and util.now) then
            break
        end
    until (util.now() - startMs) >= durationMs

    -- Leave a clear canvas status frame at the end.
    beginScene("loop")
    gfx.setColor("bright_white", "default")
    gfx.text(4, 8, "Loop complete")
    gfx.text(4, 10, "cycles=" .. tostring(cycles))
    gfx.text(4, 12, "delayMs=" .. tostring(delayMs) .. " durationMs=" .. tostring(durationMs))
    finishScene("mode=loop")
end

if mode == "help" then
    show_help()
    return
end

if mode == "list" then
    section_list()
elseif mode == "shapes" then
    section_shapes()
elseif mode == "colors" then
    section_colors()
elseif mode == "scales" then
    section_scales()
elseif mode == "layers" then
    section_layers()
elseif mode == "backend" then
    section_backend()
elseif mode == "all" then
    run_all_once()
elseif mode == "loop" then
    run_loop_mode()
else
    print("Unknown mode: " .. tostring(mode))
    print("Try: run /bin/gfx_interactive.lua help")
end

print("gfx_interactive complete (mode=" .. mode .. ")")

