# Vault API

The Vault block is a programmable credit bank. Its balance is kept on the server, survives restarts, and is tied to a UUID that never changes. **Destroying a Vault destroys its balance.**

Scripts can reach a vault two ways:

- **`vault` global**: address any vault on the computer's entity by UUID.
- **`vault` peripheral**: wrap a specific Vault block, for example `peripheral.wrapRelative("left", "vault")`.

Both run the same checks the Vault's interact dialog uses, against the player who started the script. **Scripts grant no extra authority**: a player who couldn't withdraw through the dialog can't withdraw through a script. Scripts with no player, such as startup scripts, can read balances but can't move credits.

## Access rules

| Adjacent block | Deposit | Withdraw |
| --- | --- | --- |
| None | Members of the vault entity's faction | Members of the vault entity's faction |
| `PUBLIC_PERMISSION_MODULE` (ID 346) | Anyone | Unchanged by this module |
| `FACTION_PERMISSION_MODULE` (ID 936) | Same faction | Same faction |
| `PASSWORD_PERMISSION_MODULE` (Logiscript) | Factions that have authed | Factions that have authed |

Players with no faction never match a faction rule.

## Typical usage

```lua
local bank = peripheral.wrapRelative("left", "vault")
print("Balance:", bank.getBalance())

-- Charge the player who ran the script (shows an OK/Cancel dialog)
if bank.requestPayment(500, "Program licence") then
  print("Thanks!")
end
```

Using the global instead:

```lua
for _, id in ipairs(vault.list()) do
  print(id, vault.getBalance(id))
end
```

## Vault peripheral reference

- `getUuid()`
Returns the vault's persistent UUID, the same ID used by the `vault` global.

- `getBalance()`
Returns the balance in credits.

- `getAccessLevel()`
Returns the strongest access the script's player has: `OWNER`, `FACTION`, `PASSWORD`, `PUBLIC_DEPOSIT` or `NONE`.

- `requestPayment(amount, [reason])`
Shows the player an OK/Cancel dialog, with `reason` in the body. If they press OK, moves `amount` credits from them into the vault. Returns `false` if they cancel. Throws if access is denied or they can't afford it.

- `payout(amount, [reason])`
Pays `amount` credits from the vault to the player. Throws if access is denied or the balance is too low. `reason` isn't recorded yet.

The peripheral also has every [Block](block.md) method.

## `vault` global reference

- `vault.list()`
Returns the UUIDs of every vault on this computer's entity. Doesn't check access.

- `vault.getBalance(uuid)`
- `vault.requestPayment(uuid, amount, [reason])`
- `vault.payoutToPlayer(uuid, amount, [reason])`
Same as the peripheral methods above, for the vault with that UUID. They throw if no vault on this entity has that UUID.

## Notes

- The `vault` global only reaches vaults on the computer's own entity. Wrap a block to use a vault you can reach as a peripheral.
- Every call throws a Lua error on failure, so wrap calls in `pcall` when you want to recover.
