# Introduction
## What is LuaMade?
LuaMade is a StarMade mod that adds programmable computers into the game, similar to ComputerCraft in Minecraft.
It allows players to create complex systems and automate tasks using the Lua programming language.

## Features
LuaMade provides a Unix-like computing environment with the following features:

### File System
- Create, read, write, and delete files
- Create and navigate directories
- Path manipulation with support for relative paths
- Sandboxed virtual file system per computer
- Persistent storage compressed to disk
- Password-protected path scopes with operation-based access control

### Terminal
- Interactive command-line interface with built-in Unix-like commands
- Command history navigation
- Support for custom Lua commands
- Direct Lua script execution from terminal
- Built-in commands include: ls, cd, pwd, cat, mkdir, touch, rm, cp, mv, edit, find, grep, history, stat, tree,
  which, head, tail, wc, protect, unprotect, fsauth, perms, run, runbg, jobs, kill, httpget, httpput, nano, name, echo,
  reboot, clear, help, exit

### Networking
- Computer-to-computer communication
- Message passing with different protocols
- Hostname management
- Broadcasting capabilities
- Message queuing per protocol

### Sandboxing
- Isolated Lua execution environment
- Safe library subset (base, string, table, math, package, bit32)
- No access to unsafe file operations
- Each computer has its own isolated file system

## API Reference

### File System API
Access the file system API through the `fs` global variable:
```lua
-- List files in a directory
files = fs.list("/home")

-- Create a directory
fs.makeDir("/home/user")

-- Write to a file
fs.write("/home/user/hello.txt", "Hello, world!")

-- Read from a file
content = fs.read("/home/user/hello.txt")

-- Delete a file
fs.delete("/home/user/hello.txt")

-- Change directory
fs.changeDir("/home/user")

-- Get current directory
currentDir = fs.getCurrentDir()

-- Protect /home/user for read/write/delete/list operations
fs.protect("/home/user", "secret123", "all")

-- Unlock protected scopes for this session
fs.auth("secret123")

-- Inspect current rule for a path
print(fs.getPermissions("/home/user"))

-- Clear auth session
fs.clearAuth()
```

### Terminal API
Access the terminal API through the `term` global variable:
```lua
-- Start the terminal
term.start()

-- Handle user input
term.handleInput("ls")

-- Run a terminal command from Lua code
term.runCommand("echo hello from lua")

-- Register a custom command
term.registerCommand("hello", function(args)
    print("Hello, " .. args .. "!")
end)

-- Customize prompt and prompt rendering
term.setPromptTemplate("[{hostname}] {dir} > ")
term.setAutoPrompt(true)

-- Web requests (subject to server config and domain allowlist)
body = term.httpGet("https://example.com/status")
response = term.httpPut("https://example.com/api/fleet", "{\"command\":\"recall\"}")
jsonResponse = term.httpPut("https://example.com/api/fleet", "{\"command\":\"recall\"}", "application/json")

-- Re-run /etc/startup.lua and reset terminal state
term.reboot()
```

### Terminal Commands
The terminal supports the following built-in commands:

Command chaining is supported in a single submission:

- Use newlines or `;` to always run the next command.
- Use `&&` to run the next command only if the previous command succeeds.
- Use `||` to run the next command only if the previous command fails.

