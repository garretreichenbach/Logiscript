-- /bin/sectormap.lua
-- In-world 3D galaxy/sector map, projected above a Projector block.
--
-- Scans the sectors around this ship with the `galaxy` API and renders each as
-- a floating marker in a static holographic cube: stars, planets, asteroids,
-- stations and black holes are colour-coded, your own sector pulses at the
-- centre, and a side panel lists what the scan found plus any contacts sharing
-- your sector.
--
-- Setup: place a Projector block directly BELOW this computer (or pass a side).
-- Usage:  run /bin/sectormap.lua [radius] [scale] [seconds] [side]
--           radius   sectors scanned on each axis   (default 3, max 3)
--           scale    projection size, 1.0 = fills clamp box (default 0.5)
--           seconds  run time, 0 = run forever      (default 30)
--           side     projector side                 (default "bottom")

local radius  = math.floor(tonumber(args[1]) or 3)
local scale   = tonumber(args[2]) or 0.5
local seconds = tonumber(args[3]) or 30
local side    = args[4] or "bottom"
if radius < 1 then radius = 1 end
if radius > 3 then radius = 3 end   -- galaxy.scanSectors clamps here too
if scale < 0.1 then scale = 0.1 end
if scale > 1.0 then scale = 1.0 end

-- ─── projector + ship ────────────────────────────────────────────────────────
local block = peripheral.getRelative(side)
assert(block, "No block on the '" .. side .. "' side. Place a Projector there.")
local proj = peripheral.wrap(block, "projector")
assert(proj, "The '" .. side .. "' block is not a Projector.")

local self = console.getBlock():getEntity()
assert(self, "Could not resolve this computer's entity.")

local maxOffset = proj.getMaxOffset()
-- One sector = `cell` blocks. `scale` shrinks the whole projection uniformly;
-- at scale 1.0 the cube nearly fills the projector's clamp box.
local cell = (maxOffset * 0.9) / (radius + 0.5) * scale

-- ─── helpers ─────────────────────────────────────────────────────────────────
local function safe(fn, default)
    local ok, v = pcall(fn)
    if ok and v ~= nil then return v end
    return default
end

local function vecStr(v)
    if not v then return "?" end
    return tostring(safe(function() return v:getX() end, "?")) .. ","
        .. tostring(safe(function() return v:getY() end, "?")) .. ","
        .. tostring(safe(function() return v:getZ() end, "?"))
end

-- Marker colour + draw style per sector type. Anything not listed is skipped.
local STYLE = {
    SUN         = { 1.00, 0.85, 0.20, "box" },
    GIANT       = { 1.00, 0.65, 0.15, "box" },
    DOUBLE_STAR = { 1.00, 0.55, 0.10, "box" },
    BLACK_HOLE  = { 0.65, 0.25, 0.95, "box" },
    PLANET      = { 0.30, 0.80, 0.45, "box" },
    SPACE_STATION = { 1.00, 0.40, 0.10, "wire" },
    ASTEROID    = { 0.65, 0.65, 0.65, "point" },
    LOW_ASTEROID = { 0.50, 0.50, 0.55, "point" },
    MAIN        = { 0.35, 0.65, 1.00, "point" },
}

-- ─── scan → cached markers ───────────────────────────────────────────────────
-- Scanning every frame would be wasteful, so we scan on an interval and reuse
-- the cached markers between scans.
local center, markers, counts

local function rescan()
    center = self:getSector()
    local cx = safe(function() return center:getX() end, 0)
    local cy = safe(function() return center:getY() end, 0)
    local cz = safe(function() return center:getZ() end, 0)

    markers = {}
    counts = { populated = 0, stars = 0, planets = 0, stations = 0 }

    for _, info in ipairs(galaxy.scanSectors(center, radius)) do
        local t = info:getSectorType()
        local style = t and STYLE[t]
        if style then
            local p = info:getPos()
            local dx = safe(function() return p:getX() end, cx) - cx
            local dy = safe(function() return p:getY() end, cy) - cy
            local dz = safe(function() return p:getZ() end, cz) - cz
            markers[#markers + 1] = {
                x = dx * cell, y = dy * cell, z = dz * cell,
                r = style[1], g = style[2], b = style[3], kind = style[4],
            }
            counts.populated = counts.populated + 1
            if t == "PLANET" then counts.planets = counts.planets + 1
            elseif t == "SPACE_STATION" then counts.stations = counts.stations + 1
            elseif style[4] == "box" then counts.stars = counts.stars + 1 end
        end
    end

    -- Contacts currently sharing our sector.
    counts.contacts = 0
    for _, e in ipairs(galaxy.getEntitiesInSector(center)) do
        if e then counts.contacts = counts.contacts + 1 end
    end
    counts.coords = vecStr(center)

    local sys = safe(function() return galaxy.getSystem(center) end, nil)
    counts.system = sys and tostring(safe(function() return sys:getName() end, "?")) or "unknown"
    counts.owner = "unclaimed"
    if sys and safe(function() return sys:isClaimed() end, false) then
        local owner = safe(function() return sys:getOwnerFaction() end, nil)
        counts.owner = owner and tostring(safe(function() return owner:getName() end, "?")) or "?"
    end
