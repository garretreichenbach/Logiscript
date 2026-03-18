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

## Notes

- Paths are sandboxed to this computer's virtual root.
- Escaping the sandbox via traversal is blocked.
- `delete` will not remove non-empty directories.
