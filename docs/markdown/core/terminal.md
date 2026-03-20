# Terminal Guide

The terminal is the command shell and script runner for each computer.

## Script execution model

- Run scripts in foreground with `run`.
- Run scripts in background with `runbg`.
- Foreground and background execution are subject to server time and parallel limits.
- Startup script `/etc/startup.lua` is executed on boot when present.

## Keyboard suggestions

- If the first command token is non-empty and you pause typing for about 3 seconds, the terminal shows command
  suggestions.
- Suggestions include built-in shell commands and runnable scripts from `/bin`.
- Press `Tab` once to autocomplete to the best match.
- Press `Tab` again to cycle forward through suggestions (bash-style).
- `Up` / `Down` always navigate command history regardless of suggestion state.
- Typing any character dismisses the current suggestion list.

## Key commands

- `help`: list built-in commands.
- `which [-a] <name-or-path>`: resolve commands and script paths (`-a` shows all matches).
- `ls [-a] [-l] [-R] [path]`, `cd <directory>`, `pwd [-L|-P]`, `mkdir [-p] <directory>...`, `cat [-n] [-A] <file>...`,
  `touch <file>`.
- `rm [-r] [-f] <path>...`, `cp [-r] <source> <destination>`, `mv <source> <destination>`, `edit <file> <content>`.
- `find [path] [-name <glob>] [-type f|d] [-maxdepth <n>]` for recursive search.
- `grep [-n] [-i] [-r] <pattern> <path>` for content search.
- `history` to list indexed commands and `!<n>` to re-run a history entry.
- `stat <path>...` for file/directory metadata.
- `tree [-a] [-L depth] [path]` for directory visualization.
- `head [-n lines] <file>`, `tail [-n lines] <file>`, `wc [-l] [-w] [-c] <file>...` for file inspection.
- `echo [-n] <text>` for output (`-n` suppresses trailing newline).
- `protect <path> <password> [ops]` to protect paths by operation scope.
- `unprotect <path> <password>` to remove path protection.
- `fsauth <password>` and `fsauth --clear` to manage filesystem auth session.
- `perms [path]` to inspect configured protection rules.
- `run <script> [args...]` to execute Lua.
- `runbg <script> [args...]` to execute in background.
- `jobs` to list background jobs.
- `kill [-TERM|-KILL|-INT|-HUP|-15|-9|-2|-1] <job-id>` to stop a background job.
- `nano <file>` to open the file editor pane.
- `httpget <url> [output-file]` to fetch web content (if enabled by server config).
- `httpput [--content-type <mime>] <url> <payload|@file> [output-file]` to send web content via HTTP PUT.
- `reboot` to re-run startup flow and reset prompt behavior.

If trusted-only mode is enabled, allowed domains come from:

```text
config/luamade/trusted_domains.txt
```

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

- `httpPut(url, body)` / `httpPut(url, body, contentType)`
  Sends HTTP(S) PUT request body and returns response text, subject to server web-put settings.

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

## Filesystem protection workflow

```text
/ $ protect /home secret123 all
Protection enabled for /home
/ $ cat /home/README.txt
Error: Permission denied: read requires password for /home
/ $ fsauth secret123
Filesystem scopes unlocked
/ $ cat /home/README.txt
...file contents...
/ $ fsauth --clear
Cleared filesystem auth session
```

Operation scopes for `protect` are `read`, `write`, `delete`, `list`, `all`, plus aliases `copy`, `move`, `paste`, `rw`.

