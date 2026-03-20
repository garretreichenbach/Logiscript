# JSON Library (`json`)

The `json` global provides lightweight JSON encode/decode helpers for scripts.

You can use it as a global (`json`) or load it explicitly:

```lua
local json = require("json")
```

## Reference

- `json.encode(value)`
  Returns a JSON string for Lua values (`table`, `string`, `number`, `boolean`, `nil`).

- `json.decode(text)`
  Parses JSON text into Lua tables and primitives.

- `json.null`
  Sentinel value used for JSON `null`.

## Notes

- Object keys must be strings when encoding Lua tables as JSON objects.
- Array encoding uses consecutive numeric keys starting at `1`.
- `json.decode` returns `json.null` for JSON `null`.

## Example

```lua
request = {
    fleet = "alpha",
    command = "move",
    target = {
        x = 8,
        y = 8,
        z = 8
    }
}

payload = json.encode(request)
response = term.httpPut("https://example.com/api/fleet", payload, "application/json")

if response ~= nil and response ~= "" then
    decoded = json.decode(response)
    if decoded.status == "ok" then
        print("Fleet command accepted")
    end
end
```

