# AGENTS.md: AI Developer Guide for LuaMade

## Project Overview

**LuaMade** is a StarMade mod implementing a Unix-like sandbox environment for in-game computers. It combines:
- **Lua scripting** via LuaJ VM with sandboxed execution
- **Virtual file system** with persistent compression (LZ4-based `.smdat` files)
- **Interactive terminal** with 15+ Unix-like commands
- **Network messaging** between computers

The codebase bridges StarMade's game engine (`api.*` packages) with custom Lua execution systems.

---

## Build & Execution

### Build System (Gradle)
```bash
./gradlew build          # Compile and package mod JAR
./gradlew jar            # Builds to StarMade mods directory (via gradle.properties)
```

**Key Properties** (`gradle.properties`):
- `starmade_root`: Points to StarMade installation (required for building)
- `mod_version`: Auto-updates in `mod.json` during build
- Dependencies: `LZ4-java` (compression), `ModGlossar.jar` (documentation hooks)

**Output**: JAR placed directly in StarMade's `mods/` folder with all dependencies embedded (except StarMade libs).

### Running/Testing
- Requires StarMade 2.0.0+ installed at path specified in `gradle.properties`
- Mod auto-loads on server/client start via `StarMod` extension
- In-game: Right-click Computer Block to access terminal

---

## Architecture: Four Layers

### 1. **StarMade Integration Layer** (Interfaces with Game Engine)
- `LuaMade.java`: Main mod class extending `StarMod`
- Lifecycle hooks: `onEnable()`, `onBlockConfigLoad()`, `onResourceLoad()`
- `EventManager.java`: Registers listeners for `SegmentPieceActivateByPlayer`, `SegmentPieceActivateEvent`
- **Pattern**: Manager classes (`*Manager.java`) use static initialization methods called from `LuaMade`

**Key files**:
- `element/ElementManager.java` - Registers block types
- `manager/ConfigManager.java` - Configuration loading
- `system/module/ComputerModuleContainer.java` - Integrates computers with StarMade's segment system

### 2. **Computer Core Module Layer** (ComputerModule)
`ComputerModule` is the per-computer facade managing four sub-systems:

```
ComputerModule (UUID-based identity)
├─ Console (text output)
├─ FileSystem (virtual filesystem)
├─ Terminal (command-line interface)
└─ NetworkInterface (message passing)
```

**Critical pattern**: Each computer has a unique UUID generated from segment piece position (`generateComputerUUID(long absIndex)`). This UUID is used for:
- File system storage naming (`computer_<UUID>_fs.smdat`)
- Network hostname generation
- State persistence tracking

### 3. **Lua Scripting Layer** (Script Execution & APIs)
**Sandboxed Lua Environment** created in `Terminal.createSandboxedGlobals()`:
- Safe libraries: `base, string, table, math, package, bit32`
- Removed: `dofile, loadfile, load` (prevents arbitrary code execution)
- Exposed APIs: `console, fs, term, net, args`

**Critical pattern**: Scripts receive arguments via `args` table (Lua-side), parsed from shell command arguments.

**Key files**:
- `lua/terminal/Terminal.java` - Command execution + script wrapper
- `lua/fs/FileSystem.java` - Virtual file system implementation
- `lua/networking/NetworkInterface.java` - Message queuing by protocol
- `luawrap/WrapUtils.java` - Type conversion between Java/Lua (handles Object[], primitives, nil)

### 4. **Data Persistence Layer** (Storage & Compression)
Each computer's file system is:
1. **Decompressed** on-demand to `computer_<UUID>_fs/` directory
2. **Cached in memory** during computer operation
3. **Compressed** back to `.smdat` file when idle (tracked via `lastTouched` timestamp)
4. **Cleaned up** via `saveAndCleanup()` method in `ComputerModule`

**Critical pattern**: `CompressionUtils.java` handles directory → compressed-file conversion with:
- LZ4 compression (50-70% typical ratio)
- Recursive traversal with structure preservation
- Length-prefixed string encoding for file paths
- END_DIRECTORY and END_OF_STREAM markers

---

## Code Patterns & Conventions

### Manager Pattern (Initialization Singletons)
All managers use static `initialize()` methods called during mod startup:
```java
public static void initialize(LuaMade instance) { /* ... */ }
```
Called from `LuaMade.onEnable()` or `onClientCreated()`. No instance constructors needed.

### Userdata Wrapper Pattern (Lua-Java Bridge)
Classes exposing methods to Lua extend `LuaMadeUserdata` and annotate callable methods:
```java
public class Terminal extends LuaMadeUserdata {
    @LuaMadeCallable
    public void start() { /* ... */ }
}
```
`WrapUtils` converts Java objects ↔ Lua values (see `wrapSingle()`, `unwrapSingle()`).

