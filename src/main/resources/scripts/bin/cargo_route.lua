-- CargoRouteBasic.lua
-- Drives a cargo ship from a pickup station to a delivery station using
-- fleet sector movement, local approach, and typed load/unload rail docking.
--
-- Usage: cargo_route <shiName> <pickupSector> <deliverySector>
--   pickupSector and deliverySector are formatted as "x,y,z"
--
-- The ship running this script must:
--  - Have AI enabled
--  - Have a RailDocker block (provide its position as DOCKER_POS below)
--  - Be in a faction friendly with both stations

local DOCKER_POS   = vec3i(0, 0, 0)    -- local-space position of your RailDocker block
local APPROACH_RADIUS = 30             -- stop within this many blocks of a station
local DOCK_TIMEOUT_SECS = 30           -- how long to wait for a dock before giving up

-- ─── helpers ────────────────────────────────────────────────────────────────

local function parseSector(str)
    local x, y, z = str:match("(-?%d+),(-?%d+),(-?%d+)")
    assert(x, "Bad sector format (expected x,y,z): " .. tostring(str))
    return vec3i(tonumber(x), tonumber(y), tonumber(z))
end

local function findEntityByName(name)
    local entity = console.getBlock():getEntity()
    for _, nearby in ipairs(entity:getNearbyEntities(3)) do
        if nearby:getName() == name then return nearby end
    end
    return nil
end

local function awaitDocked(entity, stationRef, timeoutSecs)
    local deadline = os.time() + timeoutSecs
    while os.time() < deadline do
        if entity:isEntityDocked(stationRef) then return true end
        sleep(1)
    end
    return false
end

-- ─── main ───────────────────────────────────────────────────────────────────

assert(args[1] and args[2] and args[3],
    "Usage: cargo_route <shipName> <pickupSector> <deliverySector>")

local shipName      = args[1]
local pickupSec     = parseSector(args[2])
local deliverySec   = parseSector(args[3])

local self = console.getBlock():getEntity()
local ai   = self:getAI()
local fleet = self:getFleet()

assert(ai:isActive(), "AI must be active before running cargo_route.")

-- ─── Phase 1: travel to pickup sector ───────────────────────────────────────

print("Moving fleet to pickup sector " .. tostring(pickupSec))
if fleet then
    fleet:moveToSector(pickupSec)
else
    ai:moveToSector(pickupSec)
end

-- wait until we arrive
while not (self:getSector():equals(pickupSec)) do sleep(2) end
print("Arrived at pickup sector.")

-- ─── Phase 2: locate and approach the pickup station ────────────────────────

local pickup = findEntityByName("pickup_station")
assert(pickup, "Could not find 'pickup_station' in sector.")

local stationPos = pickup:getPos()
print("Approaching pickup station...")
ai:navigateToPos(stationPos, APPROACH_RADIUS)
while not ai:hasReachedPos(stationPos, APPROACH_RADIUS) do sleep(1) end
ai:stopNavigation()
print("In range of pickup station.")

-- ─── Phase 3: dock to RAIL_LOAD ──────────────────────────────────────────────

local docker = self:getBlockAt(DOCKER_POS)
assert(docker, "No block found at DOCKER_POS.")

print("Docking to load rail...")
self:dockToNearestLoadDock(pickup, docker)
if not awaitDocked(self, pickup, DOCK_TIMEOUT_SECS) then
    print("Dock timed out; aborting route.")
    return
end
print("Docked. Waiting for cargo transfer...")
sleep(5)  -- allow inventory transfer systems to fill cargo

-- ─── Phase 4: undock and travel to delivery sector ───────────────────────────

self:undockEntity(pickup)
print("Undocked from pickup. Moving to delivery sector " .. tostring(deliverySec))
if fleet then
    fleet:moveToSector(deliverySec)
else
    ai:moveToSector(deliverySec)
end

while not (self:getSector():equals(deliverySec)) do sleep(2) end
print("Arrived at delivery sector.")

-- ─── Phase 5: locate and approach the delivery station ───────────────────────

local delivery = findEntityByName("delivery_station")
assert(delivery, "Could not find 'delivery_station' in sector.")

local deliveryPos = delivery:getPos()
print("Approaching delivery station...")
ai:navigateToPos(deliveryPos, APPROACH_RADIUS)
while not ai:hasReachedPos(deliveryPos, APPROACH_RADIUS) do sleep(1) end
ai:stopNavigation()

-- ─── Phase 6: dock to RAIL_UNLOAD ────────────────────────────────────────────

print("Docking to unload rail...")
self:dockToNearestUnloadDock(delivery, docker)
if not awaitDocked(self, delivery, DOCK_TIMEOUT_SECS) then
    print("Dock timed out at delivery; aborting.")
    return
end
print("Docked. Waiting for cargo offload...")
sleep(5)

self:undockEntity(delivery)
print("Cargo route complete.")
