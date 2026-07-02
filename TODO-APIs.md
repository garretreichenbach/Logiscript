# Logiscript - Planned API Additions

Identified from the StarMade codebase at `/Users/garret/Documents/GitHub/StarMade`. Organized by priority.

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

## 2. Trading & Economy (High Priority)

Enables automated trading bots, market monitors, and price comparison scripts.

### Trade System
- **Trade orders** - `TradeManager`, `TradeOrder`, `TradeOrderConfig` - create and manage trade orders
- **Trade nodes** - `TradeNode`, `TradeNodeClient` - query available trade routes
- **Trade history** - `TradeHistoryElement` - query past trades

### Shop Interaction
- **Buy/sell** - `ShopInterface`, `ShopNetworkInterface` - interact with shops programmatically
- **Shop options** - `ShopOption` - query available items and prices
- **Shop inventory** - `ShopInventory` - query shop stock

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

## 5. Docking & Rails (Medium Priority)

Control docked turrets, manage carrier operations, and rail-based automation.

### Docking
- **Dock/undock** - `DockingController` - dock and undock entities
- **Query docked entities** - list what's docked to the current entity
- **Turret control** - manage docked turrets

### Rails
- **Rail movement** - `RailController`, `RailRequest` - move entities along rails
- **Rail relations** - `RailRelation` - query rail connections

---

## 6. Sector & World Queries (Medium Priority)

Know what's around you. Enables navigation aids, sector scanners, and map tools.

### Sector Information
- **Sector contents** - `SectorInformation` - query entities, type, and ownership of sectors
- **Nearby sectors** - `ClientProximitySector`, `ClientProximitySystem` - scan nearby space
- **Sector generation** - `SectorGenerationDefault` - query sector type (void, asteroid, etc.)

### Galaxy & Star Systems
- **Galaxy data** - `GalaxyManager` - star system positions, names
- **System ownership** - query which faction owns a system
- **Warp gates** - `FTLConnection` - query jump drive routes and destinations

---

## 7. Faction Management (Medium Priority)

Deeper faction control beyond just reading basic faction info.

### Roles & Permissions
- **Faction roles** - `RemoteFactionRoles` - query and manage roles
- **Build rights** - `FactionBuildRight` - query build permissions

### Territory
- **System ownership** - `RemoteSystemOwnershipChange` - query/claim territory

### Members
- **Invitations** - `RemoteFactionInvitation` - invite/kick members
- **Faction news** - `RemoteFactionNewsPostBuffer` - post/read faction news