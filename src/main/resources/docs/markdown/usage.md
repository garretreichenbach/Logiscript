# Usage Examples

## Basic File Operations

```lua
-- Change to home directory
fs.changeDir("/home")

-- Create a new file
fs.write("myfile.txt", "Hello, World!")

-- Read the file
content = fs.read("myfile.txt")
print(content)

-- List files
files = fs.list(".")
for i, file in ipairs(files) do
    print(file)
end
```

## Terminal Commands

```
/ $ cd /home
/home $ mkdir myproject
/home $ cd myproject
/home/myproject $ edit main.lua print("Hello from my script!")
File written
/home/myproject $ run main.lua
Hello from my script!
```

## Networking

```lua
-- Set a friendly hostname
net.setHostname("my-computer")

-- Send a message to another computer
net.send("other-computer", "chat", "Hello!")

-- Check for messages
if net.hasMessage("chat") then
    msg = net.receive("chat")
    print(msg.getSender() .. " says: " .. msg.getContent())
end

-- Broadcast to everyone
net.broadcast("announce", "Server starting!")
```

## Writing Scripts

Create `/home/monitor.lua`:
```lua
-- Monitor script that checks for network messages
while true do
    if net.hasMessage("alert") then
        msg = net.receive("alert")
        print("[ALERT] " .. msg.getSender() .. ": " .. msg.getContent())
    end
    
    -- Sleep for 5 seconds
    local t = console:getTime()
    while console:getTime() - t < 5000 do end
end
```

Run it: `run /home/monitor.lua`

