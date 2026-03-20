package luamade.lua.element.inventory;

import api.utils.game.inventory.InventoryUtils;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.inventory.InventorySlot;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Inventory extends LuaMadeUserdata {

	private final org.schema.game.common.data.player.inventory.Inventory inventory;
	private final SegmentPiece segmentPiece;

	public Inventory(org.schema.game.common.data.player.inventory.Inventory inventory, SegmentPiece segmentPiece) {
		this.inventory = inventory;
		this.segmentPiece = segmentPiece;
	}

	@LuaMadeCallable
	public String getName() {
		if(isInventory()) return inventory.getCustomName();
		else return null;
	}

	@LuaMadeCallable
	public ItemStack[] getItems() {
		if(isInventory()) {
			ItemStack[] itemStacks = new ItemStack[inventory.getCountFilledSlots()];
			for(int i = 0; i < inventory.getCountFilledSlots(); i ++) {
				InventorySlot slot = inventory.getSlot(i);
				if(slot != null && slot.count() > 0) {
					itemStacks[i] = new ItemStack(slot.getType(), slot.count());
				}
			}
			return itemStacks;
		} else return null;
	}

	@LuaMadeCallable
	public Double getVolume() {
		if(isInventory()) {
			return inventory.getVolume();
		} else {
			return 0.0;
		}
	}

	@LuaMadeCallable
	public Boolean isEmpty() {
		if(!isInventory()) {
			return true;
		}
		return inventory.getCountFilledSlots() == 0;
	}

	@LuaMadeCallable
	public Boolean isFull() {
		if(!isInventory()) {
			return true;
		}
		return !inventory.hasFreeSlot();
	}

	@LuaMadeCallable
	public Boolean hasFreeSlot() {
		if(!isInventory()) {
			return false;
		}
		return inventory.hasFreeSlot();
	}

	@LuaMadeCallable
	public Long getItemCount(Integer id) {
		if(!isInventory()) {
			return 0L;
		}
		return (long) InventoryUtils.getItemAmount(inventory, (short)(int) id);
	}

	@LuaMadeCallable
	public Long getTotalItemCount() {
		if(!isInventory()) {
			return 0L;
		}
		long total = 0;
		for(int s : inventory.getSlots()) {
			InventorySlot slot = inventory.getSlot(s);
			if(slot != null) total += slot.count();
		}
		return total;
	}

	@LuaMadeCallable
	public Boolean addItems(ItemStack[] items) {
		if(!isInventory() || items == null){
			return false;
		}
		for(ItemStack itemStack : items) {
			if(!isMutableRequestValid(itemStack)) {
				return false;
			}
			if(!InventoryUtils.addElementById(inventory, itemStack.getId(), itemStack.getCount())) {
				return false;
			}
		}
		inventory.sendAll();
		return true;
	}

	@LuaMadeCallable
	public Boolean removeItems(ItemStack[] items) {
		if(!isInventory() || items == null) {
			return false;
		}
		for(ItemStack itemStack : items) {
			if(!isMutableRequestValid(itemStack)) {
				return false;
			}
			if(InventoryUtils.getItemAmount(inventory, itemStack.getId()) < itemStack.getCount()) {
				return false;
			}
		}
		for(ItemStack itemStack : items) {
			if(itemStack != null) {
				InventoryUtils.consumeItems(inventory, itemStack.getId(), itemStack.getCount());
			}
		}
		inventory.sendAll();
		return true;
	}

	@LuaMadeCallable
	public Boolean transferTo(Inventory to, ItemStack[] items) {
		if(isInventory() && to != null && to.isInventory() && !inventory.equals(to.inventory) && !isSameBackingInventory(to)) {
			for(ItemStack itemStack : items) {
				if(itemStack != null) {
					if(InventoryUtils.getItemAmount(inventory, itemStack.getId()) >= itemStack.getCount() && to.inventory.hasFreeSlot()) {
						InventoryUtils.consumeItems(inventory, itemStack.getId(), itemStack.getCount());
						InventoryUtils.addItem(to.inventory, itemStack.getId(), itemStack.getCount());
					} else {
						return false;
					}
				}
			}
			inventory.sendAll();
			to.inventory.sendAll();
			return true;
		} else return false;
	}

	@LuaMadeCallable
	public Boolean transferFrom(Inventory from, ItemStack[] items) {
		return from != null && from.transferTo(this, items);
	}

	@LuaMadeCallable
	public Boolean clearItems() {
		if(!isInventory()) {
			return false;
		}
		inventory.clear();
		inventory.sendAll();
		return true;
	}

	private boolean isMutableRequestValid(ItemStack itemStack) {
		return itemStack != null && itemStack.getCount() != null && itemStack.getCount() > 0;
	}

	private boolean isSameBackingInventory(Inventory other) {
		if(other == null) {
			return false;
		}
		if(inventory.equals(other.inventory)) {
			return true;
		}
		if(segmentPiece == null || other.segmentPiece == null) {
			return false;
		}
		return segmentPiece.equals(other.segmentPiece);
	}

	private Boolean isInventory() {
		return inventory != null;
	}

	public org.schema.game.common.data.player.inventory.Inventory getBackingInventory() {
		return inventory;
	}

	public InventorySlot getSlotAt(int index) {
		if(!isInventory() || index < 0) {
			return null;
		}
		try {
			return inventory.getSlot(index);
		} catch(Exception ignored) {
			return null;
		}
	}
}
