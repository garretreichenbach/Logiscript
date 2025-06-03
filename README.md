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

### Terminal
- Command-line interface with built-in commands
- Command history
- Support for custom commands
- Script execution

### Networking
- Computer-to-computer communication
- Message passing with different protocols
- Hostname management
- Broadcasting capabilities

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
    console.print("Hello, " .. args .. "!")
end)
```

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
- All Computer File Systems are sandboxed and isolated from each other, and their data is stored as a single compressed file per computer on write, with the file name being the computer's ID.