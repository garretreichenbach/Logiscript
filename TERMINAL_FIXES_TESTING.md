# Terminal Fixes Testing Guide

This document describes how to test the three terminal improvements implemented in this PR.

## Changes Implemented

1. **Prompt Protection**: Users cannot delete the starting prompt characters (e.g., "/ $ ")
2. **Input Persistence**: Console input is saved and restored when closing/reopening the UI
3. **Command Output Display**: Commands like "help" now properly display their output

## Testing Scenarios

### Test 1: Prompt Character Protection

**Objective**: Verify that the prompt characters cannot be deleted by the user.

**Steps**:
1. Load the mod in StarMade
2. Place and interact with a computer block to open the terminal UI
3. Wait for the prompt to appear (e.g., "/ $ ")
4. Try to delete the prompt using backspace:
   - Place cursor after the "$" and press backspace multiple times
   - Try to select and delete the prompt characters
   - Try to delete the entire line

**Expected Result**:
- The prompt characters should NOT be deletable
- When deletion is attempted, you should hear an error beep sound (audio feedback)
- The text should be automatically restored to include the prompt
- Only text AFTER the "$ " should be deletable

**Failure Indicators**:
- Prompt characters can be deleted
- No audio feedback when attempting to delete prompt
- Text restoration doesn't work

---

### Test 2: Input Persistence Across UI Sessions

**Objective**: Verify that partially typed input is saved when closing the UI and restored when reopening.

**Steps**:
1. Open the computer terminal UI
2. Type some text but DO NOT press Enter (e.g., type "help" but don't execute)
3. Close the terminal UI (click X or press ESC)
4. Reopen the same computer's terminal UI

**Expected Result**:
- The text you typed ("help") should still be present in the input line
- The text should appear after the prompt (e.g., "/ $ help")
- The cursor should be at the end of the restored text

**Additional Tests**:
- Type "ls -la", close UI, reopen → "ls -la" should still be there
- Type nothing, close UI, reopen → prompt should be empty (no extra text)
- Execute a command, close UI, reopen → input should be empty (not the executed command)

**Failure Indicators**:
- Typed text disappears when UI is closed
- Text is not restored when UI is reopened
- Executed commands reappear as input when UI is reopened

---

### Test 3: Command Output Display

**Objective**: Verify that running commands displays their output correctly instead of just clearing the input.

**Steps**:
1. Open the computer terminal UI
2. Type "help" and press Enter
3. Observe the terminal display

**Expected Result**:
- The "help" command should be cleared from the input line
- The terminal should display:
  - The list of available commands
  - Each command with its description
  - A new prompt line at the bottom
- The output should NOT just show an indented blank line

**Additional Command Tests**:
- `ls` → Should show list of files/directories
- `pwd` → Should show current directory path
- `echo test message` → Should display "test message"
- `mkdir testdir` → Should show "Directory created" message
- Unknown command → Should show "Unknown command: [name]" error

**Failure Indicators**:
- Commands clear the input but show no output
- Only indentation or blank lines appear
- Output is not visible in the terminal

---

## Implementation Details

### Prompt Protection Mechanism
The `onInputChanged` callback detects when the prompt marker (" $ ") is missing from the last line. When deletion is detected:
1. Audio feedback is played (`queueUIAudio("0022_menu_back")`)
2. Text is restored asynchronously after 10ms delay
3. Previous valid content plus any user input is restored

### Input Persistence Mechanism
Input is saved at multiple points:
1. Continuously during `draw()` method execution
2. When dialog is deactivated via `onDeactivate()`
3. Stored in `ComputerModule.savedTerminalInput` field
4. Restored during `onInit()` when dialog is initialized

### Command Output Display Mechanism
When Enter is pressed:
1. Current input is saved before clearing
2. Terminal command is executed (generates output)
3. Saved input is cleared (prevents restoration)
4. `userIsTyping` flag is set to false
5. Text field is immediately updated with new terminal content
6. New content includes command output and new prompt

## Regression Testing

Ensure existing functionality still works:

1. **Command Execution**:
   - All built-in commands (ls, cd, pwd, cat, mkdir, rm, cp, mv, edit, run, etc.) still work
   - Lua script execution still works
   
2. **Text Display**:
   - Terminal output appears correctly
   - Multiple lines display properly
   - Long output doesn't cause issues

3. **Input Handling**:
   - Typing still works normally
   - Enter key executes commands
   - Tab key behavior unchanged

## Known Limitations

1. **Prompt Position Tracking**: The `promptStartPosition` variable is calculated but not currently used for cursor restriction. Future improvement could prevent cursor movement before the prompt.

2. **Multi-line Prompts**: The implementation assumes single-line prompts. If the terminal ever uses multi-line prompts, the logic may need adjustment.

3. **Async Text Restoration**: Uses a 10ms delay and separate thread for text restoration to avoid recursion. This is a workaround for UI framework limitations. While functional, a more integrated solution using the UI framework's scheduling mechanisms would be preferable.

4. **Session-Only Persistence**: The `savedTerminalInput` is persisted only within a game session (when closing/reopening the UI). It is not saved to disk and will be lost when the application restarts. For permanent persistence, integration with the FileSystem's save/load mechanism would be needed.

## Success Criteria

All three test scenarios should pass:
- ✅ Prompt characters are protected from deletion
- ✅ Input is persisted across UI close/open cycles
- ✅ Command output is properly displayed

If any test fails, review the relevant section of code:
- Prompt protection → `onInputChanged` callback in `ComputerDialog.java`
- Input persistence → `draw()`, `onDeactivate()`, `onInit()`, and `ComputerModule` save/get methods
- Command output → `executeCurrentInput()` method in `ComputerDialog.java`
