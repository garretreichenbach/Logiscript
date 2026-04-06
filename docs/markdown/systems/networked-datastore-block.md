# Networked Data Store API

`NetworkedDataStore` is a persistent key-value store that registers itself in a global name registry, allowing any computer to access its data via the network API (`net:getDataStore(name)`) **without the owning entity being loaded**.

Unlike the regular [DataStore](datastore-block.md), access control is configured programmatically rather than through adjacent permission blocks.

## Key differences from DataStore

| Feature | DataStore | Networked Data Store |
| --- | --- | --- |
| Access when entity unloaded | No | **Yes** |
| Access control | Adjacent permission blocks | Configurable via `setAccessLevel()` |
| Network-accessible | No | **Yes**, via `net:getDataStore(name)` |
| Requires registration | No | Yes, must call `register(name)` |
| Data on block destruction | Preserved | **Deleted** |

## Setup (on the entity with the block)

```lua
-- Wrap the Networked Data Store block
local nds = peripheral.wrap("networked_data_store")

-- Register with a globally unique name
nds:register("faction-market-prices")

-- Set access level: "entity", "faction", or "public"
nds:setAccessLevel("public")

-- Write data
nds:set("iron_ore", "150")
nds:set("gold_ore", "500")
```

## Remote access (from any computer, anywhere)

```lua
-- Get a handle by name - no entity load required
local store = net:getDataStore("faction-market-prices")

if store then
    print(store:getValue("iron_ore"))   -- "150"
    store:set("iron_ore", "175")        -- write back

    local keys = store:keys()
    for i = 1, #keys do
        print(keys[i], store:getValue(keys[i]))
    end
end
```

## Access levels

| Level | Who can access |
| --- | --- |
| `"entity"` (default) | Only computers on the **same entity** as the block. Remote access via `net:getDataStore()` is denied. |
| `"faction"` | Any computer belonging to the **same faction** as the block's owning entity. |
| `"public"` | **Any** computer, regardless of faction. |

Access level is set via `setAccessLevel()` and can only be changed by a computer on the same entity as the block.

## Block peripheral reference

These methods are available when wrapping the physical block via `peripheral.wrap()`.

### Registration & configuration

- `register(name: String)`
  Registers this store in the global registry with the given name. Names must be globally unique, 1-64 characters, and contain only letters, digits, hyphens, and underscores. Returns `false` if the name is already taken.

- `unregister()`
  Removes this store from the global registry and **deletes all its data**. Returns `true` on success.

- `isRegistered()`
  Returns `true` if this store is currently registered in the global registry.

- `getStoreName()`
  Returns the registered name, or `nil` if not yet registered.

- `setAccessLevel(level: String)`
  Sets the access level to `"entity"`, `"faction"`, or `"public"`. Requires the computer to be on the same entity.

- `getAccessLevel()`
  Returns the current access level as a string.

- `getStoreId()`
  Returns the persistent UUID backing this store's data.

- `getOwnerFactionId()`
  Returns the integer faction ID of the entity this block is installed on.

### Data access

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

## Remote handle reference (`RemoteDataStore`)

These methods are available on the handle returned by `net:getDataStore(name)`.

### Data access

- `getValue(key: String)` - Returns value at `key`, or `nil`.
- `set(key: String, value: String)` - Stores `value` under `key`.
- `delete(key: String)` - Removes `key`. Returns `true` if present.
- `has(key: String)` - Returns `true` if `key` exists.
- `keys()` - Returns table of all keys.
- `keys(prefix: String)` - Returns table of keys starting with `prefix`.
- `size()` - Returns total key count.

### Info

- `getStoreName()` - Returns the registered name.
- `getAccessLevel()` - Returns the access level string.
- `getOwnerFactionId()` - Returns the owning faction ID.

## Limits

| Item | Limit |
| --- | --- |
| Store name length | 64 characters |
| Store name characters | Letters, digits, hyphens, underscores |
| Key length | 256 characters |
| Value length | 65 536 characters (64 KiB) |
| Keys per store | 10 000 |

Exceeding any limit raises a Lua error.

## Notes

- **Data is destroyed** when the block is removed or killed. This is intentional - the block is the authority for the data's lifecycle.
- Names are **case-insensitive** and globally unique across all factions.
- The store's UUID is assigned when the block is placed and never changes.
- Data is stored at `<worldData>/datastores/<uuid>.json` (same system as regular DataStore).
- The global name registry is stored at `<worldData>/networked_datastores.json`.
- A store with `"entity"` access level **cannot** be accessed remotely via `net:getDataStore()`. Set access to `"faction"` or `"public"` for remote access.
