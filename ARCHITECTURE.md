# LuaMade Architecture

## Overview

LuaMade implements a Unix-like sandbox environment for StarMade, inspired by ComputerCraft for Minecraft. Each computer in the game runs its own isolated Lua environment with a virtual file system, terminal, and network interface.

## Core Components

### ComputerModule

The `ComputerModule` class is the main controller for each computer instance. It manages:

- **UUID**: Unique identifier for the computer (based on segment piece position)
- **Console**: Output handling and display
- **FileSystem**: Virtual sandboxed file system
- **Terminal**: Interactive command-line interface
- **NetworkInterface**: Computer-to-computer communication
- **Mode**: Current operating mode (OFF, IDLE, TERMINAL, FILE_EDIT)

### FileSystem

The `FileSystem` class implements a Unix-like virtual file system:

- **VirtualFile**: Wrapper around Java File objects that enforces sandboxing
- **Compression**: File systems are compressed to `.smdat` files on disk
- **Isolation**: Each computer has its own isolated file system
- **Default Structure**: Automatically creates `/home`, `/bin`, `/usr`, `/etc`, `/tmp`
- **Default Files**: Includes example scripts and README

Key Methods:
- `list(path)` - List files in a directory
- `read(path)` - Read file contents
- `write(path, content)` - Write to a file
- `delete(path)` - Delete a file or directory
- `makeDir(path)` - Create a directory
- `changeDir(path)` - Change current working directory
- `getCurrentDir()` - Get current directory
- `exists(path)` - Check if file exists
- `isDir(path)` - Check if path is a directory

### Terminal

The `Terminal` class provides an interactive command-line interface:

#### Built-in Commands

- `ls [dir]` - List files
- `cd <dir>` - Change directory
- `pwd` - Print working directory
- `cat <file>` - Display file contents
- `mkdir <dir>` - Create directory
- `touch <file>` - Create empty file
- `rm <file>` - Delete file
- `cp <src> <dst>` - Copy file
- `mv <src> <dst>` - Move/rename file
- `edit <file> <content>` - Write to file
- `run <script> [args]` - Execute Lua script
- `echo <text>` - Print text
- `clear` - Clear terminal
- `help` - Show commands
- `exit` - Exit terminal

#### Script Execution

Scripts can be executed by:
1. Directly typing the script name (if in current directory or `/bin`)
2. Using the `run` command with full path
3. Scripts receive arguments in the `args` table

#### Sandboxed Environment

Scripts run in a sandboxed Lua environment with:
- **Safe Libraries**: base, string, table, math, package, bit32
- **Disabled Functions**: dofile, loadfile, load (to prevent arbitrary code execution)
- **Exposed APIs**: console, fs, term, net, args

### NetworkInterface

The `NetworkInterface` class enables computer-to-computer communication:

- **Hostname**: Each computer has a unique hostname (default: `computer-<UUID>`)
- **Protocols**: Messages are organized by protocol/channel
- **Message Queues**: Each protocol has its own message queue
- **Broadcasting**: Can broadcast to all computers

Key Methods:
- `getHostname()` - Get this computer's hostname
- `setHostname(name)` - Set a custom hostname
- `send(target, protocol, message)` - Send message to specific computer
- `broadcast(protocol, message)` - Send to all computers
- `receive(protocol)` - Get next message from protocol queue
- `hasMessage(protocol)` - Check if messages are waiting
- `getHostnames()` - List all computers on network
- `ping(hostname)` - Check if computer is reachable

### Console

The `Console` class handles output and printing:

- **Text Contents**: Stores all console output
- **Print Queue**: Asynchronous printing to avoid blocking
- **Color Support**: Can print with different colors
- **Display**: Can show text as in-game overlays

## Sandboxing and Security

### File System Sandboxing

Each computer's file system is completely isolated:
- Files are stored in separate directories per computer
- Virtual files prevent access outside the sandbox
- Path traversal attacks are prevented by normalization
- Maximum file system size can be configured (default: 10MB)

### Lua Sandboxing

Script execution is sandboxed:
- Only safe Lua libraries are loaded
- File system access is restricted to virtual FS
- Network access is limited to in-game computers
- No access to Java classes or system resources
- Cannot load external files or execute system commands

