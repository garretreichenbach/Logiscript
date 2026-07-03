-- /bin/hud_demo.lua
-- A live cockpit HUD projected over the bridge glass.
-- Wraps a Projector block and paints real ship telemetry (shields, reactor,
-- speed, mass, sector) plus a top-down contacts radar onto a floating 2D
-- surface, refreshed a few times a second.
--
-- Usage: run /bin/hud_demo.lua [seconds]   (default 60; 0 = until canceled)

local seconds = tonumber(args[1]) or 0

-- ─── panel placement (blocks relative to the projector) ──────────────────────
local PANEL = {
    ox = 0.5, oy = 3.5, oz = -2.5, -- top-left corner
    nx = 0, ny = 0, nz = 0,       -- normal (which way it faces)
    ux = 0, uy = 1, uz = 0,       -- up vector
    worldW = 5.0, worldH = 2.0,   -- size in blocks
    canvasW = 360, canvasH = 120, -- logical pixels for 2D drawing
}

-- Radar auto-scales to fit the farthest contact, but never zooms in tighter
-- than this floor (blocks), so a single close contact isn't absurdly magnified.
local RADAR_MIN_RANGE = 200
local MAX_CONTACTS = 24 -- cap blips to stay well under command limits

-- Push the surface this many blocks off the glass along its normal, so the
-- projection doesn't z-fight with the coplanar window behind it.
local SURFACE_NUDGE = 0.01

-- Backdrop opacity for the whole panel. 0 = fully see-through the window;
-- raise it for a tinted glass look (e.g. 0.2) if text needs more contrast.
local BG_ALPHA = 0.0

-- ─── projector + ship ────────────────────────────────────────────────────────
local block = peripheral.getRelative("bottom")
assert(block, "No block in front of the computer. Place a Projector block there.")
local proj = peripheral.wrap(block, "projector")
assert(proj, "Front block is not a Projector.")

local self = console.getBlock():getEntity()

-- Read a value through a getter chain, returning `default` if anything is nil.
local function safe(fn, default)
    local ok, v = pcall(fn)
    if ok and v ~= nil then return v end
    return default
end

local myFaction = safe(function() return self:getFaction() end, nil)
print("HUD online for " .. (seconds == 0 and "ever" or seconds .. "s") .. " on " .. tostring(self:getName()))

-- ─── helpers ─────────────────────────────────────────────────────────────────

local function clamp01(v)
    if v == nil or v ~= v then return 0 end -- nil / NaN guard
    if v < 0 then return 0 end
    if v > 1 then return 1 end
    return v
end

-- Round to `dp` decimals without string.format (this Lua ignores %f precision).
local function round(x, dp)
    local m = 10 ^ (dp or 0)
    return math.floor(x * m + 0.5) / m
end

-- Compact number formatting: 1234567 -> "1.2M". Built with tostring(round(..))
-- because string.format here does not honor precision specifiers.
local function fmt(n)
    n = n or 0
    local a = math.abs(n)
    if a >= 1e9 then return tostring(round(n / 1e9, 1)) .. "G" end
    if a >= 1e6 then return tostring(round(n / 1e6, 1)) .. "M" end
    if a >= 1e3 then return tostring(round(n / 1e3, 1)) .. "K" end
    return tostring(round(n, 0))
end

-- A labeled stat row: title (left), value (right of it), and a fill bar under it.
local function statRow(surf, x, y, w, title, valueStr, frac, r, g, b)
    surf.text(x, y, title, 0.55, 0.85, 1.0, 1.0, 1)
    surf.text(x + 118, y, valueStr, 0.9, 0.95, 1.0, 1.0, 1)
    local by, bh = y + 11, 8
    surf.rect(x, by, w, bh, 0.06, 0.10, 0.16, 0.9, true)                     -- track
    surf.rect(x, by, math.floor(w * clamp01(frac)), bh, r, g, b, 1.0, true)  -- fill
    surf.rect(x, by, w, bh, 0.25, 0.45, 0.7, 0.8, false)                    -- border
end

