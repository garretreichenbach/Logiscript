# Util API

`util` contains small runtime helpers.

## Reference

- `now()`
Returns current Unix time in milliseconds as a `Long`.

- `sleep(millis: Number)`
Pauses execution for `millis` milliseconds. The actual sleep is clamped by the server timeout configuration. Returns the actual duration slept in milliseconds.
