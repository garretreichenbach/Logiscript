package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.lua.fs.FileSystem;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.LuaValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implements a Unix-like terminal/shell interface for computers in the game.
 * This allows scripts to create a command-line interface for interacting with the computer.
 */
public class Terminal extends LuaMadeUserdata {

	private final ComputerModule module;
	private final Console console;
	private final FileSystem fileSystem;
	private final Map<String, Command> commands = new HashMap<>();
	private final List<String> history = new ArrayList<>();
	private int historyIndex;
	private String currentInput = "";
	private boolean running;

	public Terminal(ComputerModule module, Console console, FileSystem fileSystem) {
		this.module = module;
		this.console = console;
		this.fileSystem = fileSystem;

		// Register built-in commands
		registerBuiltInCommands();
	}

	/**
	 * Starts the terminal
	 */
	@LuaMadeCallable
	public void start() {
		if(running) return;

		running = true;
		console.print(valueOf("LuaMade Terminal v1.0"));
		console.print(valueOf("Type 'help' for a list of commands"));
		printPrompt();
	}

	/**
	 * Stops the terminal
	 */
	@LuaMadeCallable
	public void stop() {
		running = false;
	}

	/**
	 * Handles input from the user
	 */
	@LuaMadeCallable
	public void handleInput(String input) {
		if(!running) {
			return;
		}

		// Add to history
		if(!input.trim().isEmpty()) {
			history.add(input);
			historyIndex = history.size();
		}

		// Process the command
		String[] parts = input.trim().split("\\s+", 2);
		String commandName = parts[0];
		String args = parts.length > 1 ? parts[1] : "";

		if(commandName.isEmpty()) {
			printPrompt();
			return;
		}

		// Execute the command
		Command command = commands.get(commandName);
		if(command != null) {
			command.execute(args);
		} else {
			// Try to execute as a Lua script file
			String scriptPath = fileSystem.normalizePath(commandName);
			if(fileSystem.exists(scriptPath) && !fileSystem.isDir(scriptPath)) {
				String script = fileSystem.read(scriptPath);
				if(script != null) {
					console.print(valueOf("Running script: " + scriptPath));
					// TODO: Execute the script
				} else {
					console.print(valueOf("Error: Could not read script file"));
				}
			} else {
				console.print(valueOf("Unknown command: " + commandName));
			}
		}

		printPrompt();
	}

	/**
	 * Registers a new command
	 */
	@LuaMadeCallable
	public void registerCommand(String name, LuaValue callback) {
		if(name == null || name.isEmpty() || callback == null || !callback.isfunction()) {
			return;
		}

		commands.put(name, new LuaCommand(name, callback));
	}

	/**
	 * Gets the previous command from history
	 */
	@LuaMadeCallable
	public String getPreviousCommand() {
		if(history.isEmpty()) {
			return "";
		}

		if(historyIndex > 0) {
			historyIndex--;
		}

		if(historyIndex < history.size()) {
			return history.get(historyIndex);
		}

		return "";
	}

	/**
	 * Gets the next command from history
	 */
	@LuaMadeCallable
	public String getNextCommand() {
		if(history.isEmpty()) {
			return "";
		}

		if(historyIndex < history.size() - 1) {
			historyIndex++;
			return history.get(historyIndex);
		} else {
			historyIndex = history.size();
			return currentInput;
		}
	}

	/**
	 * Sets the current input
	 */
	@LuaMadeCallable
	public void setCurrentInput(String input) {
		currentInput = input;
	}

	/**
	 * Prints the command prompt
	 */
	private void printPrompt() {
		console.print(valueOf(fileSystem.getCurrentDir() + " $ "));
	}

	/**
	 * Registers the built-in commands
	 */
	private void registerBuiltInCommands() {
		// Help command
		commands.put("help", new Command("help", "Displays a list of available commands") {
			@Override
			public void execute(String args) {
				console.print(valueOf("Available commands:"));
				for(Command command : commands.values()) {
					console.print(valueOf("  " + command.getName() + " - " + command.getDescription()));
				}
			}
		});

		// Echo command
		commands.put("echo", new Command("echo", "Displays a message") {
			@Override
			public void execute(String args) {
				console.print(valueOf(args));
			}
		});

		// Ls command
		commands.put("ls", new Command("ls", "Lists files in a directory") {
			@Override
			public void execute(String args) {
				List<String> files = fileSystem.list(args);
				if(files.isEmpty()) {
					console.print(valueOf("Directory is empty"));
				} else {
					for(String file : files) {
						if(fileSystem.isDir(fileSystem.normalizePath(args + "/" + file))) {
							console.print(valueOf(file + "/"));
						} else {
							console.print(valueOf(file));
						}
					}
				}
			}
		});

		// Cd command
		commands.put("cd", new Command("cd", "Changes the current directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					args = "/";
				}

				if(fileSystem.changeDir(args)) {
					// Success, nothing to print
				} else {
					console.print(valueOf("Error: Directory not found"));
				}
			}
		});

		// Pwd command
		commands.put("pwd", new Command("pwd", "Prints the current working directory") {
			@Override
			public void execute(String args) {
				console.print(valueOf(fileSystem.getCurrentDir()));
			}
		});

		// Mkdir command
		commands.put("mkdir", new Command("mkdir", "Creates a new directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No directory name specified"));
					return;
				}

				if(fileSystem.makeDir(args)) {
					console.print(valueOf("Directory created"));
				} else {
					console.print(valueOf("Error: Could not create directory"));
				}
			}
		});

		// Cat command
		commands.put("cat", new Command("cat", "Displays the contents of a file") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				String content = fileSystem.read(args);
				if(content != null) {
					console.print(valueOf(content));
				} else {
					console.print(valueOf("Error: File not found or is a directory"));
				}
			}
		});

		// Touch command
		commands.put("touch", new Command("touch", "Creates a new empty file") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				if(fileSystem.write(args, "")) {
					console.print(valueOf("File created"));
				} else {
					console.print(valueOf("Error: Could not create file"));
				}
			}
		});

		// Rm command
		commands.put("rm", new Command("rm", "Removes a file or directory") {
			@Override
			public void execute(String args) {
				if(args.isEmpty()) {
					console.print(valueOf("Error: No file specified"));
					return;
				}

				if(fileSystem.delete(args)) {
					console.print(valueOf("File deleted"));
				} else {
					console.print(valueOf("Error: Could not delete file"));
				}
			}
		});

		// Clear command
		commands.put("clear", new Command("clear", "Clears the terminal") {
			@Override
			public void execute(String args) {
				// TODO: Implement clear functionality
				console.print(valueOf("\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n\n"));
			}
		});

		// Exit command
		commands.put("exit", new Command("exit", "Exits the terminal") {
			@Override
			public void execute(String args) {
				stop();
			}
		});
	}

	public String getTextContents() {
		return console.getTextContents();
	}

	/**
	 * Command interface
	 */
	private abstract static class Command {
		private final String name;
		private final String description;

		protected Command(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}

		public abstract void execute(String args);
	}

	/**
	 * Lua command implementation
	 */
	private static class LuaCommand extends Command {
		private final LuaValue callback;

		public LuaCommand(String name, LuaValue callback) {
			super(name, "User-defined command");
			this.callback = callback;
		}

		@Override
		public void execute(String args) {
			callback.call(valueOf(args));
		}
	}
}