- `ls [-a] [-l] [-R] [directory]` - List files (`-a` hidden, `-l` long format, `-R` recursive)
- `cd <directory>` - Change current directory
- `pwd [-L|-P]` - Print working directory (logical/physical mode)
- `cat [-n] [-A] <file>...` - Display file contents (`-n` line numbers, `-A` visible control chars)
- `mkdir [-p] <directory>...` - Create directories (`-p` tolerates existing dirs)
- `touch <file>` - Create an empty file
- `rm [-r] [-f] <path>...` - Delete files/dirs (`-r` recursive, `-f` ignore missing/errors)
- `cp [-r] <source> <dest>` - Copy files (`-r` for directories)
- `mv <source> <dest>` - Move or rename a file
- `edit <file> <content>` - Write content to a file
- `find [path] [-name <glob>] [-type f|d] [-maxdepth <n>]` - Search files and directories
- `grep [-n] [-i] [-r] <pattern> <path>` - Search file contents (`-n` line numbers, `-i` ignore case, `-r` recursive)
- `history` - Show indexed command history
- `!<n>` - Re-run history entry number `n`
- `stat <path>...` - Show metadata for files/directories
- `tree [-a] [-L depth] [path]` - Print directory tree (`-a` includes dot files)
- `protect <path> <password> [ops]` - Protect path scope by operations (`read,write,delete,list,all`)
- `unprotect <path> <password>` - Remove path protection
- `fsauth <password>` - Unlock protected filesystem scopes for this terminal session
- `fsauth --clear` - Clear current filesystem auth session
- `perms [path]` - List protection rules or show effective rule for one path
- `run <script> [args]` - Execute a Lua script with optional arguments
- `runbg <script> [args]` - Execute a Lua script in background (limited parallelism)
- `jobs` - List background script jobs
- `kill [-TERM|-KILL|-INT|-HUP|-15|-9|-2|-1] <job-id>` - Stop a background script job
- `which [-a] <command-or-path>` - Resolve built-ins or file paths (`-a` shows all matches)
- `httpget <url> [output-file]` - Fetch web content using HTTP GET
- `httpput [--content-type <mime>] <url> <payload|@file> [output-file]` - Send web content using HTTP PUT (`@file` reads
  payload from VFS file)
- `name [new-name|--reset]` - Show or change the displayed computer name in the prompt
- `head [-n lines] <file>` - Show first lines of a file
- `tail [-n lines] <file>` - Show last lines of a file
- `wc [-l] [-w] [-c] <file>...` - Show selected line/word/byte counts (`total` for multi-file)
- `nano <file>` - Open file in editor mode
- `echo [-n] <text>` - Print text to the terminal (`-n` suppresses newline)
- `clear` - Clear the terminal screen
- `reboot` - Reload `/etc/startup.lua` and reset terminal UI state
- `help` - Show available commands
- `exit` - Exit the terminal

### Writing Lua Scripts
Scripts executed in the terminal have access to these global variables:

- `console` - Console API for output
- `print` - Shortcut for console.print()
- `fs` - File system API
- `term` - Terminal API
- `net` - Network API
- `util` - Built-in Lua utility library with native timing helpers
- `json` - Built-in JSON encode/decode library
- `package` - Sandboxed package table (`loaded`, `preload`, `path`, `cpath`)
- `args` - Table of command-line arguments

Module loading notes:

- `dofile(path, ...)` and `loadfile(path)` support any `.lua` path inside the computer sandbox (for example `/home`,
  `/bin`, `/etc`, `/lib`).
- `load(source[, chunkname])` compiles in-memory source in the same sandbox (text chunks only).
- `require("module.name")` is library-oriented and searches `/lib` and `/etc/lib` first.
- For allowlisted module names, `require` may also load bundled libs from `/scripts/lib`.
- `require` caches modules in `package.loaded` and blocks recursive self-load loops.

Runtime behavior notes:

- Foreground scripts (`run`) are time-budgeted to prevent runaway execution.
- Background scripts (`runbg`) are limited to a small parallel pool and also time-budgeted.

JSON example:

```lua
payload = {
    fleet = "Alpha",
    command = "recall",
    priority = 2,
    online = true
}

encoded = json.encode(payload)
print(encoded)

decoded = json.decode(encoded)
print(decoded.fleet, decoded.command)
```

### Utility API

Access helper functions through the `util` global variable:

```lua
-- Time helpers
now = util.now()
util.sleep(100)

-- Value helpers
x = util.clamp(42, 0, 10)
r = util.round(3.14159)

-- String helpers
parts = util.split("a,b,c", ",")
joined = util.join(parts, "|")
ok1 = util.startsWith("starmade", "star")
ok2 = util.endsWith("terminal.lua", ".lua")
```

`util` is now loaded from `src/main/resources/scripts/lib/util.lua`, with native `util.now()` and `util.sleep()` attached from Java.

Wrapper usage:

- Use direct `block`/`entity` wrappers for both reads and writes.

### Vector Library

Load vector helpers through the module loader:

