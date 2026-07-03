-- /bin/projector_demo.lua
-- World-space projector showcase for the gfx3d API.
-- Draws a ground grid, a spinning wireframe cube hologram, and a floating
-- status panel, then animates by republishing a few frames per second.
--
-- The whole scene is rotated by SCENE_YAW about the vertical axis before it is
-- drawn, so the projection can be re-oriented without touching each shape.
--
-- Setup: place a Projector block directly in FRONT of this computer.
-- Usage:  run /bin/projector_demo.lua [seconds]

local seconds = tonumber(args[1]) or 12
if seconds < 1 then
    seconds = 1
end

-- Whole-scene orientation: rotate the entire projection about the vertical (Y)
-- axis. A 90 degree turn maps the +-5 bounds box onto itself, so nothing that
-- fits before the turn goes out of range after it.
local SCENE_YAW = -math.pi / 2 -- -90 degrees

-- Wrap the projector sitting in front of the computer.
local block = peripheral.getRelative("front")
assert(block, "No block in front of the computer. Place a Projector block there.")
local proj = peripheral.wrap(block, "projector")
assert(proj, "Front block is not a Projector.")

local maxOffset = proj.getMaxOffset()
print("projector demo for " .. seconds .. "s (clamp +-" .. maxOffset .. " blocks)")

-- Republish rate. gfx3d suits maps/HUDs, not 60fps, so keep it modest.
local fps = 12
local frameDelayMs = math.floor(1000 / fps)
local totalFrames = math.floor(seconds * fps)

-- Unit cube corners; we spin these about Y each frame, then scale + lift.
local CUBE = {
    { -1, -1, -1 }, { 1, -1, -1 }, { 1, -1, 1 }, { -1, -1, 1 },
    { -1, 1, -1 }, { 1, 1, -1 }, { 1, 1, 1 }, { -1, 1, 1 },
}
-- 12 edges as index pairs into CUBE (1-based).
local EDGES = {
    { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 1 }, -- bottom
    { 5, 6 }, { 6, 7 }, { 7, 8 }, { 8, 5 }, -- top
    { 1, 5 }, { 2, 6 }, { 3, 7 }, { 4, 8 }, -- verticals
}

local CUBE_SCALE = 1.4
local CUBE_LIFT = 2.6 -- blocks above the projector

-- Rotate (x,y,z) about the Y axis. Linear, so it also transforms direction
-- vectors (surface normal/up) correctly.
local function rotY(x, y, z, angle)
    local c, s = math.cos(angle), math.sin(angle)
    return c * x + s * z, y, -s * x + c * z
end

-- Apply the whole-scene orientation to a point or direction.
local function scene(x, y, z)
    return rotY(x, y, z, SCENE_YAW)
end

for i = 0, totalFrames do
    local t = i / fps
    local angle = t * 1.2 -- cube spin, radians/sec

    local frame = proj.newFrame()

    -- Ground grid on the y=0 plane.
    frame.setLayer("grid")
    frame.createLayer("grid", 0)
    for g = -5, 5 do
        local ax, ay, az = scene(g, 0, -5)
        local bx, by, bz = scene(g, 0, 5)
        frame.line3d(ax, ay, az, bx, by, bz, 0.15, 0.35, 0.55, 0.5)
        local cx, cy, cz = scene(-5, 0, g)
        local dx, dy, dz = scene(5, 0, g)
        frame.line3d(cx, cy, cz, dx, dy, dz, 0.15, 0.35, 0.55, 0.5)
    end

    -- Spinning wireframe cube hologram.
    frame.setLayer("holo")
    frame.createLayer("holo", 5)
    local pts = {}
    for v = 1, #CUBE do
        local x, y, z = rotY(CUBE[v][1], CUBE[v][2], CUBE[v][3], angle)
        local sx, sy, sz = scene(x * CUBE_SCALE, y * CUBE_SCALE + CUBE_LIFT, z * CUBE_SCALE)
        pts[v] = { sx, sy, sz }
    end
    local pulse = 0.6 + 0.4 * math.sin(t * 3.0)
    for _, e in ipairs(EDGES) do
        local a, b = pts[e[1]], pts[e[2]]
        frame.line3d(a[1], a[2], a[3], b[1], b[2], b[3], 0.2, 0.9 * pulse, 1.0, 1.0, 2)
    end
    -- A small marker point at the cube's center.
    local mx, my, mz = scene(0, CUBE_LIFT, 0)
    frame.point3d(mx, my, mz, 1.0, 0.5, 0.2, 1.0)

    -- Floating status panel, centered above the grid (rotated with the scene).
    frame.setLayer("panel")
    frame.createLayer("panel", 10)
    local ox, oy, oz = scene(2, 4.6, 2)   -- top-left corner
    local nx, ny, nz = scene(0, 0, 1)     -- normal (facing direction)
    local ux, uy, uz = scene(0, 1, 0)     -- up
    local surf = frame.newSurface(ox, oy, oz, nx, ny, nz, ux, uy, uz, 4.0, 1.6, 128, 52)
    surf.rect(0, 0, 128, 52, 0.02, 0.05, 0.10, 0.85, true)
    surf.rect(0, 0, 128, 52, 0.20, 0.60, 1.00, 0.9, false)
    surf.text(6, 6, "HOLO PROJECTOR", 0.6, 0.95, 1.0, 1.0, 1)
    -- A little progress bar driven by demo time.
    local pct = i / math.max(1, totalFrames)
    surf.rect(6, 30, 116, 12, 0.10, 0.20, 0.30, 0.9, true)
    surf.rect(6, 30, math.floor(116 * pct), 12, 0.20, 0.90, 0.55, 1.0, true)

    proj.publish(frame)
    util.sleep(frameDelayMs)
end

-- Leave a clean static frame up at the end.
local finalFrame = proj.newFrame()
finalFrame.setLayer("holo")
-- An axis-aligned cube is unchanged by a 90 degree yaw, so draw it directly.
finalFrame.boxWire(-CUBE_SCALE, CUBE_LIFT - CUBE_SCALE, -CUBE_SCALE,
    CUBE_SCALE * 2, CUBE_SCALE * 2, CUBE_SCALE * 2, 0.2, 0.9, 1.0, 1.0, 2)
local eox, eoy, eoz = scene(2, 4.6, 2)
local enx, eny, enz = scene(0, 0, 1)
local eux, euy, euz = scene(0, 1, 0)
local endSurf = finalFrame.newSurface(eox, eoy, eoz, enx, eny, enz, eux, euy, euz, 4.0, 1.6, 128, 52)
endSurf.rect(0, 0, 128, 52, 0.02, 0.05, 0.10, 0.85, true)
endSurf.rect(0, 0, 128, 52, 0.20, 0.60, 1.00, 0.9, false)
endSurf.text(6, 6, "STANDBY", 0.6, 0.95, 1.0, 1.0, 1)
proj.publish(finalFrame)

print("projector demo complete")
