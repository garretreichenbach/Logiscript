-- LuaMade Terminal Example
-- This demonstrates using the Unix-like terminal environment

-- Start the terminal
term.start()

-- Wait a bit for terminal to initialize
local function sleep(n)
    local t0 = console:getTime()
    while console:getTime() - t0 <= n * 1000 do end
end

sleep(0.5)

-- Create some example files and directories
print("Creating directory structure...")
term.handleInput("mkdir /home/examples")
sleep(0.2)

print("Writing example files...")
term.handleInput("edit /home/examples/test.txt Hello from the terminal!")
sleep(0.2)

print("Listing files...")
term.handleInput("ls /home")
sleep(0.2)

print("Reading file...")
term.handleInput("cat /home/examples/test.txt")
sleep(0.2)

-- Demonstrate networking
print("\nNetwork example:")
print("Current hostname: " .. net.getHostname())

-- Show available computers
local hosts = net.getHostnames()
print("Found " .. #hosts .. " computers on the network")

-- Register a custom command
print("\nRegistering custom command 'greet'...")
term.registerCommand("greet", function(args)
    print("Greetings, " .. (args or "stranger") .. "!")
end)

sleep(0.2)
term.handleInput("greet User")
sleep(0.2)

-- Run a script
print("\nRunning hello.lua script...")
term.handleInput("run /bin/hello.lua TestUser")

print("\nExample complete! Type 'help' to see all available commands.")
