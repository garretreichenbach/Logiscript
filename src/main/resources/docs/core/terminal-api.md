# Terminal API

`term` provides runtime/session helpers in addition to shell commands.


## Reference

- `start()`
- `stop()`
- `reboot()`
- `handleInput(input)`
- `registerCommand(name, callback)`
Registers Lua command callback in shell.

- `getPreviousCommand()`
- `getNextCommand()`
- `setCurrentInput(input)`
- `runCommand(commandLine)`
- `isBusy()`
Returns whether scripts are currently executing.

- `httpGet(url)`
HTTP GET helper (subject to server config/trusted domains).

- `setPromptTemplate(template)`
Placeholders: `{name}`, `{display}`, `{hostname}`, `{dir}`.

- `getPromptTemplate()`
- `resetPromptTemplate()`
- `setAutoPrompt(enabled)`
- `isAutoPromptEnabled()`
