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
				if(slot != null && slot.count() > 0) itemStacks[i] = new ItemStack(slot.getType(), slot.count());
			}
			return itemStacks;
		} else return null;
	}

	@LuaMadeCallable
	public Double getVolume() {
		if(isInventory()) return inventory.getVolume();
		else return 0.0;
	}

	@LuaMadeCallable
	public Boolean isEmpty() {
		if(!isInventory()) return true;
		return inventory.getCountFilledSlots() == 0;
	}

	@LuaMadeCallable
	public Boolean isFull() {
		if(!isInventory()) return true;
		return !inventory.hasFreeSlot();
	}

	@LuaMadeCallable
	public Boolean hasFreeSlot() {
		if(!isInventory()) return false;
		return inventory.hasFreeSlot();
	}

	@LuaMadeCallable
	public Long getItemCount(Integer id) {
		if(!isInventory()) return 0L;
		return (long) InventoryUtils.getItemAmount(inventory, (short)(int) id);
	}

	@LuaMadeCallable
	public Long getTotalItemCount() {
		if(!isInventory()) return 0L;
		long total = 0;
		for(int s : inventory.getSlots()) {
			InventorySlot slot = inventory.getSlot(s);
			if(slot != null) total += slot.count();
		}
		return total;
	}

	@LuaMadeCallable
	public Boolean transferTo(Inventory to, ItemStack[] items) {
		if(isInventory() && to.isInventory() && !inventory.equals(to.inventory) && !segmentPiece.equals(to.segmentPiece)) {
			for(ItemStack itemStack : items) {
				if(itemStack != null) {
					if(InventoryUtils.getItemAmount(inventory, itemStack.getId()) >= itemStack.getCount() && to.inventory.hasFreeSlot()) {
						InventoryUtils.consumeItems(inventory, itemStack.getId(), itemStack.getCount());
						InventoryUtils.addItem(to.inventory, itemStack.getId(), itemStack.getCount());
					} else return false;
				}
			}
			return true;
		} else return false;
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
