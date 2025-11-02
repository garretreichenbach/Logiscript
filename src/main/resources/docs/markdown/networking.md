# Networking

The `NetworkInterface` class enables computer-to-computer communication:

- **Hostname**: Each computer has a unique hostname (default: `computer-<UUID>`)
- **Protocols**: Messages are organized by protocol/channel
- **Message Queues**: Each protocol has its own message queue
- **Broadcasting**: Can broadcast to all computers

Key Methods:
- `getHostname()` - Get this computer's hostname
- `setHostname(name)` - Set a custom hostname
- `send(target, protocol, message)` - Send message to specific computer
- `broadcast(protocol, message)` - Send to all computers
- `receive(protocol)` - Get next message from protocol queue
- `hasMessage(protocol)` - Check if messages are waiting
- `getHostnames()` - List all computers on network
- `ping(hostname)` - Check if computer is reachable

