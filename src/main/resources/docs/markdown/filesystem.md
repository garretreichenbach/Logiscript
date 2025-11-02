│   ├── shell.lua           # Shell information
│   ├── chat.lua            # Network chat example
│   └── listall.lua         # Recursive file lister
├── usr/
├── etc/
└── tmp/
```
# File System

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

## Sandboxing and Security

### File System Sandboxing

Each computer's file system is completely isolated:
- Files are stored in separate directories per computer
- Virtual files prevent access outside the sandbox
- Path traversal attacks are prevented by normalization
- Maximum file system size can be configured (default: 10MB)

### Data Persistence

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

