# Terminal API

`term` provides runtime/session helpers in addition to shell commands.

The built-in `pkg` package manager is a shell command (invoked through `runCommand`), not a dedicated `term.*` API method.


## Reference

- `start()`
- `stop()`
- `reboot()`
- `handleInput(input)`
- `registerCommand(name, callback)`
Registers Lua command callback in shell.

- `unregisterCommand(name)`
Removes a command from the shell (built-in or custom).

- `hasCommand(name)`
Checks whether a command currently exists.

- `wrapCommand(name, callback)`
Wraps an existing command with a callback.
Wrapper signature: `callback(args, next)` where `next(nextArgs)` executes original behavior.

- `getPreviousCommand()`
- `getNextCommand()`
- `setCurrentInput(input)`
- `runCommand(commandLine)`
- `isBusy()`
Returns whether scripts are currently executing.

Example:

```lua
term.runCommand("pkg search vector")
```

- `httpGet(url)`
HTTP GET helper (subject to server config/trusted domains).

- `setPromptTemplate(template)`
Placeholders: `{name}`, `{display}`, `{hostname}`, `{dir}`.

- `getPromptTemplate()`
- `resetPromptTemplate()`
- `setAutoPrompt(enabled)`
- `isAutoPromptEnabled()`
- `getScrollMode()`
- `setScrollMode(mode)` where `mode` is one of `NONE`, `HORIZONTAL`, `VERTICAL`, `BOTH`


## Wiring Commands To Game Logic

`registerCommand` callbacks run inside the same Lua environment as your script, so they can directly call:

- `peripheral` to access nearby blocks/modules
- `net` for messaging and coordination
- `fs` for persistence/config
- `console` / `print` for operator feedback

Example: custom command that updates a display block and broadcasts a status event.

```lua
term.registerCommand("status", function(args)
	local text = (args == nil or args == "") and "OK" or args
	local block = peripheral.getSelf()
	if block and block.isDisplayModule and block:isDisplayModule() then
		block:setText("STATUS: " .. text)
	end
	net.sendComputer("status", text)
	print("status set:", text)
end)
```


## Override/Wrap Patterns

Hard override:

```lua
term.registerCommand("reboot", function(args)
	print("Reboot blocked by policy")
end)
```

Wrap existing command:

```lua
term.wrapCommand("rm", function(args, next)
	print("AUDIT rm", args)
	next(args)
end)
```

Remove command:

```lua
term.unregisterCommand("wget")
```