### Resource Limits

- **File System Size**: 10MB per computer (configurable)
- **Script Execution**: Runs in isolated thread
- **Memory**: Limited by Lua VM
- **CPU**: Can be throttled if causing lag

## Data Persistence

### File System Storage

Each computer's file system is:
1. **Loaded on access**: Decompressed from `.smdat` file
2. **Cached in memory**: For quick access during operation
3. **Saved on idle/shutdown**: Compressed back to disk
4. **Format**: `computer_<UUID>_fs.smdat`

### Computer State

Computer state includes:
- Current mode (OFF, IDLE, TERMINAL, FILE_EDIT)
- Last accessed time
- Last open file (if in FILE_EDIT mode)
- Terminal history

## Default File System Structure

When a new computer is initialized, it creates:

```
/
├── home/
│   └── README.txt          # Welcome and usage guide
├── bin/
│   ├── hello.lua           # Hello world example
│   ├── shell.lua           # Shell information
│   ├── chat.lua            # Network chat example
│   └── listall.lua         # Recursive file lister
├── usr/
├── etc/
└── tmp/
```

## Usage Examples

### Basic File Operations

```lua
-- Change to home directory
fs.changeDir("/home")

-- Create a new file
fs.write("myfile.txt", "Hello, World!")

-- Read the file
content = fs.read("myfile.txt")
print(content)

-- List files
files = fs.list(".")
for i, file in ipairs(files) do
    print(file)
end
```

### Terminal Commands

```
/ $ cd /home
/home $ mkdir myproject
/home $ cd myproject
/home/myproject $ edit main.lua print("Hello from my script!")
File written
/home/myproject $ run main.lua
Hello from my script!
```

### Networking

```lua
-- Set a friendly hostname
net.setHostname("my-computer")

-- Send a message to another computer
net.send("other-computer", "chat", "Hello!")

-- Check for messages
if net.hasMessage("chat") then
    msg = net.receive("chat")
    print(msg.getSender() .. " says: " .. msg.getContent())
end

-- Broadcast to everyone
net.broadcast("announce", "Server starting!")
```

### Writing Scripts

Create `/home/monitor.lua`:
```lua
-- Monitor script that checks for network messages
while true do
    if net.hasMessage("alert") then
        msg = net.receive("alert")
        print("[ALERT] " .. msg.getSender() .. ": " .. msg.getContent())
    end
    
    -- Sleep for 5 seconds
    local t = console:getTime()
    while console:getTime() - t < 5000 do end
end
```

Run it: `run /home/monitor.lua`

## Future Enhancements

Potential improvements to consider:

1. **Process Management**: Background processes/daemons
2. **Permissions**: File permissions and ownership
3. **Environment Variables**: Shell variables and configuration
4. **Piping**: Command output piping (cmd1 | cmd2)
5. **Redirection**: Output redirection (cmd > file)
6. **Tab Completion**: Command and path auto-completion
7. **Text Editor**: Built-in text editor for file editing
8. **Package Manager**: Install/manage script packages
9. **Cron Jobs**: Scheduled script execution
10. **Multi-user**: Support for multiple users per computer

## Comparison to ComputerCraft

### Similar Features
- Unix-like terminal and commands
- Lua scripting with sandboxing
- File system with persistence
- Computer-to-computer networking
- Isolated environments per computer

### Differences
- StarMade integration instead of Minecraft
- Compressed file system storage
- Different API surface (StarMade-specific functions)
- Network based on hostnames rather than modem sides
- Built-in terminal mode vs separate programs

## Development Notes

### Adding New Commands

To add a new terminal command:

```java
commands.put("mycommand", new Command("mycommand", "Description") {
    @Override
    public void execute(String args) {
        // Command implementation
        console.print(valueOf("Executing mycommand with: " + args));
    }
});
```

### Adding New APIs

To expose new functionality to Lua scripts:

1. Create a class extending `LuaMadeUserdata`
2. Annotate methods with `@LuaMadeCallable`
3. Add to `createSandboxedGlobals()` in Terminal

```java
globals.set("myapi", new MyCustomAPI());
```

### Testing

Test file system operations, terminal commands, and script execution in isolation before deploying to StarMade. The architecture is designed to be modular and testable.
