package luamade.lua.element.block;

import luamade.element.ElementRegistry;
import luamade.lua.disk.DiskDataStore;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.element.inventory.ItemStack;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.lua.vault.Vault;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.LuaError;
import luamade.utils.CustomDataUtils;
import org.json.JSONObject;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.inventory.InventorySlot;

import java.util.Collections;
import java.util.List;

@LuaMadeClass("DiskDrive")
public class DiskDriveBlock extends InventoryBlock {

	private final ComputerModule module;

	public DiskDriveBlock(SegmentPiece piece, ComputerModule module) {
		super(piece);
		this.module = module;
	}

	@LuaMadeCallable
	public Boolean hasDisk() {
		return getInsertedDiskSlot() != null;
	}

	@LuaMadeCallable
	public ItemStack getDisk() {
		InventorySlot slot = getInsertedDiskSlot();
		if(slot == null) {
			return null;
		}
		return new ItemStack(slot.getType(), slot.count());
	}

	@LuaMadeCallable
	public String getDiskKey() {
		return resolveDiskKey();
	}

	@LuaMadeCallable
	public String getDiskData() {
		Integer index = getInsertedDiskSlotIndex();
		return index == null ? null : getInventory().getSlotData(index);
	}

	@LuaMadeCallable
	public Boolean setDiskData(String json) {
		Integer index = getInsertedDiskSlotIndex();
		return index != null && getInventory().setSlotData(index, json);
	}

	/** Vanilla shows the custom data "name" key as the item's display name. */
	@LuaMadeCallable
	public String getDiskName() {
		InventorySlot slot = getInsertedDiskSlot();
		if(slot == null || !slot.hasCustomData()) {
			return null;
		}
		return slot.getCustomData().optString("name", null);
	}

	@LuaMadeCallable
	public Boolean setDiskName(String name) {
		Integer index = getInsertedDiskSlotIndex();
		if(index == null) {
			return false;
		}
		InventorySlot slot = getInventory().getSlotAt(index);
		JSONObject data = CustomDataUtils.copy(slot.hasCustomData() ? slot.getCustomData() : null);
		if(name == null || name.trim().isEmpty()) {
			data.remove("name");
		} else {
			data.put("name", name.trim());
		}
		return getInventory().setSlotData(index, data.toString());
	}

	@LuaMadeCallable
	public List<String> listPrograms() {
		String diskKey = resolveDiskKey();
		if(diskKey == null) {
			return Collections.emptyList();
		}
		return DiskDataStore.listPrograms(diskKey);
	}

	@LuaMadeCallable
	public Boolean saveProgram(String sourcePath, String programName) {
		if(module == null || sourcePath == null || sourcePath.trim().isEmpty()) {
			return false;
		}

		String diskKey = resolveDiskKey();
		if(diskKey == null) {
			return false;
		}

		String normalizedSource = module.getFileSystem().normalizePath(sourcePath);
		String sourceCode = module.getFileSystem().read(normalizedSource);
		if(sourceCode == null) {
			return false;
		}

		String normalizedProgramName = deriveProgramName(programName, normalizedSource);
		return DiskDataStore.saveProgram(diskKey, normalizedProgramName, sourceCode);
	}

	@LuaMadeCallable
	public Boolean installProgram(String programName, String destinationPath) {
		if(module == null || destinationPath == null || destinationPath.trim().isEmpty()) {
			return false;
		}

		String diskKey = resolveDiskKey();
		if(diskKey == null) {
			return false;
		}

		String sourceCode = DiskDataStore.loadProgram(diskKey, programName);
		if(sourceCode == null) {
			return false;
		}

		String normalizedDestination = module.getFileSystem().normalizePath(destinationPath);
		return module.getFileSystem().write(normalizedDestination, sourceCode);
	}

	@LuaMadeCallable
	public Boolean sellProgram(String sourcePath, String programName, Integer price) {
		int normalizedPrice = price == null ? 0 : Math.max(0, price);
		if(!saveProgram(sourcePath, programName)) {
			return false;
		}

		String diskKey = resolveDiskKey();
		if(diskKey == null) {
			return false;
		}

		String normalizedProgramName = deriveProgramName(programName, sourcePath);
		return DiskDataStore.setProgramPrice(diskKey, normalizedProgramName, normalizedPrice);
	}

