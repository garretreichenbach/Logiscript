# Logiscript - Planned API Additions

Identified from the StarMade codebase at `org.schema.game`. Organized by priority.

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

### Chat Channels
- **Send messages** - `ChatChannel`, `ChannelRouter` - send chat messages
- **Receive messages** - listen for incoming chat on channels
- **Channel types** - `AllChannel`, faction chat, sector chat, etc.
- **Chat parsing** - `ChatMessageParseEvent` - react to chat commands

---

## 4. Status Effects (Medium Priority)

Read and react to active effects on entities. Useful for defensive automation.

### Block Effects
- **Query effects** - `BlockEffect`, `BlockEffectTypes`, `BlockEffectManager` - 25+ effect types
- **Armor effects** - `StatusArmorHardenEffect`, `StatusArmorHpAbsorptionBonusEffect`
- **Shield effects** - `StatusShieldHardenEffect`, `StatusPowerShieldEffect`
- **Movement effects** - `PullEffect`, `PushEffect`, `StopEffect`, `ThrusterOutageEffect`
- **Other effects** - `EvadeEffect`, `PowerRegenDownEffect`, `ShieldRegenDownEffect`, `StatusAntiGravityEffect`, `StatusTopSpeedEffect`, `StatusPiercingProtectionEffect`

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
- **Lifts** - `LiftContainerInterface`, `NetworkLiftInterface` - control lifts

---

## 6. Sector & World Queries (Medium Priority)

Know what's around you. Enables navigation aids, sector scanners, and map tools.

### Sector Information
- **Sector contents** - `SectorInformation` - query entities, type, and ownership of sectors
- **Nearby sectors** - `ClientProximitySector`, `ClientProximitySystem` - scan nearby space
- **Sector generation** - `SectorGenerationDefault` - query sector type (void, asteroid, nebula, etc.)

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

---

## 8. Blueprint System (Lower Priority)

Query and work with ship/station blueprints.

### Blueprints
- **Ship blueprints** - `ShipOutline` - query saved ship designs
- **Station blueprints** - `SpaceStationOutline` - query station designs
- **Blueprint spawning** - `BluePrintSpawnQueueElement` - spawn from blueprint (admin/creative)
- **Blueprint I/O** - `BluePrintReadQueueElement`, `BluePrintWriteQueueElement` - read/write blueprint files

---

## 9. Player & Server Info (Lower Priority)

### Player Stats
- **Player data** - `RequestPlayerStats` - query player stats, credits, playtime

### Server Stats
- **Server info** - `RequestServerStats` - query server performance, player count, uptime

### Game Rules
- **Rule queries** - `RuleContainer`, `RuleSet` - query active game configuration and rules

---

## 10. NPC Simulation & Diplomacy (Lower Priority)

Query and interact with NPC faction behavior.

### NPC Factions
- **Faction state** - `NPCFaction`, `NPCFactionManager` - query NPC faction status
- **Faction FSM** - `NPCFactionFSM` - query current NPC faction behavior state

### Diplomacy
- **Relations** - `NPCDiplomacy`, `DiplomacyConfig` - query faction relations (war/peace/allied)
- **NPC news** - `NPCFactionNews` - 9 event types (war declared, territory lost, etc.)

### NPC Trade
- **Trade routes** - `NPCTradeController`, `NPCTradeNode` - query NPC trade networks

---

## 11. Pathfinding (Lower Priority)

### Character Pathfinding
- **Path calculation** - `SegmentPathFindingHandler` - calculate paths within entities
- **Ground pathfinding** - `SegmentPathGroundFindingHandler` - gravity-aware pathing