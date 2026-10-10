# Player API

The `player` global refers to the player who started the running script, for example by typing a command or running a program. Scripts run on the server, so this is never simply "the local player".

It can be `nil`-backed: startup scripts and other scripts with no player behind them have no player. Check `player.isValid()` first.

## Typical usage

```lua
if not player.isValid() then
  print("No player attached to this script")
  return
end

print("Hello,", player.getName(), "- you have", player.getCredits(), "credits")

if player.confirm("Self-destruct", "Are you sure?") then
  player.message("Self-destruct", "Just kidding.")
end
```

## Reference

- `isValid()`
Returns `true` when a player is attached to this script.

- `getName()`
Returns the player's name, or `nil` when there's no player.

- `getCredits()`
Returns the player's credits, or `nil` when there's no player.

- `confirm(title, body)`
Shows the player an OK/Cancel dialog and waits for them. Returns `true` for OK and `false` for Cancel, Esc, or closing the dialog any other way. Throws when there's no player. Times out after 5 minutes.

- `message(title, body)`
Shows the player a dialog and waits until they close it. Always returns `true`.

## Notes

- To charge or pay a player, use the [Vault API](../systems/vault.md); `player` can't move credits.
