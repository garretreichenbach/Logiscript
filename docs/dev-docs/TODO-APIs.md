# LuaMade - Planned API Additions

---

## 1. Combat & Weapons (High Priority)

The biggest missing system. Enables automated turrets, damage callbacks, and scripted combat.

### Weapon Control
- **Fire weapons** - `FireingUnit`, `CockpitManager` - fire cannons, beams, missiles by weapon group
- **Weapon targeting** - select targets, lock-on, aim direction
- **Weapon configuration** - query weapon stats (damage, range, fire rate)

### Damage Events
- **Incoming damage** - `DamageDealer`, `Damager`, `HitType` - callbacks when entity takes damage
- **Outgoing damage** - track hits dealt to other entities
- **Damage types** - `DamageDealerType`: CANNON, BEAM, MISSILE, EXPLOSION

### Missiles
- **Launch missiles** - `Missile`, `MissileTargetManager` - fire and track missiles
- **Missile types** - `DumbMissile`, `TargetChasingMissile`, `HeatMissile`, `BombMissile`, etc.

### Mines
- **Deploy mines** - `Mine`, `MineHandler` - place and manage mines
- **Mine types** - `CannonMineHandler`, `HeatSeekerMineHandler`

### Explosions
- **Explosion data** - `ExplosionData`, `ExplosionDataHandler` - react to or query explosions

---

## 2. Trading & Economy ✅ (implemented — see `docs/systems/shop.md`, `docs/systems/trade-network.md`)

### Trade System
- **Trade orders** ✅ - `shop.createOrder(targetDbId)` → `TradeOrderDraft` (addBuy/addSell, quote, submit); runs the game's own `checkTrade` / `executeTradeOrderServer` on the server thread. Own shop only.
- **Active trades** ✅ - `shop.getActiveTrades()`, `trade.getActiveTrades()` / `getActiveTradesFor(dbId)` → `ActiveTrade` (in-flight shipments from `TradeActiveMap`)
- **Trade nodes** ✅ - `trade.getNodes()`, `findBuyOffers` / `findSellOffers`, `getMarketSnapshot()`
- **Trade history** ⛔ skipped - vanilla never writes the `TRADE_HISTORY` table (`insertTradeHistory` has no callers), so it is always empty. Would need our own ledger.

### Shop Interaction ✅
- **Buy/sell** ✅ - `shop.buy` / `shop.sell` on behalf of a player
- **Shop options / inventory** ✅ - prices, buyable/sellable types, `getStock()`

---

## 3. Chat System (High Priority)

Enables chatbots, command systems, and alert notifications from Lua scripts.
TODO: Have config option to prevent broadcasting into public "all" chat to prevent spam.

### Chat Channels
- **Send messages** - `ChatChannel`, `ChannelRouter` - send chat messages
- **Receive messages** - listen for incoming chat on channels
- **Channel types** - `AllChannel`, faction chat, sector chat, etc.
- **Chat parsing** - `ChatMessageParseEvent` - react to chat commands

---

## 4. Docking & Rails (Medium Priority)

Control docked turrets, manage carrier operations, and rail-based automation.

### Docking
- **Dock/undock** - `DockingController` - dock and undock entities
- **Query docked entities** - list what's docked to the current entity
- **Turret control** - manage docked turrets

### Rails
- **Rail movement** - `RailController`, `RailRequest` - move entities along rails
- **Rail relations** - `RailRelation` - query rail connections

---

## 5. Sector & World Queries ✅

Know what's around you. Enables navigation aids, sector scanners, and map tools.

### Galaxy & Star Systems ✅ (implemented — `galaxy` global; see `docs/systems/galaxy.md`)
- **Galaxy data** ✅ - `GalaxyManager` / `StellarSystem` - `galaxy.getSystem()`, `getSystemAt()`, star-system names & positions, center/sector/planet types
- **System ownership** ✅ - `galaxy.getSystemOwner()` (faction) and `getSystemOwnership()` (relationship: BY_SELF/ALLY/ENEMY/NEUTRAL/NONE)
- **Warp gates** ✅ - `FTLConnection` - `galaxy.getWarpGates()` returns FTL routes (warp gate / wormhole / race-way) with sources & destinations

### Sector Information ✅ (implemented — `galaxy` global; see `docs/systems/galaxy.md`)
- **Sector contents** ✅ - `galaxy.getSectorInfo()` → `SectorInfo` (type + planet/station sub-type, loaded state, protection mode); `galaxy.getEntitiesInSector()` → loaded ships/stations/asteroids in a sector
- **Nearby-sector proximity scan** ✅ - `galaxy.scanSectors(center, radius)` → `SectorInfo[]` cube scan (server-side, reuses star-system lookups per system, no sector loading — same technique as `ClientProximitySector.updateServer`)

Demo: **`/bin/sectormap.lua`** — in-world 3D holographic sector map (galaxy scan + gfx3d projector).

---

## 6. Faction Management (Medium Priority)

Deeper faction control beyond just reading basic faction info.

### Roles & Permissions
- **Faction roles** - `RemoteFactionRoles` - query and manage roles
- **Build rights** - `FactionBuildRight` - query build permissions

### Territory
- **Claim territory** - `RemoteSystemOwnershipChange` - claim systems (reading ownership is covered by `galaxy.getSystemOwner()`)

### Members
- **Invitations** - `RemoteFactionInvitation` - invite/kick members
- **Faction news** - `RemoteFactionNewsPostBuffer` - post/read faction news