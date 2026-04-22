# Shop API

`Shop` wraps a StarMade shop (standalone shop station or a space station with shop blocks). You obtain one by calling `entity.asShop()` / `remoteEntity.asShop()` after confirming with `isShop()`.

All price fields use the **player's perspective**: `getBuyPrice` is what the player *pays*, `getSellPrice` is what the player *receives*.

## Typical usage

```lua
local entity = peripheral.getCurrentBlock().getEntity()

for _, remote in ipairs(entity.getNearbyEntities(3)) do
    if remote.isShop() then
        local shop = remote.asShop()
        print(remote.getName(), "credits:", shop.getCredits())
        for _, stack in ipairs(shop.getStock()) do
            print("  ", stack.getId(), stack.getCount(),
                  "buy:", stack.getBuyPrice(), "sell:", stack.getSellPrice())
        end
    end
end
```

## Reference

### Identity

- `isValid()`
Returns `true` if the underlying station is still a valid shop.

- `getEntity()`
Returns a `RemoteEntity` for the shop's station (useful for position/sector lookups).

- `getDbId()`
Returns the station's persistent database ID as a `Long`, or `nil` if unavailable.

### Wallet and flags

- `getCredits()`
Shop's credit reserves as a `Long`. For AI shops this may be very large; for player shops it's the balance available for purchasing from players.

- `isAiShop()`
Returns `true` for NPC-run shops.

- `isInfiniteSupply()`
Returns `true` when the shop's stock isn't depleted by purchases (typical for AI trading posts).

### Ownership and permissions

- `getFactionId()` / `getFaction()`
Owning faction ID and `Faction` wrapper. `0` means neutral/unaligned.

- `getOwners()`
Returns `String[]` of player owner names (empty for pure AI shops).

- `getPurchasePermission()` / `getTradePermission()`
Returns raw permission bitmasks as a `Long`. Useful for checking if the current player can interact — compare against StarMade's `TradePerm` values.

### Stock

- `getStock()`
Returns `ShopStockEntry[]` for every currently-populated inventory slot with a positive count.

- `getStockFor(typeId: Short)`
Returns a single `ShopStockEntry` aggregating all slots of that type. Count will be `0` if the shop doesn't carry it.

### Prices

- `getBuyPrice(typeId: Short)`
Price the player pays to buy one unit from this shop. Returns `nil` if the shop doesn't sell this type.

- `getSellPrice(typeId: Short)`
Price the player receives when selling one unit to this shop. Returns `nil` if the shop doesn't buy this type.

- `getBuyableTypes()`
Returns `Short[]` of every item type this shop has a "player buy" price set for.

- `getSellableTypes()`
Returns `Short[]` of every item type this shop has a "player sell" price set for.

### Transactions

Transactions execute **on behalf of a named player**. The player must be online on the server. Both methods are server-side only and return `true` only when the player's inventory actually changed.

- `buy(playerName: String, typeId: Short, quantity: Integer)`
Purchase `quantity` of the item from the shop, charging the player's credits. Validates stock, space, credits, and shop permissions. Returns `true` on success.

- `sell(playerName: String, typeId: Short, quantity: Integer)`
Sell `quantity` of the item to the shop, crediting the player. Validates the player's stock and the shop's credit reserves. Returns `true` on success.

Both methods send network updates so the player and any observers see the changed inventories immediately.

## ShopStockEntry

One row returned by `Shop.getStock()` / `Shop.getStockFor()`. All price fields are from the player's perspective.

- `getId()`
Item type ID as a `Short`.

- `getInfo()`
Returns `BlockInfo` metadata for the item type.

- `getCount()`
Current count in the shop's inventory.

- `getBuyPrice()` / `getSellPrice()`
Player buy / player sell prices, or `nil` if the shop doesn't trade in that direction.

- `getBuyLimit()` / `getSellLimit()`
Per-transaction caps, or `nil` when unset.

## Notes

- Only shops with their **Trade Node** flag enabled appear on the galaxy-wide network — see `trade-network.md` for wide queries. `entity.asShop()` works on any shop you can reach directly, trade node or not.
- Many AI trading posts have `isInfiniteSupply() == true`. Don't treat `getCount()` as a hard limit — check `isInfiniteSupply()` when reasoning about available stock.
- Prices on a live shop can shift with stock levels (supply/demand). Cache price values for the duration of a transaction, not for long-lived planning.
