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

### Terminal
- Interactive command-line interface with built-in Unix-like commands
- Command history navigation
- Support for custom Lua commands
- Direct Lua script execution from terminal
- Built-in commands: ls, cd, pwd, cat, mkdir, touch, rm, cp, mv, edit, run, echo, clear, help, exit

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
```

### Terminal API
Access the terminal API through the `term` global variable:
```lua
-- Start the terminal
term.start()

-- Handle user input
term.handleInput("ls")

-- Register a custom command
term.registerCommand("hello", function(args)
    print("Hello, " .. args .. "!")
end)
```

### Terminal Commands
The terminal supports the following built-in commands:

- `ls [directory]` - List files in a directory
- `cd <directory>` - Change current directory
- `pwd` - Print working directory
- `cat <file>` - Display file contents
- `mkdir <directory>` - Create a new directory
- `touch <file>` - Create an empty file
- `rm <file>` - Delete a file or empty directory
- `cp <source> <dest>` - Copy a file
- `mv <source> <dest>` - Move or rename a file
- `edit <file> <content>` - Write content to a file
- `run <script> [args]` - Execute a Lua script with optional arguments
- `echo <text>` - Print text to the terminal
- `clear` - Clear the terminal screen
- `help` - Show available commands
- `exit` - Exit the terminal

### Writing Lua Scripts
Scripts executed in the terminal have access to these global variables:

- `console` - Console API for output
- `print` - Shortcut for console.print()
- `fs` - File system API
- `term` - Terminal API
- `net` - Network API
- `args` - Table of command-line arguments

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
```

## Internals
- All Computer File Systems are sandboxed and isolated from each other
- File system data is stored as a single compressed file per computer on disk
- File name format: `computer_<UUID>_fs.smdat`
- Default directory structure includes: /home, /bin, /usr, /etc, /tmp
- Sample scripts are automatically created in /bin on first boot
- Scripts run in a sandboxed Lua environment with limited access to system functions

## Getting Started

When you first interact with a computer in-game:

1. The terminal will start automatically
2. Type `help` to see available commands
3. Type `cat /home/README.txt` to read the welcome guide
4. Try the example scripts: `run /bin/hello.lua` or `run /bin/shell.lua`
5. Create your own scripts in /home or /bin using the `edit` command

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