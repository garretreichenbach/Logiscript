# Terminal

The `Terminal` class provides an interactive command-line interface:

## Built-in Commands

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

## Script Execution

Scripts can be executed by:
1. Directly typing the script name (if in current directory or `/bin`)
2. Using the `run` command with full path
3. Scripts receive arguments in the `args` table

## Sandboxed Environment

Scripts run in a sandboxed Lua environment with:
- **Safe Libraries**: base, string, table, math, package, bit32
- **Disabled Functions**: dofile, loadfile, load (to prevent arbitrary code execution)
- **Exposed APIs**: console, fs, term, net, args