### SegmentPiece Utility Pattern
Game blocks are represented as `SegmentPiece` objects. Utility class `SegmentPieceUtils.java` provides helper queries like `getControlledPieces()`.

### File System Isolation
`VirtualFile` wraps Java `File` objects with sandboxing logic:
- All paths normalized relative to computer's root directory
- No absolute path access, no `../../../` traversal

---

## Common Developer Tasks

### Adding a New Terminal Command
1. Define command handler in `Terminal.registerBuiltInCommands()`
2. Pattern: `String commandName` → look up in `commands` HashMap → call `Command.execute(args)`
3. Use `console.print()` for output (handles color via `LuaValue`)

### Exposing New Lua API
1. Create class extending `LuaMadeUserdata` with `@LuaMadeCallable` methods
2. Instantiate in `Terminal.createSandboxedGlobals()` and add to globals table
3. Lua accesses via global variable (e.g., `fs.write()`, `net.send()`)

### Debugging File System Issues
1. Check compressed file: `<world>/computers/computer_<UUID>_fs.smdat`
2. Decompressed state: `<world>/computers/computer_<UUID>_fs/`
3. Use `FileSystem.saveToDisk()` and `cleanupTempFiles()` for manual save/cleanup

### Cross-Computer Communication
Messages are **queued per protocol**:
```java
net.send(targetHostname, protocol, messageData)  // Sender
message = net.receive(protocol)                  // Receiver (blocks until available)
```
Use protocol names as channel identifiers (e.g., "chat", "sensor-network").

---

## Integration Points & Dependencies

### StarMade API Dependencies
- `api.listener.events.*`: Game events (block activation, manager registration)
- `api.mod.StarMod`: Base class for mods
- `org.schema.game.common.data.SegmentPiece`: In-game block representation
- `api.config.BlockConfig`: Block type registration

### External Libraries
- **LuaJ** (`org.luaj.vm2`): Lua VM for script execution
- **LZ4-Java** (`org.lz4.*`): Compression for file system persistence

### Cross-Module Communication
- **EventManager** listens to StarMade events and routes to block activation handlers
- **ComputerModuleContainer** integrates computers into segment controller's module system
- **GlossaryManager** loads markdown documentation into in-game glossary

---

## Documentation & Examples

### Key Documentation
- `ARCHITECTURE.md`: Detailed component descriptions + usage examples
- `README.md`: Feature overview + API reference
- `docs/markdown/`: Topic-specific guides grouped by category (`general/`, `runtime-api/`, `game-systems/`, `block-wrappers/`, `entity-wrappers/`)
- Example scripts: `examples/TerminalExample.lua`, `src/main/resources/bin/*.lua`

### Glossary System
Documentation is loaded dynamically from markdown files in `src/main/resources/docs/` (with category subfolders). Titles are extracted from headers. Integrated into StarMade's in-game documentation UI.

---

## Testing & Validation

### Manual Testing Workflow
1. Build: `./gradlew jar`
2. Launch StarMade with modified mod
3. Create computer block in-game
4. Right-click to open terminal, test commands

### Critical Test Cases
- File system persistence: Create files, save, restart → data persists
- Script execution: Run `/bin/hello.lua` with arguments → correct output
- Network messaging: Two computers sending/receiving via protocol
- Terminal history: Up arrow navigates command history

### Known Issues to Watch
- File system decompression failures if world data path is inaccessible
- Lua sandbox escape attempts via `load()` → blocked intentionally
- SegmentPiece references may change during world save/load cycles

---

## Project-Specific Gotchas

1. **UUID Format**: `generateComputerUUID()` uses segment piece's `absIndex` (long), produces hex string. Used everywhere for file naming, networking.
2. **ComputerMode Enum**: Tracks state (OFF, IDLE, TERMINAL, FILE_EDIT). Loading requires `resumeFromLastMode()` to restore proper context.
3. **Compression Roundtrip**: Always decompress before reading, compress before saving. `cleanupTempFiles()` must be called to prevent stale directories.
4. **Lua Library Whitelist**: `String.match()`, `String.gsub()` available via `package.loaded.string`. No direct `io` or `os` access by design.
5. **Path Normalization**: All paths converted to forward slashes, `..` removed. Scripts see normalized paths; Java side uses normalized paths for comparisons.

---

## Quick Reference: StarMade

- StarMade is a voxel-based space sandbox game with modding support via Java.
- MCP tools can be used to access game internals, but you should refernece only the `release` branch for stability.
- Mods should prefer to use the FastListener system when possible for better performance.

For Lua API docs, see `docs/markdown/runtime-api/` and `docs/markdown/general/`.

