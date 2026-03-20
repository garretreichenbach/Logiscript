# Util API

`util` contains small runtime helpers.

You can use it as a global (`util`) or load it explicitly:

```lua
local util = require("util")
```

## Reference

- `util.now()`
Returns current Unix time in milliseconds as a `Long`.

- `util.sleep(millis: Number)`
Pauses execution for `millis` milliseconds. The actual sleep is clamped by the server timeout configuration. Returns the actual duration slept in milliseconds.
