# VirtualFile API

`VirtualFile` is a file wrapper returned by internal filesystem operations.

## Reference

- `listFiles()`
Returns children as `VirtualFile[]` for directories.

- `getParentFile()`
- `getPath()`
Path relative to virtual root.

- `getAbsolutePath()`
Sandbox-relative absolute path.

- `isDirectory()`
- `getName()`

## Notes

- Most scripts use `fs` directly and do not need this wrapper.
