# DataStore API

`DataStore` is a persistent, per-block key-value store. Each DataStore block holds its own independent data identified by a stable UUID that never changes, even if the block is moved within an entity. Data is written to disk on every mutation and survives server restarts.

Access is controlled by physically placing permission module blocks adjacent to the DataStore in the world.

## Access levels

| Adjacent block | Who can access |
| --- | --- |
| None | Only computers on the **same entity** as the DataStore |
| `FACTION_PERMISSION_MODULE` (ID 936) | Any computer in the **same faction** as the DataStore's owning entity |
| `PUBLIC_PERMISSION_MODULE` (ID 346) | **Any** computer, regardless of faction |
| `PASSWORD_PERMISSION_MODULE` (Logiscript) | Any computer whose faction has called `auth(password)` successfully |

Multiple permission modules can coexist. The most permissive adjacent module wins. Access is checked on every call — changing adjacent blocks takes effect immediately without restarting the script.

## Typical usage

```lua
-- Wrap the DataStore block adjacent to this computer
local ds = console.getBlock():getEntity():getBlockAt(vec3i(1, 0, 0)):wrapAs("datastore")

-- If protected by a PASSWORD_PERMISSION_MODULE, authenticate first
if ds:getAccessLevel() == "password" then
    assert(ds:auth("mySecret"), "Wrong password")
end

-- Basic read/write
ds:set("status", "online")
print(ds:getValue("status"))  -- "online"

-- Delete a key
ds:delete("status")

-- List keys with a prefix
local keys = ds:keys("sensor/")
for i = 1, #keys do
    print(keys[i], ds:get(keys[i]))
end
```

## Reference

- `auth(password: String)`
  Verifies `password` against any adjacent `PASSWORD_PERMISSION_MODULE` blocks.
  On success, registers the computer's faction in the auth manager for 5 minutes,
  granting access to this DataStore and any other blocks protected by the same module.
  Returns `true` if at least one adjacent module accepted the password.

- `getValue(key: String)`
  Returns the value stored at `key`, or `nil` when absent.

- `set(key: String, value: String)`
  Stores `value` under `key`. Pass `nil` as the value to delete the key.

- `delete(key: String)`
  Removes `key`. Returns `true` when a key was actually present.

- `has(key: String)`
  Returns `true` when `key` exists in the store.

- `keys()`
  Returns a table (array) of all keys in this store.

- `keys(prefix: String)`
  Returns a table (array) of all keys that begin with `prefix`.

- `size()`
  Returns the total number of keys currently stored.

- `getStoreId()`
  Returns the persistent UUID string that identifies this block's data file.

- `getOwnerFactionId()`
  Returns the integer faction ID of the entity this DataStore is installed on.

- `getAccessLevel()`
  Returns the effective access level as a string: `"public"`, `"faction"`, `"password"`, or `"entity"`.

## Limits

| Item | Limit |
| --- | --- |
| Key length | 256 characters |
| Value length | 65 536 characters (64 KiB) |
| Keys per store | 10 000 |

Exceeding any limit raises a Lua error on the offending `set()` call.

## Notes

- Data is **per-block**: each DataStore block has its own independent key-space. Place multiple blocks for isolated namespaces on the same ship.
- The block's UUID is assigned the moment it is placed and never changes, so data is not lost if blocks are rearranged within an entity.
- Data is stored at `<worldData>/datastores/<uuid>.json`.
- Faction ID `0` is the "no faction" / wilderness faction. Cross-faction access via `FACTION_PERMISSION_MODULE` is denied when either the DataStore or the accessing computer has faction ID `0`.
