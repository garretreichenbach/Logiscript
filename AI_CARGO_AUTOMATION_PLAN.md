# AI Cargo Automation Plan (LuaMade)

## Goal
Enable reliable Lua-driven cargo logistics between player bases by exposing safe, high-level ship AI controls for:

1. Sector travel (including jump-driven multi-sector movement)
2. Approximate world-space approach within a sector
3. Rail docking/undocking for load and unload workflows

---

## Key Findings from StarMade Source

### 1) Fleet command path is the safest high-level movement API
Relevant StarMade files:

- `org/schema/game/common/data/fleet/FleetCommandTypes.java`
- `org/schema/game/common/data/fleet/FleetManager.java`
- `org/schema/game/common/data/fleet/Fleet.java`
- `org/schema/game/common/data/fleet/missions/machines/states/FleetState.java`
- `org/schema/game/server/ai/program/common/TargetProgram.java`
- `org/schema/game/server/ai/program/fleetcontrollable/states/FleetMovingToSector.java`

Observed behavior:

- `Fleet.sendFleetCommand(FleetCommandTypes, Object...)` is the central public entry point.
- `FleetCommandTypes.MOVE_FLEET` takes a `Vector3i` sector target.
- `FleetManager.executeCommand(...)` applies transitions and move targets server-side.
- Loaded ship AI movement uses `TargetProgram.setSectorTarget(...)` + FSM transition `MOVE_TO_SECTOR`.

Conclusion:

- For long-range cargo routing, prefer fleet commands over ad-hoc per-tick steering.

### 2) World-space movement is direction-driven and approximate
Relevant files:

- `org/schema/game/server/ai/ShipAIEntity.java`
- `org/schema/game/server/ai/program/common/states/GettingToTarget.java`
- `org/schema/game/server/ai/AIShipControllerStateUnit.java`

Observed behavior:

- Local maneuvering is done by feeding movement direction vectors.
- Exact braking/arrival precision is not guaranteed by stock AI.

Conclusion:

- Use staged approach radii and stop thresholds in wrapper logic.

### 3) Rail docking APIs are available and already used by game code
Relevant files:

- `org/schema/game/common/controller/rails/RailController.java`
- `org/schema/game/common/controller/elements/rail/pickup/RailPickupUnit.java`
- `org/schema/game/common/controller/elements/commandmodule/parser/RailCommandParser.java`

Observed behavior:

- Docking path: `railController.connectServer(dockerPiece, targetRailPiece)`
- Undocking path: `disconnect()` on child rails and `undockAll...` variants
- Rail command parser already manipulates docked AI state and rail behaviors

Conclusion:

- Lua wrapper can safely expose higher-level docking workflows using existing server rail calls.

---

## Current LuaMade Surface (What Already Exists)

Relevant mod files:

- `src/main/java/luamade/lua/entity/ai/Fleet.java`
- `src/main/java/luamade/lua/entity/ai/EntityAI.java`
- `src/main/java/luamade/lua/entity/Entity.java`

Already present:

- `Fleet.setCurrentCommand(String, Object...)`
- `EntityAI.moveToSector(Vec3i)`
- `EntityAI.moveToPos(Vec3i)`
- `Entity.dockTo(...)`, `Entity.undockEntity(...)`, `Entity.undockAll()`

Gap:

- API is currently low-level/stringly in places and lacks explicit cargo-mission primitives.

---

## Proposed API Additions

### Fleet wrapper (`Fleet`)

- `moveToSector(Vec3i sector)`
- `patrolSectors(Vec3i[] sectors)`
- `attackSector(Vec3i sector)`
- `defendSector(Vec3i sector)`
- `setCommand(FleetCommandType type, ...)` (typed enum wrapper, optional)

### Entity AI wrapper (`EntityAI`)

- `moveToSector(Vec3i sector)` (ensure transition firing when needed)
- `navigateToPos(Vec3i pos, Integer stopRadius)`
- `hasReachedPos(Vec3i pos, Integer radius)`
- `stopNavigation()`

