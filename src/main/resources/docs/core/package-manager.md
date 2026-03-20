# Package Manager

LuaMade now includes a built-in package manager design for trusted package distribution.

## Goals

- Allow players to install community Lua libraries and programs.
- Keep package install available even when generic web fetch is disabled.
- Enforce a server-controlled trusted registry endpoint.

## Trust and Security Model

- Generic `httpget`/`httpput` controls remain unchanged.
- Package manager has its own config flags and can be enabled independently.
- Registry base URL is configured by the server owner.
- Package downloads are size-limited.
- Optional trusted-domain enforcement applies to download URLs.
- SHA-256 verification is supported when the registry provides checksums.
- Archive extraction blocks path traversal and absolute paths.

## Proposed Registry API (Azure)

This repository implements the Java client side against these endpoints:

- `GET /v1/search?q=<query>`
  - Returns package summaries.
- `GET /v1/packages/<name>`
  - Returns latest package metadata.
- `GET /v1/packages/<name>/<version>`
  - Returns metadata for a specific version.

### Expected Metadata Fields

The terminal package client accepts this metadata shape (extra fields are ignored):

- `name` (string)
- `version` (string)
- `type` (optional: `library` or `program`)
- `description` (optional string)
- `downloadUrl` (string)
- `sha256` (optional string)
- `artifactType` (optional: `zip` or `lua`, default `zip`)
- `installRoot` (optional string)

## Terminal Commands

The `pkg` command is implemented with these subcommands:

- `pkg search <query>`
- `pkg info <name> [version]`
- `pkg fetch <name> [version] [output-file]`
- `pkg install <name> [version]`
- `pkg list`
- `pkg remove <name>`

## Filesystem Layout

- Installed package metadata file: `/etc/pkg/installed.json`
- Default install roots:
  - libraries: `/lib/<name>`
  - programs: `/bin`

## Azure Repository Split

This repo currently contains only the in-mod Java implementation.
Azure-side service/repository setup is intentionally separate and can be built later.
