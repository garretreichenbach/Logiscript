-- Simple chat client
-- Requires a Network Module block adjacent to this computer.
-- Usage: run /bin/chat.lua <target_hostname> <message>

local nm = peripheral.wrapRelative("front", "networkmodule")
if not nm then
    print("No Network Module found adjacent to this computer")
    return
end
local net = nm.getNet()

local target = args[1]
local message = args[2] or "Hello!"

if not target then
    print("Usage: chat <target_hostname> <message>")
    print("Your hostname: " .. net.getHostname())
    print("\nAvailable computers:")
    local hosts = net.getHostnames()
    for i = 1, #hosts do
        if hosts[i] ~= net.getHostname() then
            print("  " .. hosts[i])
        end
    end
else
    if net.send(target, "chat", message) then
        print("Message sent to " .. target)
    else
        print("Failed to send message. Computer not found.")
    end
end
