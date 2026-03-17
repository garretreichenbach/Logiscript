# Fleet API

`Fleet` provides member management and command dispatch.

## Reference

- `getId()`
- `getFaction()`
- `getName()`
- `getSector()`
- `getFlagship()`
- `getMembers()`
- `addMember(remoteEntity)`
- `removeMember(remoteEntity)`
- `getCurrentCommand()`
- `setCurrentCommand(command, ...)`

## Notes

- `setCurrentCommand` expects command names matching StarMade fleet command enums.
- Additional arguments depend on the selected command type.
