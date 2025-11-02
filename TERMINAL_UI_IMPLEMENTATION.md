# Terminal UI Implementation

## Overview

This document describes the implementation of interactive terminal typing in the LuaMade mod UI, allowing users to type Unix commands and edit files directly through the graphical interface.

## Architecture

### Components

1. **ComputerDialog.java**: The main UI dialog for the computer
   - `ComputerPanel`: Inner class that manages the terminal display
   - `consolePane`: GUIActivatableTextBar for text input/output
   - `currentInputLine`: Tracks the current line being typed by the user

2. **Console.java**: Handles console output and text management
   - `textContents`: StringBuilder that stores all console output
   - `printQueue`: Queue for asynchronous printing
   - `print()`: Appends output to textContents for GUI display

3. **Terminal.java**: Processes terminal commands
   - `handleInput()`: Processes user input and executes commands
   - `printPrompt()`: Displays the command prompt
   - Built-in commands: ls, cd, pwd, cat, mkdir, touch, rm, cp, mv, edit, run, etc.

## Data Flow

### Input Flow (User → Terminal)

1. User types in the terminal window
2. `OnInputChangedCallback.onInputChanged()` is called
   - Extracts the current line being typed (text after the prompt)
   - Stores it in `currentInputLine`
3. User presses Enter
4. `TextCallback.onTextEnter()` or `TabCallback.onEnter()` is called
   - Sends `currentInputLine` to `Terminal.handleInput()`
   - Clears `currentInputLine`
5. Terminal processes the command and generates output

### Output Flow (Terminal → UI)

1. Command execution calls `console.print()`
2. Print thread processes the print queue
   - Appends output to `Console.textContents`
3. GUI's `draw()` method detects changes
   - Compares `computerModule.getLastTextContent()` with current display
   - Updates display via `setTextContent()` if different

## Implementation Details

### Input Tracking

The `currentInputLine` field tracks what the user is currently typing. The `onInputChanged` callback extracts the user's input by:

1. Splitting the full text content into lines
2. Taking the last line (where the user is typing)
3. Finding the prompt marker (" $ ")
4. Extracting text after the prompt

Example:
```
Full text: "LuaMade Terminal v1.0\nType 'help' for commands\n/ $ ls -la"
Last line: "/ $ ls -la"
Extracted input: "ls -la"
```

### Command Execution

When Enter is pressed, the following happens:

1. `Terminal.handleInput(currentInputLine)` is called
2. Terminal parses the command: `command [args]`
3. If it's a built-in command, executes it
4. If it's a file, tries to execute it as a Lua script
5. Otherwise, shows "Unknown command" error
6. Prints a new prompt

### Output Display

The Console's print method:

1. Adds output to a queue (thread-safe)
2. Print thread processes queue every 2 seconds
3. Appends output to `textContents`
4. GUI's draw method picks up changes

### Synchronization

- **Print Queue**: Thread-safe queue for async output
- **StringBuilder**: Not synchronized, but only modified by print thread
- **GUI Updates**: Happen on GUI thread via draw() method

## Key Features Enabled

### 1. Real-time Command Typing

Users can now type commands directly in the terminal window, seeing their input appear character-by-character.

### 2. Command Execution

Press Enter to execute any command:
- Built-in commands: `ls`, `cd`, `pwd`, `cat`, `mkdir`, `rm`, etc.
- Lua scripts: Any file in the current directory or `/bin`
- Custom commands registered via `term.registerCommand()`

### 3. File Editing via Commands

The `edit` command allows file creation and editing:
```
/ $ edit myfile.txt Hello, this is my file content!
File written
/ $ cat myfile.txt
Hello, this is my file content!
```

### 4. Output Display

All command output appears in the terminal:
- Command results
- Error messages
- Script output
- File contents

## Usage Examples

### Basic Commands

```
/ $ pwd
/
/ $ mkdir /home/myproject
Directory created
/ $ cd /home/myproject
/home/myproject $ touch main.lua
File created
/home/myproject $ edit main.lua print("Hello from Lua!")
File written
/home/myproject $ run main.lua
Running script: /home/myproject/main.lua
Hello from Lua!
/home/myproject $
```

### File Operations

```
/ $ cd /home
/home $ edit notes.txt These are my notes
File written
/home $ cat notes.txt
These are my notes
/home $ cp notes.txt backup.txt
File copied
/home $ ls
notes.txt
backup.txt
```

### Script Execution

```
/ $ cat /bin/hello.lua
print("Hello, " .. (args[1] or "World") .. "!")
/ $ run /bin/hello.lua LuaMade
Running script: /bin/hello.lua
Hello, LuaMade!
```

## Future Enhancements

Potential improvements to the terminal UI:

1. **Command History Navigation**: Use up/down arrow keys to navigate history
2. **Tab Completion**: Auto-complete commands and file paths
3. **Cursor Positioning**: Allow editing anywhere in the current line
4. **Multi-line Input**: Support for multi-line commands or scripts
5. **Syntax Highlighting**: Color-code commands, paths, and output
6. **Copy/Paste**: Standard clipboard operations
7. **Selection**: Select and copy text from the terminal
8. **Scrollback Buffer**: Limit stored text and allow scrolling through history

## Testing

Since this is a StarMade mod, testing requires:

1. Load the mod in StarMade
2. Place a computer block in the game
3. Interact with the computer to open the UI
4. Verify:
   - Terminal prompt appears
   - Typing appears in the terminal
   - Enter key executes commands
   - Output appears correctly
   - All built-in commands work
   - Scripts can be run
   - File operations work

## Known Limitations

1. **No Command History Navigation**: Up/down arrows not yet implemented
2. **Single-line Editing**: Can't edit previous lines
3. **No Tab Completion**: Must type full command/path names
4. **Fixed Prompt Position**: Can't edit text before the prompt
5. **Basic Text Editor**: The `edit` command is simple, not a full text editor

## Code Changes Summary

### ComputerDialog.java

- Added `currentInputLine` field to track user input
- Implemented `onInputChanged()` to extract current input from text
- Implemented `onTextEnter()` to send input to Terminal
- Implemented `TabCallback.onEnter()` as backup Enter handler

### Console.java

- Modified `startPrintThread()` to append output to `textContents`
- This fixes the bug where console output wasn't appearing in the UI

### No Changes Required

- Terminal.java: Already had `handleInput()` method
- ComputerModule.java: Already wired up components correctly

## Security Considerations

The implementation maintains all existing security measures:

- **Sandboxed File System**: Users can't access files outside their computer's FS
- **Sandboxed Lua**: Scripts run in restricted environment
- **Input Validation**: Terminal validates and sanitizes all input
- **No Code Injection**: Commands are parsed, not eval'd

## Performance

- **Input Tracking**: Minimal overhead, runs on GUI thread
- **Command Execution**: Synchronous, blocks terminal until complete
- **Output Display**: Asynchronous, queued and batched
- **GUI Updates**: Only when content changes (checked in draw())

## Conclusion

The terminal typing implementation successfully connects the UI to the underlying terminal system, enabling a complete Unix-like interactive experience within the game. Users can now type commands, execute scripts, and manage files all through the graphical terminal interface.
