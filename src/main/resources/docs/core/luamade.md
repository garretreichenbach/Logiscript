# LuaMade API Overview

LuaMade lets you run sandboxed Lua scripts on Computer Blocks.

Use it to automate ship behavior, build status dashboards, and coordinate multiple computers through network messaging.

## Quick start

1. Place and activate a Computer Block.
2. Open terminal and create a script using either editor:

```text
edit /home/hello.lua
```

Or use the in-game editor:

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
- `shell`: compatibility shell helpers (`shell.run(pathOrCommand, ...)`).
- `util`: utility helpers from bundled Lua library plus native `now`/`sleep`.
- `json`: bundled JSON encode/decode helpers (`encode`, `decode`, `null`).
- `coroutine`: standard Lua coroutine API (`create`, `resume`, `yield`, `status`, `running`, `wrap`).
- `package`: sandboxed package table (`loaded`, `preload`, `path`, `cpath`).

Terminal web requests are available through `httpget` / `term.httpGet(url)` and `httpput` /
`term.httpPut(url, body[, contentType])` when enabled in server config.

Trusted package distribution is available through the `pkg` terminal command when `package_manager_enabled` is enabled.
This is controlled separately from generic web fetch settings.

When `web_fetch_trusted_domains_only` or `web_put_trusted_domains_only` is enabled, trusted domains are loaded from:

```text
config/luamade/trusted_domains.txt
```

The file is created automatically with default entries on first startup.

Package manager registry base URL is loaded from:

```text
config/luamade/package_manager_base_url.txt
```

Quick JSON example:

```lua
raw = json.encode({ status = "ok", count = 3, enabled = true })
obj = json.decode(raw)
print(obj.status, obj.count)
```

## Sandbox module loading

Sandboxed loaders are available and constrained:

- `dofile(path, ...)` executes a resolved script path inside the sandbox.
- `loadfile(path)` loads a resolved script file as a function.
- `load(source)` loads an in-memory Lua chunk.
- `require("module.name")` loads modules using strict sandbox rules.

Path policy:

- `dofile` and `loadfile` can load any `.lua` file path inside the computer sandbox (for example `/home`, `/bin`,
  `/etc`, `/lib`).
- `require` is for reusable libraries and searches library roots only.

`require` rules:

- Module names must be lowercase dot notation with `[a-z0-9_]` segments (for example `myteam.math`).
- Resolution order is `package.loaded`, then `package.preload`, then filesystem search.
- Filesystem search checks player library roots first: `/lib/<module>.lua`, `/etc/lib/<module>.lua`.
- For allowlisted names, `require` can also load bundled libs from `/scripts/lib/<module>.lua`.
- Loaded modules are cached in `package.loaded`; modules returning `nil` are cached as `true`.
- Cyclic loads are blocked with a recursion guard (`require("a")` -> `require("a")`).

Allowlisted module names are loaded from:

```text
config/luamade/allowed_lua_packages.txt
```

The file is created automatically with safe defaults (`json`, `util`, `vector`).

Bundled module compatibility:

- `json` and `util` are exposed as globals and are also available through `require("json")` / `require("util")`.
- `vector` is exposed as a global and is also available through `require("vector")`.

Shared library examples:

```lua
-- /lib/myteam/math.lua
local M = {}

function M.add(a, b)
    return a + b
end

return M
```

```lua
-- anywhere else
local mathlib = require("myteam.math")
print(mathlib.add(2, 3))
```

## Typical workflow

Command batching:

- The terminal accepts multiple commands in one submission.
- Separate commands with newlines, `;`, `&&`, or `||` (outside quotes).
- `;` and newlines always continue to the next command.
- `&&` only runs the next command if the previous command succeeded.
- `||` only runs the next command if the previous command failed.
- Empty/whitespace-only segments are ignored.

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
- Execution is controlled by server-configured parallel limits and manual interruption/cancellation.
- Coroutines are cooperative (single script thread) and do not create parallel workers.
- Web fetch is server-gated and can be restricted to trusted domains only.
- Protected filesystem paths require successful auth before gated operations are allowed.
