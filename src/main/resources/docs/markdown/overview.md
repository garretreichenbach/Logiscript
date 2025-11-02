# Overview

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