```lua
vector = require("vector")
v = vector.new(1, 2, 3)
v:add({ x = 4, y = 5, z = 6 })
length = v:length()
```

Server resource controls (config):

- `script_max_parallel` controls max concurrent scripts per computer.
- `script_timeout_ms` controls foreground/background script timeout.
- `startup_script_timeout_ms` controls startup script timeout.
- `script_overload_mode` controls behavior when at capacity:
    - `0` hard-stop (reject immediately)
    - `1` stall (wait until a slot is available)
    - `2` hybrid (wait up to queue budget, then reject)
- `script_queue_wait_ms` controls hybrid queue wait budget.

### Startup Script

On terminal boot, LuaMade will execute `/etc/startup.lua` if present.

- Use it to print custom startup messages
- Set prompt style with `term.setPromptTemplate(template)`
- Disable automatic prompts for fully custom shells with `term.setAutoPrompt(false)`
- Trigger startup reload in-session with `reboot`

Prompt placeholders:

- `{name}` - Prompt display name
- `{display}` - Saved display name
- `{hostname}` - Network hostname
- `{dir}` - Current working directory

Default startup scripts are bundled under `src/main/resources/scripts` and copied into each computer's virtual filesystem on first boot:

- `scripts/etc/startup.lua` -> `/etc/startup.lua`
- `scripts/bin/*.lua` -> `/bin/*.lua`

Built-in Lua libraries are also bundled under `src/main/resources/scripts/lib` and auto-loaded into the sandbox.

Consolidated wrapper examples:

```lua
block = console.getBlock()
entity = block.getEntity()
print(entity.getName())
print(entity.getSector().x, entity.getSector().y, entity.getSector().z)

block.setActive(true)
block.setDisplayText("Online")

entity.setName("Relay-01")
entity.activateJamming(true)

topBlock = peripheral.getRelative("top")
if topBlock ~= nil then
    print("Top block:", topBlock.getInfo().getName())
end
```

## GitHub Release Automation

This repository now includes a GitHub Actions workflow at [.github/workflows/release-mod.yml](.github/workflows/release-mod.yml) that can:

- Download StarMade compile dependencies from:
    - http://files-origin.star-made.org/build/dev/
- Build the mod jar with Gradle (`clean jar`)
- Publish jar and checksum files to a GitHub Release

How to use:

1. Push a tag like `v2.1.0`, or run the workflow manually via Actions > Release Mod.
2. For manual runs, provide:
     - `tag_name` (required)
     - `release_name` (optional)
     - `starmade_build_url` (optional direct zip URL override)

Notes:

- The workflow auto-discovers a recent StarMade build zip from the dev feed unless `starmade_build_url` is provided.

## Local Codespaces Build With StarMade Dependencies

You can build in Codespaces without manually installing StarMade locally.

When `starmade_root` is left as `STARMADE_PATH`, Gradle now auto-downloads and stages compile dependencies into `.ci/starmade/StarMade/`.

If dev-feed discovery fails in your environment, set `starmade_build_url` in `gradle.properties` (this repo already includes a pinned default).

Examples:

```bash
# Auto-discover newest dev zip from files-origin.star-made.org
./gradlew clean jar

# Force a specific build zip
./gradlew clean jar -Pstarmade_build_url="http://files-origin.star-made.org/build/dev/starmade-build_20260319_094639.zip"

# Use your own existing StarMade directory instead of auto-download
./gradlew clean jar -Pstarmade_root="/path/to/StarMade/"
```

Web request commands:

```text
httpget <url> [output-file]
httpput <url> <payload|@file> [output-file]
httpput --content-type <mime> <url> <payload|@file> [output-file]
```

Examples:

```text
httpget https://raw.githubusercontent.com/user/repo/main/data.txt
httpget https://raw.githubusercontent.com/user/repo/main/data.txt /home/data.txt
httpput https://example.com/api/fleet '{"command":"recall"}'
httpput --content-type application/json https://example.com/api/fleet '{"command":"recall"}'
httpput https://example.com/api/fleet @/home/payload.json /home/put-response.json
```

Server config options:

