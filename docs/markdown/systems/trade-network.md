# Trade Network API

`trade` is a global that queries StarMade's galaxy-wide trade-node registry. No travel is required — the registry is server-side and hits the persistent DB for shops in unloaded sectors.

Only shops that have their **"Trade Node"** flag enabled appear. AI trading posts typically have this on; player shops default off. Server-side only; clients receive empty results.

## Typical usage

```lua
-- Find the cheapest place to buy Silicon Capsules.
local offers = trade.findBuyOffers(512)
if #offers > 0 then
    local best = offers[1]
    print("Cheapest:", best.getPrice(), "cr at", best.getNode().getStationName())
end

-- One-pass galactic market aggregate.
for _, entry in ipairs(trade.getMarketSnapshot()) do
    local info = entry.getInfo()
    if info ~= nil and info.isCapsule() then
        print(entry.getName(),
              "avg buy:", entry.getAvgBuy(),
              "avg sell:", entry.getAvgSell(),
              "total stock:", entry.getTotalStock())
    end
end
```

## Reference

### `trade`

- `getNodes()`
Returns `TradeNodeInfo[]` for every registered trade node in the galaxy.

- `getNodeCount()`
Returns total trade node count as an `Integer`. Cheaper than `#getNodes()` when you just want a size check.

- `findBuyOffers(typeId: Short)`
Returns `TradeOffer[]` where the player can **buy** this item type, sorted cheapest first.

- `findSellOffers(typeId: Short)`
Returns `TradeOffer[]` where the player can **sell** this item type, sorted highest-paying first.

- `findAllOffers(typeId: Short)`
Returns `TradeOffer[]` in both directions, unsorted.

- `getMarketSnapshot()`
One-pass aggregate across every node and every item type. Returns `MarketEntry[]`. Far cheaper than calling `findBuyOffers` / `findSellOffers` per type when you want broad market data.

## TradeNodeInfo

A lightweight, read-only snapshot of one trade node. Data reflects the network's most recent cache — not live.

- `getEntityDbId()`
The underlying station's persistent database ID as a `Long`.

- `getStationName()`
Display name of the station.

- `getFactionId()` / `getFaction()`
Owning faction.

- `getCredits()`
Node's credit reserves (DB-cached — may be slightly stale for unloaded sectors).

- `getSystem()` / `getSector()`
Galaxy-space coordinates as `LuaVec3i`.

- `getVolume()` / `getCapacity()`
Current cargo volume in use and total capacity, both as `Double`.

- `getTradePermission()`
Raw permission bitmask as a `Long`.

- `getOwners()`
`String[]` of player owner names.

## TradeOffer

One price quote from a trade node for one item type in one direction.

- `getNode()`
The `TradeNodeInfo` this offer came from.

- `getId()`
Item type ID as a `Short`.

- `getInfo()`
`BlockInfo` metadata for the item type.

- `getPrice()`
Quoted unit price as an `Integer`.

- `getAmount()`
Stock available at this price point.

- `getLimit()`
Per-transaction purchase cap.

- `isPlayerBuy()`
`true` when this is a price the player can buy *from* the shop, `false` when the shop is buying *from* the player.

## MarketEntry

Aggregate statistics for one item type across the entire trade network. Produced by `trade.getMarketSnapshot()`.

All counts and prices are summed/averaged across every node that lists the type.

- `getId()` / `getInfo()` / `getName()`
Item identity.

- `getBuyOfferCount()`
Number of nodes selling this item (i.e. offering it for the player to buy).

- `getSellOfferCount()`
Number of nodes buying this item (i.e. offering to purchase from the player).

- `getAvgBuy()` / `getAvgSell()`
Mean unit price the player would pay / receive across all applicable nodes. `nil` when no offer exists in that direction.

- `getMinBuy()` / `getMaxBuy()`
Cheapest and most expensive "player buy" prices across the network.

- `getMinSell()` / `getMaxSell()`
Lowest and highest "player sell" prices across the network.

- `getTotalStock()`
Sum of `amount` across every node selling this type — galactic-wide inventory count.

## Notes

- `getMarketSnapshot()` internally iterates every node once and assembles all aggregate stats in a single pass. Prefer it over repeated per-type queries.
- Unloaded sectors' prices come from the `TradeNodeTable` database table and may lag the live game state by up to one persistence cycle.
- When working with many types at once, treat this as a periodic sampling tool — polling every few seconds is cheap, polling every frame will hit DB throughput hard for large galaxies.
