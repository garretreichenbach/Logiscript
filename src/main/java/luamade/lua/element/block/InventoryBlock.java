package luamade.lua.element.block;

import luamade.lua.element.inventory.Inventory;
import luamade.lua.element.inventory.ItemStack;
import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.data.SegmentPiece;

public class InventoryBlock extends Block {

	public InventoryBlock(SegmentPiece piece) {
		super(piece);
	}

	@LuaMadeCallable
	public String getInventoryName() {
		Inventory inventory = getInventory();
		return inventory == null ? null : inventory.getName();
	}

	@LuaMadeCallable
	public ItemStack[] getItems() {
		Inventory inventory = getInventory();
		return inventory == null ? null : inventory.getItems();
	}

	@LuaMadeCallable
	public Double getInventoryVolume() {
		Inventory inventory = getInventory();
		return inventory == null ? 0.0 : inventory.getVolume();
	}

	@LuaMadeCallable
	public Boolean hasItems() {
		ItemStack[] items = getItems();
		return items != null && items.length > 0;
	}

	@LuaMadeCallable
	public Boolean addItems(ItemStack[] items) {
		Inventory inventory = getInventory();
		return inventory != null && inventory.addItems(items);
	}

	@LuaMadeCallable
	public Boolean removeItems(ItemStack[] items) {
		Inventory inventory = getInventory();
		return inventory != null && inventory.removeItems(items);
	}

	@LuaMadeCallable
	public Boolean transferItemsTo(Inventory targetInventory, ItemStack[] items) {
		Inventory inventory = getInventory();
		return inventory != null && inventory.transferTo(targetInventory, items);
	}

	@LuaMadeCallable
	public Boolean transferItemsFrom(Inventory sourceInventory, ItemStack[] items) {
		Inventory inventory = getInventory();
		return inventory != null && inventory.transferFrom(sourceInventory, items);
	}

	@LuaMadeCallable
	public Boolean clearItems() {
		Inventory inventory = getInventory();
		return inventory != null && inventory.clearItems();
	}
}
