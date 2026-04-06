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

## Entity scope (same ship/station)

- `sendEntity(protocol, message)`
- `receiveEntity(protocol)`
- `hasEntityMessage(protocol)`

Delivers protocol messages to other computers on the same entity.

## Computer scope (this computer only)

- `sendComputer(protocol, message)`
- `receiveComputer(protocol)`
- `hasComputerMessage(protocol)`

Useful for coordination between foreground commands and background scripts on one computer.

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

## Networked Data Stores

- `getDataStore(name)`
  Returns a `RemoteDataStore` handle for the named [Networked Data Store](../systems/networked-datastore-block.md),
  or `nil` if the name is not registered. The handle allows reading and writing
  data without the owning entity being loaded.

```lua
local store = net:getDataStore("faction-prices")
if store then
    print(store:getValue("iron_ore"))
    store:set("iron_ore", "200")
end
```

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
