# LuaMade API Overview

LuaMade lets you run sandboxed Lua scripts on Computer Blocks.

Use it to automate ship behavior, build status dashboards, and coordinate multiple computers through network messaging.

## Quick start

1. Place and activate a Computer Block.
2. Open terminal and create a script:

```text
nano /home/hello.lua
```

```lua
-- /home/hello.lua
print("Hello from LuaMade")

block = console.getBlock()
info = block.getInfo()
print("Running on:", info.getName(), "id=", info.getId())
```

1. Save and run:

```text
run /home/hello.lua
```

## Core globals

- `console`: output and context access (`getBlock`, `print`, `getTime`).
- `fs`: virtual filesystem read/write/list APIs, including password-protected path scopes (`protect`, `auth`,
  `unprotect`).
- `term`: terminal/session controls and command hooks.
- `net`: direct, channel, local, and modem networking APIs.
- `peripheral`: relative and absolute nearby block access helpers.
- `args`: script argument array for `run` and direct script execution.
- `util`: utility helpers from bundled Lua library plus native `now`/`sleep`.
- `vector`: bundled vector helper library.

Terminal web requests are available through `httpget` / `term.httpGet(url)` and `httpput` /
`term.httpPut(url, body[, contentType])` when enabled in server config.

When `web_fetch_trusted_domains_only` or `web_put_trusted_domains_only` is enabled, trusted domains are loaded from:

```text
config/luamade/trusted_domains.txt
```

The file is created automatically with default entries on first startup.

## Typical workflow

1. Read current state from `console.getBlock()` and wrappers.
2. Perform logic in Lua.
3. Persist local state to files under `/home` or `/etc`.
4. Use `net` to coordinate with other computers.

Peripheral side lookups are orientation-aware: `front`/`back`/`left`/`right` follow the computer block's facing.

## Startup behavior

If `/etc/startup.lua` exists, it runs at terminal boot.

Use startup for:

- custom boot banner
- prompt template setup
- registering custom terminal commands
- opening network channels/modem listeners

## Safety and limits

- Scripts run in a sandboxed Lua environment.
- Host filesystem and unsafe loaders are not exposed.
- Execution is controlled by server-configured timeout and parallel limits.
- Web fetch is server-gated and can be restricted to trusted domains only.
- Protected filesystem paths require successful auth before gated operations are allowed.
