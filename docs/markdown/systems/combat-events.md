# Combat Events API

`combat` is a global available in all scripts that provides a queue of combat events for the entity the computer is on. Events are pushed automatically when the entity takes block damage or shield hits.

## Typical usage

```lua
-- Simple damage monitor
while true do
    local e = combat.waitFor(1000)
    if e then
        if e.type == "block_damage" then
            print("Hit! " .. e.damageType .. " damage: " .. e.damage)
            if e.attackerName then
                print("  from: " .. e.attackerName)
            end
        elseif e.type == "shield_hit" then
            print("Shield hit! Type: " .. e.damageType)
        end
    end
end
```

```lua
-- Alert on high damage
while true do
    local e = combat.waitFor(500)
    if e and e.type == "block_damage" and e.damage > 100 then
        print("WARNING: Heavy fire from " .. (e.attackerName or "unknown"))
    end
end
```

## Queue methods

- `poll()`
Returns the next event table, or `nil` if the queue is empty. Non-blocking.

- `waitFor(timeoutMs: Integer)`
Blocks up to `timeoutMs` milliseconds for an event. Returns the event table, or `nil` on timeout.

- `clear()`
Discards all pending events.

- `isEnabled()`
Returns `true` when event capture is active.

- `setEnabled(enabled: Boolean)`
Enable or disable event capture. Disabling also clears the queue.

- `getPendingCount()`
Returns the number of events waiting in the queue.

## Event types

### block_damage

Fired when a block on the entity takes damage.

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Always `"block_damage"` |
| `damageType` | `string` | `"PROJECTILE"`, `"BEAM"`, `"MISSILE"`, `"PULSE"`, `"EXPLOSIVE"`, or `"GENERAL"` |
| `damage` | `number` | Damage amount dealt to the block |
| `blockType` | `number` | Element type ID of the damaged block |
| `attackerName` | `string` | Name of the attacking entity (may be absent) |
| `attackerFaction` | `number` | Faction ID of the attacker (may be absent) |
| `isServer` | `boolean` | `true` when the event fired on the server |

### shield_hit

Fired when the entity's shields are struck.

| Field | Type | Description |
|-------|------|-------------|
| `type` | `string` | Always `"shield_hit"` |
| `damageType` | `string` | `"PROJECTILE"`, `"BEAM"`, `"MISSILE"`, `"PULSE"`, `"EXPLOSIVE"`, or `"GENERAL"` |
| `isHighDamage` | `boolean` | `true` for high-damage hits |
| `isLowDamage` | `boolean` | `true` for low-damage hits |
| `isServer` | `boolean` | `true` when the event fired on the server |

## Notes

- The event queue holds up to 256 events. Older events are dropped if a script doesn't poll fast enough.
- Events are dispatched to all computers on the entity, not just the one closest to the impact.
- Use `combat.setEnabled(false)` on computers that don't need combat events to avoid unnecessary overhead.
