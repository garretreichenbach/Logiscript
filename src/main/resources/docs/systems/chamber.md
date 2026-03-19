# Chamber API

`Chamber` wraps a reactor chamber/specialization node.

## Reference

- `getName()`
Returns the chamber's display name.

- `getBlockInfo()`
Returns the `BlockInfo` metadata for this chamber's block type.

- `getReactor()`
Returns the parent `Reactor` this chamber belongs to.

- `specify(name: String)`
Attempts to switch the chamber to the specialization named `name`. Returns `true` on success.

- `getValidSpecifications()`
Returns a `String[]` of valid specialization names for this chamber.

- `isUsable()`
Returns `true` when this chamber supports active use (e.g. can be charged/activated).

- `getUsable()`
Returns a `UsableChamber` wrapper when the chamber supports active use, or `nil`.
