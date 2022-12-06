package luamade.lua.element.inventory;

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
	public Boolean isInventory() {
		return inventory != null;
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
}
