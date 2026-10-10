# Network Interface API

Networking is provided by the **Network Module** block, accessed through the [peripheral](../systems/peripheral.md) system. Place a Network Module adjacent to a computer and wrap it to obtain the `net` handle.

## Setup

```lua
local nm  = peripheral.wrapRelative("front", "networkmodule")
local net = nm.getNet()
```

```scene
{
  "height": 240,
  "blocks": [
    { "block": "SHIP_CORE", "at": [1, 0, 1] },
    { "block": "videogoose.luamade~Computer", "at": [1, 1, 1] },
    { "block": "videogoose.luamade~Network Module", "at": [2, 1, 1] }
  ],
  "steps": [
    { "text": "Each computer needs its own Network Module touching it." },
    {
      "text": "A second ship sets up the same way. The ships don't need to be docked or touching.",
      "blocks": [
        { "block": "SHIP_CORE", "at": [7, 0, 1] },
        { "block": "videogoose.luamade~Computer", "at": [7, 1, 1] },
        { "block": "videogoose.luamade~Network Module", "at": [6, 1, 1] }
      ]
    },
    { "text": "Give each computer a hostname with net.setHostname, then they can message each other with net.send." }
  ]
}
```

## Typical usage

```lua
local nm  = peripheral.wrapRelative("front", "networkmodule")
local net = nm.getNet()

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
local nm  = peripheral.wrapRelative("front", "networkmodule")
local net = nm.getNet()

local store = net.getDataStore("faction-prices")
if store then
    print(store.getValue("iron_ore"))
    store.set("iron_ore", "200")
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
