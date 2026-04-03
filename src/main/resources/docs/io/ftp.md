# FTP API

`ftp` transfers files directly between computers in memory, bypassing the network message queue.
This makes it far more efficient than line-by-line script-driven transfers via `net`.

## Typical usage

```lua
-- ── Server computer ──────────────────────────────────────────────────────────
ftp.listen("secret")          -- accept one client at a time
-- optional: ftp.listen("secret", true)  -- read-only (no uploads/deletes)

-- ── Client computer ──────────────────────────────────────────────────────────
if ftp.connect("server-host", "secret") then
    -- single-file transfers
    ftp.download("/remote/prog.lua", "/local/prog.lua")
    ftp.upload("/local/config.txt", "/remote/config.txt")

    -- recursive directory transfers
    local n = ftp.downloadDir("/remote/libs", "/local/libs")
    print("downloaded " .. n .. " files")

    ftp.disconnect()
end

-- ── Server computer (cleanup) ────────────────────────────────────────────────
ftp.stop()
```

## Server API

- `listen(password)`
  Start accepting FTP connections with the given password. Returns `false` if already listening or password is blank. Only one client may connect at a time.

- `listen(password, readOnly)`
  Same as above, but when `readOnly` is `true` clients are limited to `download`, `downloadDir`, `list`, `exists`, and `isDir`. Upload, `mkdir`, and `delete` operations are rejected.

- `stop()`
  Stop listening and disconnect any connected client.

- `isListening()`
  Returns `true` if this computer is currently listening for FTP connections.

- `getConnectedClient()`
  Returns the hostname of the connected client, or `nil`.

## Client API

- `connect(serverHostname, password)`
  Connect to a listening FTP server. Returns `true` on success. Fails if already connected, the server is busy, or the password is wrong.

- `disconnect()`
  Disconnect from the current FTP server.

- `isConnected()`
  Returns `true` if this computer has an active FTP connection.

- `getServer()`
  Returns the hostname of the connected server, or `nil`.

## Remote file inspection

- `exists(remotePath)`
  Returns `true` if the path exists on the server.

- `isDir(remotePath)`
  Returns `true` if the path is a directory on the server.

- `list(remotePath)`
  Returns an array of file/directory names in the remote directory, or `nil` if not connected or path is invalid.

## Remote mutations

- `mkdir(remotePath)`
  Creates a directory on the server. Returns `false` if the server is read-only or the call fails.

- `delete(remotePath)`
  Deletes a file or directory on the server. Returns `false` if the server is read-only or the call fails.

## File transfer

- `download(remotePath, localPath)`
  Downloads a single file from the server to this computer's filesystem. Returns `true` on success.

- `upload(localPath, remotePath)`
  Uploads a single file from this computer's filesystem to the server. Returns `false` if the server is read-only.

- `downloadDir(remotePath, localPath)`
  Recursively downloads an entire remote directory. Creates `localPath` if it does not exist.
  Returns the number of files transferred, or `-1` if not connected or the source is not a directory.

- `uploadDir(localPath, remotePath)`
  Recursively uploads an entire local directory to the server. Creates `remotePath` if it does not exist.
  Returns the number of files transferred, or `-1` if not connected, read-only, or the source is not a directory.

## Notes

- `listen` creates a single-client lock. A second client trying to `connect` while one is already connected will receive `false`.
- The server's existing `fs.protect` / `fs.auth` rules are enforced on every transfer. Protect a path and call `fs.auth` on the server before `ftp.listen` if you want FTP clients to be able to write to protected directories.
- Transfers are in-memory operations. No data crosses the network message queue and there is no per-file protocol overhead.
- Both the server and client must be in the same running game instance (same JVM). FTP does not work across separate servers.
- When a computer is removed or shut down, any active FTP server or client state is cleaned up automatically.
- `downloadDir` and `uploadDir` skip files that fail to read or write (e.g. due to permission rules) and continue transferring the rest; the returned count reflects only files that succeeded.
