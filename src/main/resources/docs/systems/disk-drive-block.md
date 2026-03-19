# Disk Drive Block API

`DiskDriveBlock` extends `InventoryBlock` and represents a LuaMade disk drive with one disk slot.

You usually get this via `peripheral.wrapRelative(side, "diskdrive")` or by wrapping a block as `diskdrive`.

## Typical usage

```lua
local drive = peripheral.wrapRelative("right", "diskdrive")
if drive == nil or not drive.hasDisk() then
  print("Insert a disk first")
  return
end

-- Save local script onto disk
print("Saved:", drive.saveProgram("/bin/chat.lua", "chat"))

-- List available programs on the disk
for _, name in ipairs(drive.listPrograms()) do
  print("Program:", name, "Price:", drive.getProgramPrice(name) or 0)
end

-- Install script from disk into local filesystem
print("Installed:", drive.installProgram("chat", "/home/chat.lua"))

-- Publish program for sale
print("For sale:", drive.sellProgram("/home/chat.lua", "chat_pro", 5000))

-- Buy/install and get listed price
local paid = drive.buyProgram("chat_pro", "/home/chat_pro.lua")
print("Paid:", paid)
```

## Reference

- `hasDisk()`
Returns `true` when a valid LuaMade disk item is inserted.

- `getDisk()`
Returns the inserted `ItemStack`, or `nil`.

- `getDiskKey()`
Returns internal disk identity key, or `nil`.

- `listPrograms()`
Returns program names stored on disk under `/programs/*.lua`.

- `saveProgram(sourcePath, [programName])`
Reads a local file from `fs` and stores it on disk as a program.

- `installProgram(programName, destinationPath)`
Loads a disk program and writes it to local `fs`.

- `sellProgram(sourcePath, [programName], price)`
Saves program and records an integer sale price.

- `getProgramPrice(programName)`
Returns recorded program price or `nil`.

- `buyProgram(programName, destinationPath)`
Installs the program and returns price (defaults to `0` if unpriced).

- `write(path, content)`
Writes arbitrary UTF-8 text directly to disk path.

- `read(path)`
Reads arbitrary text from disk path.

## Notes

- Program sale price storage is metadata only; credit transfer/economy integration is not automatic yet.
- Disk identity persistence uses StarMade inventory metadata APIs when available at runtime.
- If metadata APIs are unavailable, data falls back to drive-slot scoped storage.
