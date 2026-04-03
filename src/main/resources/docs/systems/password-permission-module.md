# PasswordPermissionModule API

`PasswordPermissionModule` is a Logiscript-native permission block that complements StarMade's built-in `PUBLIC_PERMISSION_MODULE` (346) and `FACTION_PERMISSION_MODULE` (936).

When placed adjacent to any block that uses StarMade's permission system — or adjacent to a `DataStore` — it gates access behind a password that a computer script must supply. Authentication is faction-scoped and time-limited (5 minutes by default).

## How it works

1. A computer on the **same entity** as the module sets the password via `setPassword()`.
2. Any computer in any faction calls `auth("password")` to authenticate.
3. On success, the computer's faction is registered as authenticated for 5 minutes.
4. During that window, the authenticated faction can:
   - Access adjacent `DataStore` blocks (via `datastore:auth()` or direct `checkAccess`)
   - Activate adjacent console blocks (via StarMade's permission event)
   - Dock to / interact with blocks gated by this module (rail, beam, shipyard)

## Typical usage

```lua
-- On a setup computer (same entity as the module):
local ppm = console.getBlock():getEntity():getBlockAt(vec3i(0, 1, 0)):wrapAs("passwordmodule")
ppm:setPassword("secret")

-- On any other computer (possibly a different entity):
local ppm = block.wrapAs(nearbyModulePiece, "passwordmodule")
if ppm:auth("secret") then
    print("Authenticated for 5 minutes")
end

-- Check current auth state:
print(ppm:isAuthed())  -- true/false

-- Revoke auth early:
ppm:deauth()
```

## Reference

- `auth(password: String)`
  Verifies `password` against this module's stored hash. On success, registers the
  calling computer's faction in the auth manager and returns `true`.
  If no password is configured, any call to `auth()` succeeds.

- `deauth()`
  Immediately revokes the calling computer's faction auth for this module.

- `isAuthed()`
  Returns `true` if the calling computer's faction is currently authenticated.

- `setPassword(password: String)`
  Sets the password for this module. Requires the computer to be on the **same entity**
  as the module. Pass `nil` or `""` to clear the password.

- `clearPassword()`
  Removes the password, making the module grant access to any faction that calls
  `auth()` regardless of what they pass. Requires same-entity computer.

- `isProtected()`
  Returns `true` when a password is currently configured on this module.

## Integration

The `PasswordPermissionModule` integrates with StarMade's `BlockPublicPermissionEvent`,
which fires for:

- Rail docking checks
- Personal beam targeting
- Shipyard access
- Console block activation

Any of these operations on a block adjacent to a `PasswordPermissionModule` will
succeed if the accessing entity's faction has previously authenticated via `auth()`.

## Notes

- Auth state is **in-memory only** and does not survive server restarts.
- Auth is **faction-scoped**: one computer authenticating grants access to all
  computers in the same faction for the duration of the TTL.
- Faction ID `0` (no faction) is never granted auth.
- Password configuration requires a computer physically on the same entity as the module.
