# FileSystem API

`fs` exposes the computer's sandboxed virtual filesystem.

## Typical usage

```lua
fs.makeDir("/home/logs")
fs.write("/home/logs/boot.txt", "online")

if fs.exists("/home/logs/boot.txt") then
  print(fs.read("/home/logs/boot.txt"))
end

for _, name in ipairs(fs.list("/home")) do
  print(name)
end

-- Protect a scope and require auth for writes
fs.protect("/home/logs", "secret123", "write, delete")
print(fs.write("/home/logs/boot.txt", "denied until auth")) -- false

fs.auth("secret123")
print(fs.write("/home/logs/boot.txt", "allowed after auth")) -- true

fs.clearAuth()
```

## Reference

- `list(path)`
Returns file/directory names in `path`.

- `makeDir(path)`
Creates a directory recursively. Returns `true` on success.

- `read(path)`
Reads a file and returns text content, or `nil`.

- `write(path, content)`
Writes UTF-8 text to a file, creating parent directories when needed.

- `delete(path)`
Deletes a file or an empty directory.

- `changeDir(path)`
Changes current working directory.

- `getCurrentDir()`
Returns current working directory (for example `/home`).

- `exists(path)`
Returns whether a path exists.

- `isDir(path)`
Returns whether a path is a directory.

- `normalizePath(path)`
Returns canonical virtual path (resolves `.`, `..`, and relative paths).

- `protect(path, password, [operations])`
  Adds or updates password protection for a path scope.

- `unprotect(path, password)`
  Removes protection rule from a path when password matches.

- `auth(password)`
  Unlocks all matching protected scopes for the current session.

- `clearAuth()`
  Clears the current filesystem auth session.

- `listPermissions()`
  Returns all configured protection rules with lock state.

- `getPermissions(path)`
  Returns the effective protection rule for `path`, or `none`.

- `getLastError()`
  Returns the last filesystem error (including permission-denied reason).

## Notes

- Paths are sandboxed to this computer's virtual root.
- Escaping the sandbox via traversal is blocked.
- `delete` will not remove non-empty directories.
- Protection rules are path-scoped and inherited by child paths (longest matching scope wins).
- Supported operation tokens: `read`, `write`, `delete`, `list`, `all`.
- Aliases: `copy` (`read+write`), `move` (`read + write + delete`), `paste` (`write`), `rw` (`read + write`).
- `auth(password)` unlocks matching scopes until `clearAuth()` or computer/session reset.
- Protection metadata is persisted under `/etc/.fs_permissions` by the runtime.
