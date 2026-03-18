# Network Usage Guide

LuaMade networking is exposed through the `net` global.

## Message types

- Direct messages: one sender to one hostname.
- Global channels: named pub/sub across all sectors.
- Local channels: named pub/sub within the same sector only.
- Modem link: explicit 1-to-1 long-range connection.

## Direct messaging

```lua
net.send("relay-1", "chat", "hello")

if net.hasMessage("chat") then
  msg = net.receive("chat")
  print("From", msg.getSender(), msg.getData())
end
```

## Global channels

```lua
net.openChannel("trade", "pw123")
net.sendChannel("trade", "pw123", "uranium wanted")

if net.hasChannelMessage("trade") then
  msg = net.receiveChannel("trade")
  print("[trade]", msg.getSender(), msg.getData())
end
```

## Local sector channels

```lua
net.openLocalChannel("dockyard", "localpw")
net.sendLocal("dockyard", "localpw", "pad clear")

if net.hasLocalMessage("dockyard") then
  msg = net.receiveLocal("dockyard")
  print("[local]", msg.getSender(), msg.getData())
end
```

## Modem links

```lua
-- host machine
net.openModem("linkpw")

-- remote machine
net.connectModem("host-01", "linkpw")
net.sendModem("ping")

if net.hasModemMessage() then
  msg = net.receiveModem()
  print("[modem]", msg.getSender(), msg.getData())
end
```

## Hostname and discovery

```lua
ok = net.setHostname("miner-02")
print("hostname:", net.getHostname())

for _, host in ipairs(net.getHostnames()) do
  print(host, net.ping(host))
end
```

## Reference

- Direct: `send`, `receive`, `hasMessage`, `broadcast`.
- Global channel: `openChannel`, `closeChannel`, `sendChannel`, `receiveChannel`, `hasChannelMessage`.
- Local channel: `openLocalChannel`, `closeLocalChannel`, `sendLocal`, `receiveLocal`, `hasLocalMessage`.
- Modem: `openModem`, `closeModem`, `connectModem`, `disconnectModem`, `isModemConnected`, `getModemPeer`, `sendModem`, `receiveModem`, `hasModemMessage`.
- Network utility: `setHostname`, `getHostname`, `getHostnames`, `getCurrentSector`, `isHostnameAvailable`, `ping`.

Use this guide as the primary reference for message routing patterns in scripts.
