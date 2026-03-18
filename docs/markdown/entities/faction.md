# Faction API

`Faction` exposes diplomacy helpers and faction identification.

## Reference

### Identification
- `getFactionId()` - Returns the numeric faction ID
- `getName()` - Returns the faction's display name
- `isNPCFaction()` - Returns true if this is an NPC faction

### Diplomacy
- `isSameFaction(otherFaction)` - Check if two factions are the same
- `isFriend(otherFaction)` - Check if friendly relation exists
- `getFriends()` - Get array of friendly factions
- `isEnemy(otherFaction)` - Check if hostile relation exists
- `getEnemies()` - Get array of enemy factions
- `isNeutral(otherFaction)` - Check if neutral relation exists

## Examples

```lua
-- Get player's faction
playerShip = console.getEntity()
faction = playerShip:getFaction()

-- Check faction properties
print("Faction: " .. faction:getName())
print("Faction ID: " .. faction:getFactionId())
print("Is NPC: " .. tostring(faction:isNPCFaction()))

-- Check diplomatic relations
if faction:isFriend(otherFaction) then
    print("We are friends!")
end

-- Iterate enemies
enemies = faction:getEnemies()
for i, enemy in ipairs(enemies) do
    print("Enemy: " .. enemy:getName())
end
```