- `web_fetch_enabled` enables/disables all web fetching (default `false`).
- `web_fetch_trusted_domains_only` restricts fetches to trusted domains only (default `true`).
- `web_fetch_timeout_ms` sets connect/read timeout.
- `web_fetch_max_bytes` sets max response size.
- `web_put_enabled` enables/disables HTTP PUT (default `false`).
- `web_put_trusted_domains_only` restricts PUT to trusted domains only (default `true`).
- `web_put_timeout_ms` sets PUT connect/read timeout.
- `web_put_max_request_bytes` sets max UTF-8 request payload size.
- `web_put_max_response_bytes` sets max response size for PUT responses.

Trusted domain list is configurable in:

```text
config/luamade/trusted_domains.txt
```

Default entries:

- `raw.githubusercontent.com`
- `gist.githubusercontent.com`
- `pastebin.com`
- `hastebin.com`

Example script (/bin/example.lua):
```lua
-- Access command-line arguments
if args[1] then
    print("Hello, " .. args[1] .. "!")
else
    print("Hello, World!")
end

-- Use file system
files = fs.list("/home")
for i, file in ipairs(files) do
    print(file)
end

-- Use networking
net.broadcast("announce", "Script executed!")
```

Run it with: `run /bin/example.lua username`

### Network API
Access the network API through the `net` global variable:
```lua
-- Get hostname
hostname = net.getHostname()

-- Set hostname
net.setHostname("my-computer")

-- Send a message
net.send("other-computer", "chat", "Hello!")

-- Broadcast a message
net.broadcast("announcement", "Server restarting in 5 minutes")

-- Receive a message
message = net.receive("chat")
if message then
    console.print(message.getSender() .. ": " .. message.getContent())
end

-- Check if a message is available
if net.hasMessage("chat") then
    message = net.receive("chat")
    console.print(message.getSender() .. ": " .. message.getContent())
end

-- Join a galaxy-wide channel (optional password)
net.openChannel("trade", "")
net.sendChannel("trade", "", "Anyone selling fuel?")

-- Join a same-sector local broadcast channel
net.openLocalChannel("sector-alert", "")
net.sendLocal("sector-alert", "", "Pirates on scan")

-- Long-range modem: one active link only
net.openModem("secret")
net.connectModem("other-computer", "secret")
net.sendModem("Handshake complete")

-- Transport metadata on received messages
message = net.receiveChannel("trade")
if message then
    print(message.getTransport() .. " via " .. message.getRoute())
end
```

Network model summary:

- Direct messages: hostname to hostname using `send` / `receive`
- Galaxy channels: unique named channels with optional password, many listeners anywhere
- Local channels: unique named channels scoped by current sector at send time, many listeners in-sector
- Long-range modems: explicit 1-to-1 links using `openModem`, `connectModem`, `sendModem`

## Internals
- All Computer File Systems are sandboxed and isolated from each other
- File system data is stored as a single compressed file per computer on disk
- File name format: `computer_<UUID>_fs.smdat`
- Default directory structure includes: /home, /bin, /usr, /etc, /tmp
- Sample scripts are automatically created in /bin on first boot
- Scripts run in a sandboxed Lua environment with limited access to system functions

## Getting Started

When you first interact with a computer in-game:

1. The terminal will start automatically with an interactive UI
2. You can type commands directly in the terminal window - your input appears after the prompt (e.g., `/ $ `)
3. Press Enter to execute your command
4. Type `help` to see available commands
5. Type `cat /home/README.txt` to read the welcome guide
6. Try the example scripts: `run /bin/hello.lua` or `run /bin/shell.lua`
7. Create your own scripts in /home or /bin using the `edit` command

### Interactive Terminal Features

The terminal UI now supports:
- **Real-time typing**: Type commands directly in the terminal window
- **Command execution**: Press Enter to run your command
- **Output display**: See command results immediately in the terminal
- **Command history**: Navigate previous commands and re-run entries with `history` and `!<n>`
- **File editing**: Use commands like `edit`, `cat`, and text-based file manipulation

Example session:
```
/ $ ls /bin
hello.lua
shell.lua

/ $ run /bin/hello.lua World
Hello, World!
Hello, World!

/ $ cd /home
/home $ edit myfile.txt This is my first file!
File written

/home $ cat myfile.txt
This is my first file!
```
