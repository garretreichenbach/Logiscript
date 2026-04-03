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
    which, head, tail, wc, protect, unprotect, fsauth, perms, run, runbg, jobs, kill, httpget, httpput, pkg, nano, name, echo,
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

See the [API documentation](https://garretreichenbach.github.io/Logiscript/) for detailed information on available functions and usage examples.