-- Simple chat client
-- Usage: run /bin/chat.lua <target_hostname> <message>

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