-- ─── telemetry snapshot ──────────────────────────────────────────────────────
local function sample()
    local s = {}
    s.name = tostring(safe(function() return self:getName() end, "UNKNOWN"))
    -- getSector()'s object has no Lua tostring, so build it from components.
    local sec = safe(function() return self:getSector() end, nil)
    if sec then
        s.sector = tostring(safe(function() return sec:getX() end, "?")) .. ","
            .. tostring(safe(function() return sec:getY() end, "?")) .. ","
            .. tostring(safe(function() return sec:getZ() end, "?"))
    else
        s.sector = "?"
    end
    s.mass = safe(function() return self:getMass() end, 0)
    s.speed = safe(function() return self:getSpeed() end, 0)

    local thrust = safe(function() return self:getThrust() end, nil)
    s.maxSpeed = thrust and safe(function() return thrust:getMaxSpeed() end, 0) or 0

    local shields = safe(function() return self:getShieldSystem() end, nil)
    s.shCur = shields and safe(function() return shields:getCurrent() end, 0) or 0
    s.shCap = shields and safe(function() return shields:getCapacity() end, 0) or 0

    if safe(function() return self:hasReactor() end, false) then
        local r = safe(function() return self:getReactor() end, nil)
        if r then
            s.reHP = safe(function() return r:getHP() end, 0)
            s.reMax = safe(function() return r:getMaxHP() end, 0)
            s.reCharge = safe(function() return r:getRecharge() end, 0)
            s.reUse = safe(function() return r:getConsumption() end, 0)
        end
    end
    s.reHP = s.reHP or 0
    s.reMax = s.reMax or 0
    s.reCharge = s.reCharge or 0
    s.reUse = s.reUse or 0

    -- Nearby contacts, projected into the ship's forward/right plane (top-down).
    s.contacts = {}
    s.nearest = nil
    s.farthest = 0
    local list = safe(function() return self:getNearbyEntities() end, nil)
    if list then
        -- Ship basis: forward heading, up, and right = heading x up.
        local hx, hy, hz = 0, 0, 1
        local h = safe(function() return self:getHeading() end, nil)
        if h then hx, hy, hz = h:getX(), h:getY(), h:getZ() end
        local ux, uy, uz = 0, 1, 0
        local u = safe(function() return self:getUp() end, nil)
        if u then ux, uy, uz = u:getX(), u:getY(), u:getZ() end
        local rvx = hy * uz - hz * uy
        local rvy = hz * ux - hx * uz
        local rvz = hx * uy - hy * ux

        local sp = safe(function() return self:getPos() end, nil)
        local spx, spy, spz = 0, 0, 0
        if sp then spx, spy, spz = sp:getX(), sp:getY(), sp:getZ() end

        for _, c in ipairs(list) do
            if #s.contacts >= MAX_CONTACTS then break end
            local cp = safe(function() return c:getPos() end, nil)
            if cp then
                local dx, dy, dz = cp:getX() - spx, cp:getY() - spy, cp:getZ() - spz
                local dist = math.sqrt(dx * dx + dy * dy + dz * dz)
                -- 0 = neutral/unknown, 1 = friendly, 2 = hostile.
                local rel = 0
                local cf = safe(function() return c:getFaction() end, nil)
                if myFaction and cf then
                    if safe(function() return myFaction:isEnemy(cf) end, false) then
                        rel = 2
                    elseif safe(function() return myFaction:isFriend(cf) end, false)
                        or safe(function() return myFaction:isSameFaction(cf) end, false) then
                        rel = 1
                    end
                end
                table.insert(s.contacts, {
                    fwd = dx * hx + dy * hy + dz * hz,
                    rgt = dx * rvx + dy * rvy + dz * rvz,
                    dist = dist,
                    rel = rel,
                })
                if not s.nearest or dist < s.nearest then s.nearest = dist end
                if dist > s.farthest then s.farthest = dist end
            end
        end
    end
    return s
end

-- ─── radar widget ────────────────────────────────────────────────────────────
local function drawRadar(surf, cx, cy, R, contacts, range)
    surf.circle(cx, cy, R, 0.20, 0.55, 0.45, 0.5, false, 40)
    surf.circle(cx, cy, R * 0.5, 0.20, 0.55, 0.45, 0.35, false, 32)
    surf.line(cx - R, cy, cx + R, cy, 0.20, 0.55, 0.45, 0.35)
    surf.line(cx, cy - R, cx, cy + R, 0.20, 0.55, 0.45, 0.35)
    -- Own ship: a small triangle at center pointing "forward" (up).
    surf.polygon({ cx, cy - 6, cx - 4, cy + 4, cx + 4, cy + 4 }, 0.6, 0.9, 1.0, 1.0, true)

    for _, ct in ipairs(contacts) do
        local pmag = math.sqrt(ct.rgt * ct.rgt + ct.fwd * ct.fwd)
        local bx, by = cx, cy
        if pmag > 0.001 then
            local rr = math.min(R, (ct.dist / range) * R)
            if rr < 3 then rr = 3 end
            bx = cx + (ct.rgt / pmag) * rr
            by = cy - (ct.fwd / pmag) * rr
        end
        local r, g, b = 0.90, 0.85, 0.30 -- neutral / unknown
        if ct.rel == 2 then
            r, g, b = 1.00, 0.25, 0.25    -- hostile
        elseif ct.rel == 1 then
            r, g, b = 0.30, 0.90, 0.40    -- friendly
        end
        surf.rect(bx - 2, by - 2, 4, 4, r, g, b, 1.0, true)
    end
