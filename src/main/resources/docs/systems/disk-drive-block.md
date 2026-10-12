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

-- Label the disk (shows in inventories)
drive.setDiskName("Chat Installer")

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

-- Charge the player into an adjacent vault, then install
local till = peripheral.wrapRelative("left", "vault")
local paid = drive.buyProgram("chat_pro", "/home/chat_pro.lua", till)
print(paid and ("Paid: " .. paid) or "Purchase cancelled")
```

## Reference

- `hasDisk()`
Returns `true` when a valid LuaMade disk item is inserted.

- `getDisk()`
Returns the inserted `ItemStack`, or `nil`.

- `getDiskKey()`
Returns internal disk identity key, or `nil`.

- `getDiskName()` / `setDiskName(name)`
Reads or sets the inserted disk's display name (the vanilla `name` custom data key). Pass `nil` or `""` to reset it to "Disk".

- `getDiskData()` / `setDiskData(json)`
Reads or replaces the inserted disk's item custom data as a JSON string. The disk's `luamadeDiskId` is always kept, so its files stay with it.

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

- `buyProgram(programName, destinationPath, [vault])`
Charges the player who ran the script the program's price into `vault` (a [Vault](vault.md) peripheral), then installs it. They see an OK/Cancel dialog first. Returns the price paid (`0` for free programs, which need no vault), or `nil` if they cancelled or the program doesn't exist. If installing fails after payment, the payment is refunded. Throws if a priced program has no vault, or the payment is refused.

- `write(path, content)`
Writes arbitrary UTF-8 text directly to disk path.

- `read(path)`
Reads arbitrary text from disk path.

## Notes

- Prices are stored on the disk. Payment only happens through `buyProgram` with a vault; `installProgram` is always free.
- A disk's files are tied to an ID stored in its item data, so they follow the disk between drives.
