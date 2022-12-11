--- Simple enemy detector for LuaMade.
--- Author: TheDerpGamer

local entity = console:getBlock():getEntity()
local faction = entity:getFaction()

function sleep(n)
    local t0 = console:getTime()
    while console:getTime() - t0 <= n do end
end

function detect()
    local enemyMass = 0
    local enemyDetected = false
    local nearbyEntities = entity:getNearbyEntities()
    if(nearbyEntities ~= nil and nearbyEntities[1] ~= nil) then
        for i = 1, #nearbyEntities do
            local nearbyEntity = nearbyEntities[i]
            if(nearbyEntity ~= nil) then
                local otherFaction = nearbyEntity:getFaction()
                if(otherFaction ~= nil and otherFaction ~= faction and faction:isEnemy(otherFaction)) then
                    enemyMass = enemyMass + nearbyEntity:getMass()
                    enemyDetected = true
                end
            end
        end
    end
    if(not enemyDetected) then
        console:print("No enemies detected")
    else
        console:print("Enemies detected with a total mass of " .. enemyMass)
    end
end

while(true) do
    detect()
    sleep(10)
end