end

-- ─── render one frame ────────────────────────────────────────────────────────
local function draw(blink)
    local s = sample()
    local frame = proj.newFrame()
    frame.setLayer("hud")

    -- Nudge the origin off the glass along the (normalized) normal to kill z-fighting.
    local nlen = math.sqrt(PANEL.nx * PANEL.nx + PANEL.ny * PANEL.ny + PANEL.nz * PANEL.nz)
    if nlen == 0 then nlen = 1 end
    local surf = frame.newSurface(
        PANEL.ox + PANEL.nx / nlen * SURFACE_NUDGE,
        PANEL.oy + PANEL.ny / nlen * SURFACE_NUDGE,
        PANEL.oz + PANEL.nz / nlen * SURFACE_NUDGE,
        PANEL.nx, PANEL.ny, PANEL.nz,
        PANEL.ux, PANEL.uy, PANEL.uz,
        PANEL.worldW, PANEL.worldH,
        PANEL.canvasW, PANEL.canvasH)

    local W, H = PANEL.canvasW, PANEL.canvasH
    local pad = 8
    local divX = 232 -- stats | radar split

    -- Backdrop (optional tint) + border. Keep the panel see-through by default.
    if BG_ALPHA > 0 then
        surf.rect(0, 0, W, H, 0.02, 0.05, 0.10, BG_ALPHA, true)
    end
    surf.rect(1, 1, W - 2, H - 2, 0.20, 0.55, 1.00, 0.9, false)

    -- Header bar (full width) — kept lightly tinted so the title stays readable.
    surf.rect(0, 0, W, 18, 0.06, 0.14, 0.24, 0.55, true)
    surf.text(pad, 5, s.name, 0.8, 0.95, 1.0, 1.0, 1)
    if blink then
        surf.rect(W - 16, 5, 8, 8, 0.2, 1.0, 0.5, 1.0, true)
    end

    -- Vertical divider between stats and radar.
    surf.line(divX, 20, divX, H - 4, 0.20, 0.40, 0.60, 0.6)

    -- Left: stat rows.
    local sx, sw = pad, divX - pad * 2
    local shFrac = s.shCap > 0 and s.shCur / s.shCap or 0
    statRow(surf, sx, 26, sw, "SHIELDS",
        fmt(s.shCur) .. " / " .. fmt(s.shCap), shFrac, 0.20, 0.70, 1.00)

    local reFrac = s.reMax > 0 and s.reHP / s.reMax or 0
    statRow(surf, sx, 50, sw, "REACTOR",
        fmt(s.reHP) .. " / " .. fmt(s.reMax), reFrac, 1.00, 0.55, 0.20)

    local spFrac = s.maxSpeed > 0 and s.speed / s.maxSpeed or 0
    statRow(surf, sx, 74, sw, "SPEED",
        fmt(s.speed) .. " / " .. fmt(s.maxSpeed) .. " M/S", spFrac, 0.30, 0.90, 0.55)

    surf.text(sx, 96, "PWR +" .. fmt(s.reCharge) .. "  -" .. fmt(s.reUse), 0.6, 0.85, 1.0, 1.0, 1)
    surf.text(sx, 107, "MASS " .. fmt(s.mass) .. "   SECTOR " .. s.sector, 0.6, 0.85, 1.0, 1.0, 1)

    -- Right: contacts radar. Auto-fit range to the farthest contact (with floor).
    local range = math.max(RADAR_MIN_RANGE, s.farthest * 1.1)
    surf.text(divX + 8, 22, "CONTACTS " .. #s.contacts, 0.6, 0.85, 1.0, 1.0, 1)
    drawRadar(surf, 296, 70, 40, s.contacts, range)
    local nearStr = s.nearest and ("NR " .. fmt(s.nearest) .. "  RNG " .. fmt(range)) or "NO CONTACTS"
    surf.text(divX + 8, 110, nearStr, 0.6, 0.85, 1.0, 1.0, 1)

    proj.publish(frame)
end

-- ─── loop ────────────────────────────────────────────────────────────────────
local refreshMs = 250 -- 4 Hz; stats don't move fast
local ticks = seconds == 0 and math.huge or math.floor(seconds * 1000 / refreshMs)

local i = 0
while i < ticks do
    draw((i % 2) == 0) -- toggle the online dot each tick
    util.sleep(refreshMs)
    i = i + 1
end

proj.clear()
print("HUD demo complete")
