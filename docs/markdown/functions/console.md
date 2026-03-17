# Console API

`console` is your script's output and context entry point.

## Typical usage

```lua
console.print("Booting...")

block = console.getBlock()
info = block.getInfo()
console.print("Running on " .. info.getName())

console.print("Now: " .. tostring(console.getTime()))
```

## Reference

- `getTime()`
Returns current time in milliseconds.

- `getBlock()`
Returns the computer `Block` currently running the script.

- `print(value)`
Prints a value to terminal output (appends newline).

## Notes

- `print(...)` is also globally available and mapped to `console.print(...)`.
- Convert non-string values with `tostring(...)` when you need exact formatting.
