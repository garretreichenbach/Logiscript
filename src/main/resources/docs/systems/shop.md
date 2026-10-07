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

Transactions execute **on behalf of the player who ran the script** — `playerName` must be that player (e.g. `player.getName()`); any other name, or a script with no invoking player (such as the startup script), raises an error. Both methods are server-side only, run on the server thread (the script waits briefly), and return `true` only when the player's inventory actually changed.

- `buy(playerName: String, typeId: Short, quantity: Integer)`
Purchase `quantity` of the item from the shop, charging the player's credits. Validates stock, space, credits, and shop permissions. Returns `true` on success.

- `sell(playerName: String, typeId: Short, quantity: Integer)`
Sell `quantity` of the item to the shop, crediting the player. Validates the player's stock and the shop's credit reserves. Returns `true` on success.

Both methods send network updates so the player and any observers see the changed inventories immediately.

### Trade orders (Trading Guild)

Ship goods between this shop and any other trade node, like the in-game trade dialog. The shop pays from its own credits; goods arrive by Trading Guild cargo fleet after a delay. Only the computer's **own** station can place orders — `createOrder` on a shop obtained from a `RemoteEntity` raises an error.

- `createOrder(targetDbId: Long)`
Returns a `TradeOrderDraft` to the trade node with that database ID (see `trade.getNodes()` / `TradeOffer.getNode().getEntityDbId()`).

- `getActiveTrades()`
Returns `ActiveTrade[]` for shipments currently in flight to or from this shop.

```lua
local shop = peripheral.getCurrentBlock().getEntity().asShop()
local best = trade.findBuyOffers(512)[1]

local order = shop.createOrder(best.getNode().getEntityDbId())
order.addBuy(512, 100)

if order.quote() then
    print("Total:", order.getTotalPrice(), "cr (delivery", order.getDeliveryCost(), ")")
    if order.submit() then print("Order sent") else print("Failed:", order.getError()) end
else
    print("Rejected:", order.getError())
end
```

## TradeOrderDraft

Built with `addBuy` / `addSell`, then checked with `quote()` or sent with `submit()`. Both run the game's own trade checks on the server thread (credits, cargo capacity, stock, limits, pending deliveries) and block the script briefly.

- `addBuy(typeId: Short, amount: Integer)` / `addSell(typeId: Short, amount: Integer)`
Add or replace a line. Buy pulls goods from the target node; sell sends goods from this shop's stock to it. Amount `0` removes the line. Both return the draft, so calls can be chained.

- `clear()`
Removes all lines.

- `quote()`
Prices the order and validates it without sending. Returns `true` if it would be accepted.

- `submit()`
Sends the order. Returns `true` on success; credits are deducted immediately and the target's stock is reserved.

- `getError()`
Reason the last `quote()` / `submit()` failed, or `nil`.

- `getBuyPrice()` / `getSellPrice()` / `getDeliveryCost()` / `getTotalPrice()`
Credit amounts from the last `quote()` / `submit()`. Total is buy price minus sell price plus delivery cost.

- `getBuyVolume()` / `getSellVolume()` / `getDistance()` / `getCargoShips()`
Cargo volume each way, distance in star systems, and number of guild cargo ships used.

- `getTargetDbId()`
The target trade node's database ID.

## ActiveTrade

A Trading Guild shipment in flight. Returned by `shop.getActiveTrades()` and `trade.getActiveTrades()`.

- `getFromDbId()` / `getToDbId()` / `getFromStation()` / `getToStation()`
Source and destination nodes.

- `getFromFactionId()` / `getToFactionId()`
Owning factions of each end.

- `getItems()`
Cargo as `ItemStack[]`.

- `getBlockPrice()` / `getDeliveryPrice()` / `getVolume()`
Value of the goods, guild fee, and cargo volume.

- `getStartTime()` / `getEstimatedDuration()`
Departure time and expected travel time, in milliseconds.

- `getStartSystem()` / `getTargetSystem()` / `getCurrentSector()`
Route positions as `Vec3i`.

- `isCargoLoaded()`
`true` once the cargo has been picked up.

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
