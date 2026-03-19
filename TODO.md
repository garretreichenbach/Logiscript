# TODO

Last updated: 2026-03-18

## Terminal Command Parity Roadmap

### Phase 1: Parser + Safety Baseline

- [x] Unify command argument parsing across built-ins using `parseCommandTokens`.
- [x] Ensure quoted paths with spaces work in `cp`, `mv`, and `edit`.
- [x] Fix `touch` behavior to avoid truncating existing files.
- [ ] Add regression checks for path/argument parsing edge cases.

### Phase 2: Flag Support for Existing Commands

- [x] `ls`: add support for `-a`, `-l`, `-R`.
- [x] `rm`: add support for `-r` and `-f`.
- [x] `cp`: add support for `-r`.
- [x] `head`: support `-n <count>`.
- [x] `tail`: support `-n <count>`.
- [x] `echo`: support `-n`.

### Phase 3: New Common Unix-Like Commands

- [x] Add `find` with minimal subset:
    - [x] `find <path> -name <glob>`
    - [x] `-type f|d`
    - [x] `-maxdepth <n>`
- [x] Add `grep` with minimal subset:
    - [x] `grep <pattern> <path>`
    - [x] `-n`
    - [x] `-i`
    - [x] `-r`
- [x] Add `history` command.
- [x] Add optional `!<n>` history rerun support.
- [x] Add `stat` command for type/size/time and VFS metadata.
- [x] Add `tree` command for directory visualization.

### Parity Notes (Current Gaps to Track)

- [x] `pwd` does not support `-L` / `-P`.
- [ ] `cat` still lacks `-A` and multi-file concat (now supports `-n`).
- [ ] `mkdir` currently behaves similar to always-`-p` but does not parse flags.
- [ ] `wc` still limited (single-file only; no multi-file summary).
- [ ] `which` is basic (no options like `-a`).
- [ ] `kill` has no signal support (job cancel only).

### Suggested Implementation Order

- [x] 
    1) Parser unification + `touch` fix.
- [x] 
    2) Add minimal flags for `ls`, `rm`, `cp`, `head`, `tail`, `echo`.
- [x] 
    3) Implement `find`.
- [x] 
    4) Implement `grep`.
- [x] 
    5) Add `history` + optional `!<n>` rerun.
- [x] 
    6) Add `stat` and `tree`.

### Documentation Follow-Ups

- [x] Update `README.md` command list and flag examples after each phase.
- [x] Update `docs/markdown/core/terminal.md` with option reference.
- [ ] Update `docs/markdown/io/filesystem.md` where file-operation behavior changes.
- [ ] Add migration notes for behavior changes (especially `touch`, `rm -r`, `cp -r`).

