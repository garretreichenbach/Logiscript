-- Galaxy-wide or local channel chat helper
-- Usage:
--   run /bin/channel_chat.lua open <channel> [password]
--   run /bin/channel_chat.lua send <channel> <message> [password]
--   run /bin/channel_chat.lua sendlocal <channel> <message> [password]
--   run /bin/channel_chat.lua recv <channel>
--   run /bin/channel_chat.lua recvlocal <channel>

local command = args[1]
local channel = args[2]

if not command or not channel then
    print("Usage: channel_chat <open|send|sendlocal|recv|recvlocal> <channel> ...")
    return
end

if command == "open" then
    local password = args[3] or ""
    if net.openChannel(channel, password) then
        print("Joined channel '" .. channel .. "'")
    else
        print("Failed to join channel")
    end
elseif command == "send" then
    local message = args[3]
    local password = args[4] or ""
    if not message then
        print("Usage: channel_chat send <channel> <message> [password]")
        return
    end
    if net.sendChannel(channel, password, message) then
        print("Sent to channel '" .. channel .. "'")
    else
        print("Channel send failed")
    end
elseif command == "sendlocal" then
    local message = args[3]
    local password = args[4] or ""
    if not message then
        print("Usage: channel_chat sendlocal <channel> <message> [password]")
        return
    end
    if net.sendLocal(channel, password, message) then
        print("Sent local sector broadcast on '" .. channel .. "'")
    else
        print("Local send failed")
    end
elseif command == "recv" then
    local msg = net.receiveChannel(channel)
    if msg then
        print("[channel:" .. msg.getRoute() .. "] " .. msg.getSender() .. ": " .. msg.getContent())
    else
        print("No channel messages")
    end
elseif command == "recvlocal" then
    local msg = net.receiveLocal(channel)
    if msg then
        print("[local:" .. msg.getRoute() .. "] " .. msg.getSender() .. ": " .. msg.getContent())
    else
        print("No local messages")
    end
else
    print("Unknown command")
end