	@LuaMadeCallable
	public Integer getProgramPrice(String programName) {
		String diskKey = resolveDiskKey();
		if(diskKey == null) {
			return null;
		}
		return DiskDataStore.getProgramPrice(diskKey, programName);
	}

	/**
	 * Charges the invoking player the program's price into {@code vault}, then installs it. Free programs need no vault.
	 * Returns the price paid, or nil if the player cancelled or the program couldn't be installed (any payment is refunded).
	 */
	@LuaMadeCallable
	public Integer buyProgram(String programName, String destinationPath) {
		return buyProgram(programName, destinationPath, null);
	}

	@LuaMadeCallable
	public Integer buyProgram(String programName, String destinationPath, VaultBlock vault) {
		String diskKey = resolveDiskKey();
		if(diskKey == null || DiskDataStore.loadProgram(diskKey, programName) == null) {
			return null;
		}
		Integer price = getProgramPrice(programName);
		int amount = price == null ? 0 : price;
		if(amount == 0) {
			return installProgram(programName, destinationPath) ? 0 : null;
		}
		if(vault == null) {
			throw new LuaError("Program costs " + amount + " credits; pass a vault to receive payment");
		}
		SegmentPiece vaultPiece = vault.requireLiveSegmentPiece();
		String reason = "Buy program '" + programName + "'";
		if(!Vault.requestPayment(vaultPiece, (long) amount, reason)) {
			return null;
		}
		if(!installProgram(programName, destinationPath)) {
			Vault.refund(vaultPiece, amount, reason + " (install failed)");
			return null;
		}
		return amount;
	}

	@LuaMadeCallable
	public Boolean write(String path, String content) {
		String diskKey = resolveDiskKey();
		if(diskKey == null || path == null || path.trim().isEmpty()) {
			return false;
		}
		return DiskDataStore.writeText(diskKey, path, content == null ? "" : content);
	}

	@LuaMadeCallable
	public String read(String path) {
		String diskKey = resolveDiskKey();
		if(diskKey == null || path == null || path.trim().isEmpty()) {
			return null;
		}
		return DiskDataStore.readText(diskKey, path);
	}

	private InventorySlot getInsertedDiskSlot() {
		Integer index = getInsertedDiskSlotIndex();
		return index == null ? null : getInventory().getSlotAt(index);
	}

	private Integer getInsertedDiskSlotIndex() {
		Inventory inventory = getInventory();
		if(inventory == null || inventory.getBackingInventory() == null) {
			return null;
		}

		for(int i : inventory.getBackingInventory().getSlots()) {
			InventorySlot slot = inventory.getSlotAt(i);
			if(slot != null && slot.count() > 0 && slot.getType() == ElementRegistry.DISK.getId()) {
				return i;
			}
		}
		return null;
	}

	private String resolveDiskKey() {
		Integer index = getInsertedDiskSlotIndex();
		if(index == null) {
			return null;
		}
		InventorySlot slot = getInventory().getSlotAt(index);
		String before = slot.hasCustomData() ? slot.getCustomData().toString() : null;
		String key = DiskDataStore.resolveDiskKey(slot, getSegmentPiece());
		if(slot.hasCustomData() && !slot.getCustomData().toString().equals(before)) {
			// a fresh disk just got its ID; sync it so clients keep it when the disk is moved
			getInventory().getBackingInventory().sendInventoryModification(index);
		}
		return key;
	}

	private String deriveProgramName(String explicitProgramName, String sourcePath) {
		if(explicitProgramName != null && !explicitProgramName.trim().isEmpty()) {
			return explicitProgramName.trim();
		}
		if(sourcePath == null || sourcePath.trim().isEmpty()) {
			return "program";
		}

		String normalized = sourcePath.replace('\\', '/');
		int slashIndex = normalized.lastIndexOf('/');
		String fileName = slashIndex >= 0 ? normalized.substring(slashIndex + 1) : normalized;
		if(fileName.endsWith(".lua")) {
			fileName = fileName.substring(0, fileName.length() - 4);
		}
		return fileName.isEmpty() ? "program" : fileName;
	}
}