end

-- ─── render ──────────────────────────────────────────────────────────────────
local fps = 12
local frameDelayMs = math.floor(1000 / fps)
local forever = seconds == 0
local totalFrames = forever and math.huge or math.floor(seconds * fps)
local rescanEveryFrames = fps * 2   -- re-scan every ~2s

local half = radius * cell          -- scan-cube half-extent in blocks
local mark = cell * 0.22            -- marker half-size
local youHalf = cell * 0.3          -- your-sector marker half-size

print(string.format("sectormap: r=%d scale=%.2f cell=%.2f (clamp +-%.1f)", radius, scale, cell, maxOffset))
rescan()

local i = 0
while forever or i <= totalFrames do
    if i > 0 and (i % rescanEveryFrames) == 0 then rescan() end

    local pulse = 0.55 + 0.45 * math.sin((i / fps) * 3.0)

    local frame = proj.newFrame()

    -- Reference frame: faint bounding cube of the scanned volume.
    frame.setLayer("frame")
    frame.createLayer("frame", 0)
    frame.boxWire(-half, -half, -half, half * 2, half * 2, half * 2, 0.15, 0.35, 0.5, 0.35)

    -- Markers.
    frame.setLayer("markers")
    frame.createLayer("markers", 5)
    for _, m in ipairs(markers) do
        if m.kind == "point" then
            frame.point3d(m.x, m.y, m.z, m.r, m.g, m.b, 1.0)
        elseif m.kind == "wire" then
            frame.boxWire(m.x - mark, m.y - mark, m.z - mark, mark * 2, mark * 2, mark * 2, m.r, m.g, m.b, 1.0, 1.5)
        else
            frame.boxFilled(m.x - mark, m.y - mark, m.z - mark, mark * 2, mark * 2, mark * 2, m.r, m.g, m.b, 0.9)
        end
    end

    -- Your sector: a pulsing marker at the centre.
    frame.setLayer("you")
    frame.createLayer("you", 8)
    frame.boxWire(-youHalf, -youHalf, -youHalf, youHalf * 2, youHalf * 2, youHalf * 2, 1.0, 1.0, 1.0, pulse, 2)
    frame.point3d(0, 0, 0, 1.0, 0.9, 0.3, 1.0)

    -- Side panel: legend + scan summary, sitting just past the +X face.
    frame.setLayer("panel")
    frame.createLayer("panel", 10)
    local surf = frame.newSurface(
        half + cell * 0.6, half * 0.4, -half,   -- origin (top-left corner)
        0, 0, 1,                                 -- normal (faces +Z)
        0, 1, 0,                                 -- up
        4.2 * scale, 3.0 * scale, 140, 100)
    surf.rect(0, 0, 140, 100, 0.02, 0.05, 0.10, 0.85, true)
    surf.rect(0, 0, 140, 100, 0.20, 0.60, 1.00, 0.9, false)
    surf.text(6, 5, "SECTOR MAP", 0.6, 0.95, 1.0, 1.0, 1)
    surf.text(6, 18, "sys " .. counts.system, 0.6, 0.85, 1.0, 1.0, 1)
    surf.text(6, 28, "at  " .. counts.coords, 0.55, 0.8, 0.95, 1.0, 1)
    surf.text(6, 38, "own " .. counts.owner, 0.55, 0.8, 0.7, 1.0, 1)
    surf.text(6, 52, "stars    " .. counts.stars, 1.0, 0.85, 0.3, 1.0, 1)
    surf.text(6, 62, "planets  " .. counts.planets, 0.35, 0.85, 0.5, 1.0, 1)
    surf.text(6, 72, "stations " .. counts.stations, 1.0, 0.5, 0.2, 1.0, 1)
    surf.text(6, 82, "contacts " .. counts.contacts, 1.0, 0.4, 0.4, 1.0, 1)

    proj.publish(frame)
    util.sleep(frameDelayMs)
    i = i + 1
end

-- Clear the projection on exit.
proj.publish(proj.newFrame())
print("sectormap complete")