### Entity wrapper (`Entity`)

- `dockToNearestLoadDock(RemoteEntity station, Block docker)`
- `dockToNearestUnloadDock(RemoteEntity station, Block docker)`
- `findRailBlocksByType(...)` (optional utility)

---

## Phased Implementation Plan

## Phase 1 - Typed movement primitives

- Add explicit `Fleet` movement methods backed by `FleetCommandTypes`.
- Harden `EntityAI.moveToSector` to ensure server-safe transitions.
- Add robust `navigateToPos` with stop radius.

Deliverable:

- Scripts can issue clean sector travel and local approach commands without string commands.

## Phase 2 - Docking workflow helpers

- Build convenience methods around existing rail connect/disconnect calls.
- Keep faction/same-sector/root-rail safety checks strict.
- Support both nearest valid rail and explicit target rail block.

Deliverable:

- Scripts can do predictable dock/undock for cargo handoff points.

## Phase 3 - Cargo mission orchestration

Introduce mission-state abstraction (wrapper class or method set):

- `beginRoute(pickupSector, pickupPos, dropoffSector, dropoffPos, options)`
- States:
  - `MOVE_TO_PICKUP_SECTOR`
  - `APPROACH_PICKUP`
  - `DOCK_PICKUP`
  - `LOAD`
  - `MOVE_TO_DROPOFF_SECTOR`
  - `APPROACH_DROPOFF`
  - `DOCK_DROPOFF`
  - `UNLOAD`
  - `RETURN_OR_IDLE`

Expose:

- `getMissionState()`
- `getLastError()`
- `isBusy()`
- `abortMission()`

Deliverable:

- Turn-key cargo automation loop usable from Lua scripts.

## Phase 4 - Reliability and safeguards

- Retry logic for missed docking or blocked rails
- Timeout + fallback behavior for stuck movement
- Optional blacklist/cooldown per failed dock target
- Telemetry hooks for debugging (state transitions and timing)

Deliverable:

- Production-stable logistics automation for release users.

---

## Validation Strategy

### Core test cases

1. Sector travel:
   - ship starts in sector A
   - command to sector B
   - verify arrival and state transition

2. In-sector approach:
   - command position near station rail
   - verify stop within threshold radius

3. Dock/load/undock/unload:
   - dock to load rail
   - transfer cargo
   - undock
   - dock to unload rail
   - transfer cargo

4. Failure handling:
   - blocked dock
   - target out of sector
   - faction permission mismatch

### Metrics to record

- Time-to-arrive sector
- Overshoot distance at local approach
- Dock retries before success
- Mission completion/failure rate

---

## Suggested Near-Term Milestone

Milestone M1 (release-friendly):

- Complete Phase 1 + minimal Phase 2
- Publish one example script: `examples/CargoRouteBasic.lua`
- Document API in:
  - `docs/markdown/entities/` (Fleet/EntityAI)
  - `docs/markdown/systems/` (rail docking workflow)

This gives players immediate value while keeping complexity manageable before full mission orchestration.

---

## Useful Grep Commands

```bash
cd "/home/garret/Documents/Projects/StarMade-Release"
grep -RIn --include='*.java' -e 'FleetCommandTypes' -e 'FleetCommand' -e 'ShipAIEntity' -e 'AIControllerStateUnit' src/main/java | head -220
```

```bash
cd "/home/garret/Documents/Projects/StarMade-Release"
grep -RIn --include='*.java' -e 'setSectorTarget' -e 'targetPosition' -e 'moveTo(' src/main/java/org/schema/game/server/ai src/main/java/org/schema/game/common/data/fleet | head -240
```

```bash
cd "/home/garret/Documents/Projects/StarMade-Release"
grep -RIn --include='*.java' -e 'railController.connectServer' -e 'railController.disconnect' -e 'undockAllServer' src/main/java | head -220
```

