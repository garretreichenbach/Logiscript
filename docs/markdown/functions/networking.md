# Network Interface API

`net` provides direct messaging plus channel/local/modem transports.

## Typical usage

```lua
net.setHostname("miner-01")

net.send("miner-02", "job", "start")
if net.hasMessage("status") then
  local msg = net.receive("status")
  print(msg.getSender(), msg.getContent())
end

net.openChannel("ops", "")
net.sendChannel("ops", "", "hello channel")
```

## Direct messaging

- `setHostname(name)`
- `getHostname()`
- `send(targetHostname, protocol, message)`
- `receive(protocol)`
- `hasMessage(protocol)`
- `broadcast(protocol, message)`

## Channels

- `openChannel(channelName, password)`
- `closeChannel(channelName)`
- `sendChannel(channelName, password, message)`
- `receiveChannel(channelName)`
- `hasChannelMessage(channelName)`

## Local channels (same sector)

- `openLocalChannel(channelName, password)`
- `closeLocalChannel(channelName)`
- `sendLocal(channelName, password, message)`
- `receiveLocal(channelName)`
- `hasLocalMessage(channelName)`

## Modem (1:1 link)

- `openModem(password)`
- `closeModem()`
- `connectModem(targetHostname, password)`
- `disconnectModem()`
- `isModemConnected()`
- `getModemPeer()`
- `sendModem(message)`
- `receiveModem()`
- `hasModemMessage()`

## Discovery helpers

- `getHostnames()`
- `getCurrentSector()`
- `isHostnameAvailable(name)`
- `ping(targetHostname)`

## Message object

Objects returned by `receive*` methods expose:

- `getSender()`
- `getContent()`
- `getRoute()`
- `getTransport()`
- `getTimestamp()`
