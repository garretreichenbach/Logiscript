# LuaMade Recovery TODO

_Last updated: 2026-03-16_

This TODO compares the current codebase to the project objectives in `README.md`, `ARCHITECTURE.md`,
`IMPLEMENTATION_SUMMARY.md`, `TERMINAL_UI_IMPLEMENTATION.md`, and `TERMINAL_FIXES_TESTING.md`.

## Objective Coverage Snapshot

- [ ] **Build/packaging objective** (`./gradlew build`/`jar`) is not met yet.
    - Verified blocker: `./gradlew compileJava` fails with LZ4 capability conflict (`org.lz4:lz4-java` vs
      `at.yawk.lz4:lz4-java`).
- [~] **Terminal + file system core** exists, but UI/config integration is incomplete.
    - Verified compile error: `ConfigManager.getMainConfig()` calls in `src/main/java/luamade/gui/ComputerDialog.java`
      do not match current `ConfigManager` API.
- [~] **In-game computer flow** is incomplete.
    - `src/main/java/luamade/element/block/ActivationInterface.java` exists, but no activation listener/wiring currently
      creates/opens `ComputerDialog`.
- [~] **Persistence objective** is partially implemented.
    - `ComputerModule.saveAndCleanup()` exists, but `ComputerModuleContainer.handle()` is empty and no shutdown save
      orchestration is visible in `src/main/java/luamade/LuaMade.java`.
- [~] **Documentation/glossary objective** is partial.
    - `src/main/java/luamade/manager/GlossaryManager.java` currently loads an empty file list (
      `String[] docFiles = {};`).

## Priority 0 - Make It Compile Again

- [ ] Resolve LZ4 dependency conflict in `build.gradle` so `./gradlew compileJava` succeeds.
    - Pick one artifact source (`org.lz4` or relocated `at.yawk.lz4`) and enforce it.
    - Re-run compile and confirm no capability conflict remains.
- [ ] Fix `ConfigManager` API mismatch used by `ComputerDialog`.
    - Either restore config accessors in `ConfigManager` or replace usage with hardcoded defaults/config fields.
    - Verify `src/main/java/luamade/gui/ComputerDialog.java` has zero compile errors.
- [ ] Run `./gradlew compileJava` after fixes and log all remaining errors (if any) into this file.

## Priority 1 - Restore In-Game Interaction Path

- [ ] Implement event wiring for computer block activation.
    - Add listener registration in mod startup and route activation events to computer handling.
    - Connect `Computer` block activation to creating/fetching a `ComputerModule` and opening `ComputerDialog`.
- [ ] Define where active `ComputerModule` instances live (container/registry) and ensure consistent lookup by block
  position/UUID.
- [ ] Ensure terminal startup path calls `resumeFromLastMode()` or `loadIntoTerminal()` at first open.

## Priority 2 - Persistence and Lifecycle Correctness

- [ ] Implement idle-save checks in `src/main/java/luamade/system/module/ComputerModuleContainer.java` `handle(Timer)`.
- [ ] Add save-on-shutdown flow from `src/main/java/luamade/LuaMade.java` for all active computers.
- [ ] Confirm round-trip behavior: create file -> close UI/server -> reopen -> file persists.

## Priority 3 - Terminal UX Gaps vs Claimed Features

- [ ] Re-enable/finish prompt protection in `ComputerDialog` (`onInputChanged` currently has restoration code commented
  out).
- [ ] Validate and finish input persistence behavior across close/reopen in `ComputerDialog` +
  `ComputerModule.savedTerminalInput`.
- [ ] Replace placeholder `clear` behavior in `src/main/java/luamade/lua/terminal/Terminal.java` with real console
  clear/reset logic.
- [ ] Decide whether command history navigation is in scope now; docs currently present mixed claims.

## Priority 4 - Documentation and Glossary Truthfulness

- [ ] Populate glossary markdown file list in `GlossaryManager` to load docs from `src/main/resources/docs/markdown/`.
- [ ] Reconcile docs with actual state:
    - `README.md`
    - `IMPLEMENTATION_SUMMARY.md`
    - `TERMINAL_UI_IMPLEMENTATION.md`
    - `TERMINAL_FIXES_TESTING.md`
    - `ARCHITECTURE.md`
- [ ] Mark unimplemented items explicitly as planned to avoid future drift.

## Verification Checklist (Use After Each Milestone)

- [ ] `./gradlew compileJava` passes.
- [ ] `./gradlew jar` produces mod jar in StarMade mods directory.
- [ ] In-game: place computer block and open terminal UI.
- [ ] In-game: run `help`, `ls`, `pwd`, `edit`, `cat`, `run /bin/hello.lua`.
- [ ] In-game: prompt protection works and command output renders.
- [ ] In-game: two computers can exchange messages with `net.send`/`net.receive`.
- [ ] Persistence: data survives reopen and restart.

## Notes

- First hard blocker to solve is dependency resolution in `build.gradle`.
- Once compile is green, tackle activation wiring before polishing terminal behavior.

