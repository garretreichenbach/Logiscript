# Terminal Command and Editor Roadmap

This document tracks the near-term terminal and editor work for LuaMade.

## Scope

- Add practical Unix-like terminal commands for script development and debugging.
- Add an interactive in-game Lua editor pane.
- Keep all behavior sandboxed to the computer virtual file system.

Out of scope for now:

- `chmod`

## Command Roadmap

### Phase 1 (low risk, read-oriented)

- `which <command-or-path>`: identify built-in command names or existing script paths.
- `head <file> [n]`: print first `n` lines (default 10).
- `tail <file> [n]`: print last `n` lines (default 10).
- `wc <file>`: print line/word/byte counts.
- `find <path> [name-filter]`: recursive file listing in the virtual file system.
- `grep <pattern> <file>`: simple text search (plain contains to start).

### Phase 2 (shell quality of life)

- `history`
- `!!`
- `!<index>`
- `alias`
- `unalias`
- `type` (or `which` extension)

## Editor Pane MVP

### User flow

1. Player runs `nano <file.lua>`.
2. UI switches from terminal mode to file editor mode.
3. Player edits file content in a multiline pane.
4. `Ctrl+S` saves through `FileSystem.write()`.
5. `Ctrl+X` closes editor and returns to terminal mode.
6. `Ctrl+R` saves and runs current file.

### MVP capabilities

- Multiline text editing for one file at a time.
- Current path + modified flag in footer.
- Dirty-check confirmation before discarding unsaved edits.
- All paths normalized via virtual file system.

### Implementation notes

- Reuse `ComputerMode.FILE_EDIT` and route panel behavior by mode.
- Keep terminal prompt protection logic terminal-only.
- Add explicit editor state fields to `ComputerModule` and serialize them in container tags.
- Avoid direct host filesystem access from editor operations.

## Acceptance checks

- Command behavior:
    - `which ls` resolves built-in command.
    - `head /home/file.lua 5` prints exactly 5 lines (or fewer when short).
- Editor behavior:
    - Open existing/new file with `nano`.
    - Save persists across world reload.
    - Return to terminal without losing prompt/input state.

