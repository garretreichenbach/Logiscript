package luamade.lua.element.inventory;

import api.inventory.InventoryUtils;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.SegmentPiece;
import luamade.utils.CustomDataUtils;
import org.json.JSONObject;
import org.schema.game.common.data.player.inventory.InventorySlot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			List<ItemStack> itemStacks = new ArrayList<>();
			for(int s : inventory.getSlots()) {
				InventorySlot slot = inventory.getSlot(s);
				if(slot != null && slot.count() > 0) {
					itemStacks.add(new ItemStack(slot.getType(), slot.count(), s));
				}
			}
			return itemStacks.toArray(new ItemStack[0]);
		} else return null;
	}

	@LuaMadeCallable
	public String getSlotData(Integer slot) {
		InventorySlot inventorySlot = slot == null ? null : getSlotAt(slot);
		if(inventorySlot == null || !inventorySlot.hasCustomData()) {
			return inventorySlot == null ? null : "{}";
		}
		return CustomDataUtils.toJson(inventorySlot.getCustomData());
	}

	@LuaMadeCallable
	public Boolean setSlotData(Integer slot, String json) {
		InventorySlot inventorySlot = slot == null ? null : getSlotAt(slot);
		if(inventorySlot == null || inventorySlot.count() <= 0) {
			return false;
		}
		JSONObject data = CustomDataUtils.fromScript(json, inventorySlot.hasCustomData() ? inventorySlot.getCustomData() : null);
		if(data.length() == 0) {
			inventorySlot.clearCustomData();
		} else {
			inventorySlot.setCustomData(data);
		}
		inventory.sendInventoryModification(slot);
		return true;
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
		if(!hasAll(items)) {
			return false;
		}
		for(ItemStack itemStack : items) {
			consume(itemStack.getId(), itemStack.getCount());
		}
		inventory.sendAll();
		return true;
	}

	/** True when every stack is valid and the inventory holds the combined count per item, so stacks of the same item can't each pass on the same items. */
	private boolean hasAll(ItemStack[] items) {
		if(items == null) {
			return false;
		}
		Map<Short, Long> needed = new HashMap<>();
		for(ItemStack itemStack : items) {
			if(!isMutableRequestValid(itemStack)) {
				return false;
			}
			needed.merge(itemStack.getId(), (long) itemStack.getCount(), Long::sum);
		}
		for(Map.Entry<Short, Long> entry : needed.entrySet()) {
			if(InventoryUtils.getItemAmount(inventory, entry.getKey()) < entry.getValue()) {
				return false;
			}
		}
		return true;
	}

	@LuaMadeCallable
	public Boolean transferTo(Inventory to, ItemStack[] items) {
		if(isInventory() && to != null && to.isInventory() && !inventory.equals(to.inventory) && !isSameBackingInventory(to) && hasAll(items)) {
			for(ItemStack itemStack : items) {
				if(itemStack != null) {
					if(InventoryUtils.getItemAmount(inventory, itemStack.getId()) >= itemStack.getCount() && to.inventory.hasFreeSlot()) {
						consume(itemStack.getId(), itemStack.getCount());
						InventoryUtils.addItem(to.inventory, itemStack.getId(), itemStack.getCount());
					} else {
						return false;
					}
				}
			}
			inventory.sendAll();
			to.inventory.sendAll();
			return true;
		} else {
			return false;
		}
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

	/** InventoryUtils.consumeItems leaves an exactly-emptied slot at count 0 with its type set, so it still counts as filled; remove those. */
	private void consume(short id, int count) {
		InventoryUtils.consumeItems(inventory, id, count);
		for(int slot : new ArrayList<>(inventory.getSlots())) {
			InventorySlot inventorySlot = inventory.getSlot(slot);
			if(inventorySlot != null && inventorySlot.getType() == id && !inventorySlot.isMultiSlot() && inventorySlot.count() <= 0) {
				inventory.removeSlot(slot, true);
			}
		}
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
