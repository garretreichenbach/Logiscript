package luamade.lua.element.block;

import luamade.element.ElementRegistry;
import luamade.lua.disk.DiskDataStore;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.element.inventory.ItemStack;
import luamade.luawrap.LuaMadeCallable;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.inventory.InventorySlot;

import java.util.Collections;
import java.util.List;

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
		InventorySlot slot = getInsertedDiskSlot();
		if(slot == null) {
			return null;
		}
		SegmentPiece piece = getSegmentPiece();
		return DiskDataStore.resolveDiskKey(slot, piece);
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

	@LuaMadeCallable
	public Integer buyProgram(String programName, String destinationPath) {
		if(!installProgram(programName, destinationPath)) {
			return null;
		}
		Integer price = getProgramPrice(programName);
		return price == null ? 0 : price;
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
		Inventory inventory = getInventory();
		if(inventory == null || inventory.getBackingInventory() == null) {
			return null;
		}

		org.schema.game.common.data.player.inventory.Inventory nativeInventory = inventory.getBackingInventory();
		for(int i = 0; i < nativeInventory.getCountFilledSlots(); i++) {
			InventorySlot slot = inventory.getSlotAt(i);
			if(slot != null && slot.count() > 0 && slot.getType() == ElementRegistry.DISK.getId()) {
				return slot;
			}
		}
		return null;
	}

	private String resolveDiskKey() {
		InventorySlot slot = getInsertedDiskSlot();
		if(slot == null) {
			return null;
		}
		SegmentPiece piece = getSegmentPiece();
		return DiskDataStore.resolveDiskKey(slot, piece);
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
