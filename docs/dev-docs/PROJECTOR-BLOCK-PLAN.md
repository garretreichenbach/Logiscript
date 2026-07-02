# Projector Block — Lua-Driven World-Space 2D/3D Graphics

Status: **not started**. Design only. Written up after the [server-side script execution migration](#note-simplification-unlocked-by-the-server-side-migration) landed, which changes and simplifies part of this design (see note at the bottom) — read that note before implementing.

## Context

Logiscript computers can already draw 2D graphics via `gfx2d`, but it renders **screen-space** primitives onto the terminal UI of whichever player is viewing that computer's dialog — it is not in the world. There is no way to draw graphics *in the world* (holographic maps, waypoints, ship schematics visible to everyone standing near the ship).

This adds a new **Projector** block that a computer's Lua script drives (via the peripheral system) to render 2D and 3D vector graphics anchored in world space near the projector, at a user-defined offset clamped to ±5 blocks per axis.

Requirements gathered from the user:
- **Visibility:** shared — the projection is server-authoritative and rendered identically for **every player near the entity**, including players who never opened the computer.
- **2D projection:** **vector primitives on an oriented plane** (a resolution-independent 2D "surface" floating in 3D), not a raster bitmap.
- **3D primitives:** basic vector set — points, lines, wireframe/filled boxes, polylines, triangles. No textured meshes in v1.

The design mirrors existing, proven patterns in this codebase rather than inventing new structure: the **Vault** system (block → `SystemModule` container → authoritative state → sync to nearby clients) for networking/state, and **`Gfx2d`** (thread-safe, layered, batched, snapshot-based draw-command buffer) for the drawing API.

## Why this shape (verified against the code and the real StarMade source)

- World rendering hook: `api.utils.draw.ModWorldDrawer` registered via `api.listener.events.draw.RegisterWorldDrawersEvent` (fired once by `org.schema.game.client.view.WorldDrawer`). `postWorldDraw()` is the per-frame world-space draw hook. Per-block world transform via `SegmentPiece.getTransform(...)`, applied with `GlUtil.glPushMatrix()/glMultMatrix()/translateModelview()/glPopMatrix()`. (No usable per-segment-piece draw event exists in the base game — `SegmentDrawListener` is defined but never invoked — so the projector must enumerate active projectors itself.)
- Enumerating all live projectors on the client each frame: reuse the static `ACTIVE_CONTAINERS` set pattern already used by `ComputerModuleContainer` (see `src/main/java/luamade/system/module/ComputerModuleContainer.java:28`).
- New block registration is a one-line enum add in `src/main/java/luamade/element/ElementRegistry.java` (`PROJECTOR(new Projector())`), matching `VAULT`.
- `SystemModule` gives server→nearby-client sync for free: `flagUpdatedData()` → `syncToNearbyClients()` (uses StarMade's own `ModParticleUtil.getPlayersInRange(sector)`), with `onTagSerialize`/`onTagDeserialize` as the wire+save format. Confirmed in `VaultModuleContainer.java`.

## Data-flow overview (original design — see simplification note at the end)

```
Lua script (computer, driven via peripheral)         Server (authoritative)          Every nearby client
──────────────────────────────                       ──────────────────────          ───────────────────
proj = peripherals.wrap(block,"projector")
frame = proj.newFrame()
frame.line3d(...) / frame.boxWire(...)
surf = frame.newSurface(offset,normal,up,...)
surf.rect(...) surf.text(...)  -- 2D on plane
proj.publish(frame) ──PacketCSProjectorPublish──▶  validate (type, ±5 clamp,
   (blocks on CompletableFuture, 10s)               caps) → ProjectorModule
                                                     Container.publishFrame()
                                                        │  flagUpdatedData()
                          ◀─PacketSCProjectorPublishResult (ack to publisher only)
                                                        │  syncToNearbyClients()
                                                        └──PacketSCSyncMCModule──▶ onTagDeserialize()
                                                           (StarMade built-in)      → frame stored in
                                                                                     ProjectorModuleContainer
                                                                                          │
                                            ProjectorWorldDrawer.postWorldDraw() each frame:
                                            iterate ACTIVE_CONTAINERS → live SegmentPiece transform
                                            → GL11 draw 3D cmds + surface planes in world space
```

Rendering costs **no per-frame network traffic** — only `publish()` calls hit the network, same profile as a Vault balance change.

## New Java files

**Block + system layer**
- `element/block/Projector.java` — `extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener` (no interact dialog in v1). Mirrors `Vault.java`: `initData()` (description, `setOrientatable(true)`, `setCanActivate(false)`), `postInitData()` (recipe), `initResources()` (**reuse an existing model/texture for v1** — e.g. Computer's LOD via `BlockConfig.assignLod(..., "Computer", null)` or a decorative texture like Vault uses; bespoke model is a follow-up). On remove/killed → `ProjectorModuleContainer.getContainer(...).removeBlock(absIndex)`.
- `system/module/ProjectorModuleContainer.java` — `extends SystemModule`, keyed on block `absIndex`. Holds `Long2ObjectOpenHashMap<ProjectorFrame> frames`. Static `ACTIVE_CONTAINERS` set (add in ctor) so the world drawer can enumerate live projectors. Methods: `publishFrame(abs, frame)` / `clearFrame(abs)` (both call `flagUpdatedData()`), `getFrame(abs)`, `removeBlock(abs)`, `getFrames()`. `onTagSerialize`/`onTagDeserialize` a versioned format serializing `abs → ProjectorFrame` (doubles as sync payload **and** world-save persistence). `handle(Timer)`: optional stale-frame TTL sweep; no-op is fine for v1.
- `system/module/ProjectorFrame.java` — serializable value type: `revision`/`publishedAtMs`, a list of `Command3d` (3D primitives), and a list of `Surface` (each = oriented plane + its own list of `Command2d`). `writeTo(PacketWriteBuffer)`/`readFrom(PacketReadBuffer)` used by both the sync tag path and the publish packet. Server-side `validate/clamp` applied on read.

**Lua-facing drawing API** (mirrors `Gfx2d` structure: `LuaMadeUserdata`, lock-guarded, layered, `beginBatch`/`commitBatch`, `revision`, immutable command types, `snapshot()`; **coordinates are block-space floats relative to the projector**, clamped to ±5)
- `lua/gfx/Gfx3d.java` — the frame builder. Layer methods reused in spirit from `Gfx2d` (`setLayer`/`createLayer`/`removeLayer`/`getLayers`/`setLayerVisible`/`clearLayer`/`clear`/`beginBatch`/`commitBatch`). 3D primitives, following `gfx2d` conventions (position args first, then `r,g,b,a` as normalized 0–1 doubles, then shape args, trailing optional `thickness`/`filled` with overloads):
  - `point3d(ox,oy,oz, r,g,b,a)`
  - `line3d(ox1,oy1,oz1, ox2,oy2,oz2, r,g,b,a[, thickness])`
  - `boxWire(ox,oy,oz, w,h,d, r,g,b,a[, thickness])`
  - `boxFilled(ox,oy,oz, w,h,d, r,g,b,a)`
  - `polyline3d(points[/*flattened xyz*/], r,g,b,a[, thickness, closed])`
  - `triangle3d(ox1..oz3, r,g,b,a[, filled])`
  - `newSurface(ox,oy,oz, nx,ny,nz, ux,uy,uz, worldW, worldH[, canvasW, canvasH])` → returns a `ProjectorSurface`; attaches it to the active layer. Public (non-`@LuaMadeCallable`) `snapshot()` returns an immutable frame the wrapper serializes. No network code here (kept clean like `Gfx2d`).
- `lua/gfx/ProjectorSurface.java` — a `LuaMadeUserdata` "2D vector canvas floating in 3D." Defines an oriented plane (offset origin in block-space, `normal` + `up` vectors, world width/height, logical canvas width/height) and exposes the familiar **2D** primitives in canvas coordinates, matching `gfx2d` signatures: `point(x,y,r,g,b,a)`, `line(x1,y1,x2,y2,r,g,b,a[,thickness])`, `rect(x,y,w,h,r,g,b,a,filled)`, `circle(x,y,radius,r,g,b,a,filled[,segments,thickness])`, `polygon(points[],r,g,b,a,filled[,thickness])`, `text(x,y,str,r,g,b,a,scale[,maxWidth,maxHeight,align,wrap])`. Stores a list of `Command2d`; at render time each is mapped canvas→(u,v)→world via `origin + u*right + v*up` (`right = normal × up`).
- `lua/element/block/ProjectorBlock.java` — the peripheral wrapper (package matches `DisplayModule.java`). `extends Block`. `@LuaMadeCallable`: `newFrame()` → fresh `Gfx3d`; `publish(Gfx3d frame)` → build `ProjectorFrame` from `frame.snapshot()` and hand off to the server (see networking section — simplified under the new execution model); `clear()` → publish an empty frame; `getMaxOffset()` → effective ±clamp from config.
- `lua/projector/ProjectorScriptRequests.java` — only needed if `publish()` remains a blocking round-trip (see simplification note); mirrors `VaultScriptRequests`.

**Networking (original design, pre-migration assumption)**
- `network/PacketCSProjectorPublish.java` — client→server. Payload: `requestId(int)`, `entityId(int)`, `absIndex(long)`, serialized `ProjectorFrame`. `processPacketOnServer`: resolve `SegmentPiece`, verify `type == PROJECTOR`, **re-clamp offsets ±5 and enforce command/vertex caps server-side (authoritative)**, `ProjectorModuleContainer.publishFrame(...)`, then send `PacketSCProjectorPublishResult` to sender only.
- `network/PacketSCProjectorPublishResult.java` — server→client ack.

**Rendering**
- `gui/ProjectorWorldDrawer.java` — `extends ModWorldDrawer`. `postWorldDraw()`: iterate `ProjectorModuleContainer.ACTIVE_CONTAINERS` → each non-empty frame → resolve live `SegmentPiece` (`segmentController.getSegmentBuffer().getPointUnsave(absIndex)`, skip if null/wrong type) → `piece.getTransform(t)` → `GlUtil.glPushMatrix()/glMultMatrix(t)`, then per command translate by its ±5 offset and issue immediate-mode `GL11` calls (same style as `TerminalGfxOverlay.drawLayer`: `GL_LINES` for lines/wire edges, `GL_QUADS`/`GL_TRIANGLES` for filled, `GL_LINE_STRIP/LOOP` for polylines; surfaces = compute the 4 plane corners and draw their 2D commands as world-space geometry). **Respect depth testing** (holograms z-sort against world geometry) — a deliberate difference from the terminal GUI overlay, which disables it. Enable blend for alpha. Guard degenerate surface vectors (normal ∥ up).

## Existing files to modify

- `element/ElementRegistry.java` — add `PROJECTOR(new Projector()),` after `VAULT`. Generic listener wiring handles the rest.
- `manager/EventManager.java` — in the existing `ManagerContainerRegisterEvent` listener, add `event.addModMCModule(new ProjectorModuleContainer(event.getSegmentController(), event.getContainer()));`. Add a new `StarLoader.registerListener(RegisterWorldDrawersEvent.class, ...)` that does `event.getModDrawables().add(new ProjectorWorldDrawer());`.
- `LuaMade.java` — register the new packets in `registerPackets()`.
- `lua/peripheral/PeripheralRegistry.java` — add a `PeripheralProvider` in `registerDefaults()`: type names `{"projector"}`, `canWrap` checks `PROJECTOR.getId()`, `wrap` returns `new ProjectorBlock(piece, module)`.
- `manager/ConfigManager.java` — add caps following the `gfxMaxLayers`/`gfxMaxCommandsPerLayer` pattern: `projector_max_commands_per_frame` (~2048), `projector_max_offset_blocks` (default & hard-capped at 5.0 — config may only lower it), `projector_max_surfaces_per_frame` / per-surface 2D command cap.
- `docs/markdown/graphics/INDEX.md` — add a `gfx3d.md` bullet; note in "Notes" that `gfx3d` is world-space and server-synced/multi-viewer (unlike `gfx2d`).

## Docs

New `docs/markdown/graphics/gfx3d.md`, structured like `gfx2d.md`: intro; coordinate space (block units relative to projector, ±5 clamp, offsets rotate with the ship); "Obtaining a projector" (`peripherals.wrap(block,"projector")`, `newFrame()`); layers (reused from gfx2d); 3D primitives; "2D surfaces in 3D" (the in-world-map section — `newSurface(...)` + the familiar 2D primitive calls); publishing (`publish()` semantics, why bystanders see it); limits.

Example the doc will lead with:
```lua
local proj  = peripherals.wrap(peripherals.getRelative("front"), "projector")
local frame = proj.newFrame()

frame.setLayer("grid")
for i = -5, 5 do frame.line3d(i,0,-5, i,0,5, 0.3,0.3,0.3,1.0) end

frame.setLayer("map")
local surf = frame.newSurface(0,3,0,  0,1,0,  0,0,1,  4.0,4.0,  128,128) -- 4x4-block plane, 3 above, 128px canvas
surf.rect(0,0,128,128, 0.02,0.05,0.1,0.8, true)
surf.circle(64,64,40, 0.2,0.9,0.5,1.0, false, 32)
surf.text(6,6,"SECTOR MAP", 1,1,1,1, 1)

proj.publish(frame)   -- now visible to every player near the ship
```

## Defaults chosen (not asked, sensible per existing conventions — flag if you disagree)

- **Access:** open — anyone who can reach the entity's network can publish (no Permission-Module gating in v1; can add later like Vault).
- **Persistence:** frames persist across restart, reusing the same tag-serialize path that drives live sync (free; matches Vault/Computer). A script can also just republish on boot.
- **Out-of-range offsets:** clamped, not rejected (matches `gfx2d`'s clamp-don't-reject philosophy); server re-clamps authoritatively.
- **Model:** reuse an existing block model/texture for v1; bespoke Projector mesh is a follow-up.

## Verification

**Checkable without a running game:** pattern/consistency review against `Vault.java` / `VaultModuleContainer.java` / `Gfx2d.java`; packet read/write symmetry and no-arg constructors; confirm the ±5 clamp is enforced both client-side (UX) and server-side (authoritative); `ConfigManager` key-name uniqueness; `./gradlew compileJava` against the real `StarMade.jar` (available locally — see `gradle.properties`' `starmade_root`).

**Needs manual in-game testing:**
- World-transform correctness: rotate the ship, confirm the projection + offset rotate with it (not world-axis-locked).
- **Multi-client visibility** (the core feature): two players near one entity, only one runs the script, both see the identical synced hologram.
- Publisher disconnect: frame persists for others (lives in server-side container).
- Depth-sort / blending against hull and terrain; surface-orientation edge cases; perf with many projectors / large frames (tune the config caps).
- Persistence across a server restart.

## Out of scope for v1 (follow-ups)
- Bespoke Projector model/mesh + `ResourceManager` loading.
- Textured 3D meshes / model primitives.
- Bitmap-on-plane projection (chose vector surfaces instead).
- Permission-gated publishing.
- Per-tick animated frames pushed at high rate (current model = republish on change; fine for maps/HUDs, not 60fps animation).

## Note: simplification unlocked by the server-side migration

This design was drafted *before* the ["move computer script execution to the server"](../../src/main/java/luamade/system/module/ComputerModuleContainer.java) migration landed. That migration is done now (as of this writing), which changes one part of this plan for the better:

The original design needed `proj.publish(frame)` to be an explicit, blocking client→server round trip (`PacketCSProjectorPublish` + `PacketSCProjectorPublishResult`, mirroring Vault) **because the authoring client wasn't the source of truth** — the script that built the frame ran on whichever player's client had a computer terminal open, so its local buffer had to be explicitly handed off to the server to become authoritative.

Now that scripts execute **server-side** by default (see `ComputerModuleContainer`, `Terminal`, `ScriptInvoker`), a `Gfx3d` frame built by a running script *is already server truth as it's being built* — there's no "which side is authoritative" question anymore. This means:

- `Gfx3d` can behave like a **live, persistent buffer** owned by the `ProjectorModuleContainer` itself (one per projector block, analogous to how `ComputerModule` now owns a persistent server-side `Gfx2d`), rather than a fresh per-`publish()`-call snapshot object.
- The sync-to-bystanders mechanism can reuse the **exact same revision-based push-sweep pattern** built for computer console/gfx streaming (`ComputerModuleContainer.pushOutputToViewers()`) instead of a bespoke request/response packet pair — except a projector's "viewers" are just "everyone within sync range of the entity" (StarMade's own `ModParticleUtil.getPlayersInRange`), not an explicit connect/disconnect session like a computer dialog.
- `PacketCSProjectorPublish`/`PacketSCProjectorPublishResult` and `ProjectorScriptRequests` from this plan can likely be **dropped entirely** — a script just draws onto the projector's frame directly (via the peripheral) and the container's own `flagUpdatedData()`/sync path takes it from there, the same way `gfx2d.rect(...)` calls don't need an explicit "publish" today.

Re-scope this plan's networking section against that pattern before implementing — the block/Lua-API/rendering sections above (Projector block, `Gfx3d`/`ProjectorSurface` primitives, `ProjectorWorldDrawer`) are still accurate and don't need rework, only the "how does a frame get from the script to bystanders" plumbing simplifies.
