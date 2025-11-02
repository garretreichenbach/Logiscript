# Development Notes

## Adding New Commands

To add a new terminal command:

```java
commands.put("mycommand", new Command("mycommand", "Description") {
    @Override
    public void execute(String args) {
        // Command implementation
        console.print(valueOf("Executing mycommand with: " + args));
    }
});
```

## Adding New APIs

To expose new functionality to Lua scripts:

1. Create a class extending `LuaMadeUserdata`
2. Annotate methods with `@LuaMadeCallable`
3. Add to `createSandboxedGlobals()` in Terminal

```java
globals.set("myapi", new MyCustomAPI());
```

## Testing

Test file system operations, terminal commands, and script execution in isolation before deploying to StarMade. The architecture is designed to be modular and testable.

