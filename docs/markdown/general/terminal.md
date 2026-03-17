# Terminal Guide

The terminal is the command shell and script runner for each computer.

## Script execution model

- Run scripts in foreground with `run`.
- Run scripts in background with `runbg`.
- Foreground and background execution are subject to server time and parallel limits.
- Startup script `/etc/startup.lua` is executed on boot when present.

## Key commands

- `help`: list built-in commands.
- `which <name-or-path>`: resolve commands and script paths.
- `ls`, `cd`, `pwd`, `mkdir`, `cat`, `touch`, `rm`, `cp`, `mv`, `edit`.
- `head`, `tail`, `wc` for file inspection.
- `run <script> [args...]` to execute Lua.
- `runbg <script> [args...]` to execute in background.
- `jobs` to list background jobs.
- `kill <jobId>` to stop a background job.
- `nano <file>` to open the file editor pane.
- `httpget <url> [output-file]` to fetch web content (if enabled by server config).
- `reboot` to re-run startup flow and reset prompt behavior.

## Terminal API (`term`)

Inside scripts, the `term` global provides runtime terminal controls.

```lua
term.setPromptTemplate("[{hostname}] {dir} $ ")
term.setAutoPrompt(true)

term.registerCommand("hi", function(args)
  print("hello from custom command")
end)

term.runCommand("hi")
```

### `term` reference

- `registerCommand(name, callback)`
Registers a custom command available in this terminal session.

- `runCommand(commandLine)`
Executes a command line from Lua.

- `isBusy()`
Returns true while scripts are running.

- `httpGet(url)`
Fetches HTTP(S) response body as text, subject to server web-fetch settings.

- `setPromptTemplate(template)`
Sets the shell prompt format.

- `getPromptTemplate()`
Gets the current prompt template.

- `resetPromptTemplate()`
Restores default prompt format.

- `setAutoPrompt(enabled)`
Enables or disables automatic prompt printing.

- `isAutoPromptEnabled()`
Returns current auto-prompt setting.

- `getPreviousCommand()`, `getNextCommand()`
Expose command history navigation helpers.

- `setCurrentInput(text)`
Sets current input buffer text.

## Startup script tips

Use `/etc/startup.lua` for consistent boot behavior:

```lua
print("Boot script starting")
term.setPromptTemplate("{name}@{hostname}:{dir}$ ")
term.setAutoPrompt(true)
```

Prompt placeholders:

- `{name}` prompt display name.
- `{display}` saved display name.
- `{hostname}` network hostname.
- `{dir}` current working directory.

Use this guide for runtime command behavior and script-side terminal control.
