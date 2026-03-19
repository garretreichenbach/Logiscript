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
- [x] `cat` supports `-n`, `-A`, and multi-file concat.
- [x] `mkdir` parses `-p` and supports multi-path creation.
- [x] `wc` supports `-l`, `-w`, `-c`, and multi-file `total` summary.
- [x] `which` supports `-a` to show all matches.
- [x] `kill` accepts common signal forms (`-TERM`, `-KILL`, `-INT`, `-HUP`, numeric aliases) and maps to cancel.

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
- [x] Update `docs/markdown/io/filesystem.md` where file-operation behavior changes.
- [x] Add migration notes for behavior changes (especially `touch`, `rm -r`, `cp -r`).

