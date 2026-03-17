-- Long-range modem helper
-- Usage:
--   run /bin/modem.lua listen [password]
--   run /bin/modem.lua dial <hostname> [password]
--   run /bin/modem.lua send <message>
--   run /bin/modem.lua recv
--   run /bin/modem.lua status
--   run /bin/modem.lua hangup

local command = args[1]

if not command then
    print("Usage: modem <listen|dial|send|recv|status|hangup> ...")
    return
end

if command == "listen" then
    local password = args[2] or ""
    if net.openModem(password) then
        print("Modem opened on " .. net.getHostname())
    else
        print("Could not open modem")
    end
elseif command == "dial" then
    local host = args[2]
    local password = args[3] or ""
    if not host then
        print("Usage: modem dial <hostname> [password]")
        return
    end
    if net.connectModem(host, password) then
        print("Connected to modem on " .. host)
    else
        print("Dial failed")
    end
elseif command == "send" then
    local message = args[2]
    if not message then
        print("Usage: modem send <message>")
        return
    end
    if net.sendModem(message) then
        print("Sent modem message")
    else
        print("No modem link active")
    end
elseif command == "recv" then
    local msg = net.receiveModem()
    if msg then
        print("[modem] " .. msg.getSender() .. ": " .. msg.getContent())
    else
        print("No modem messages")
    end
elseif command == "status" then
    if net.isModemConnected() then
        print("Connected to " .. net.getModemPeer())
    else
        print("No active modem connection")
    end
elseif command == "hangup" then
    if net.disconnectModem() then
        print("Modem disconnected")
    else
        print("No active modem connection")
    end
else
    print("Unknown command")
end
