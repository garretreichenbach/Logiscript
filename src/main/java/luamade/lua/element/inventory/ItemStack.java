package luamade.lua.element.inventory;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementKeyMap;

public class ItemStack extends LuaMadeUserdata {

	private short id;
	private int count;
	private final Integer slot;

	public ItemStack(short id, int count) {
		this(id, count, null);
	}

	public ItemStack(short id, int count, Integer slot) {
		this.id = id;
		this.count = count;
		this.slot = slot;
	}

	/** Inventory slot this stack was read from, or nil for stacks built by scripts. */
	@LuaMadeCallable
	public Integer getSlot() {
		return slot;
	}

	@LuaMadeCallable
	public Short getId() {
		return id;
	}

	@LuaMadeCallable
	public Integer getCount() {
		return count;
	}

	@LuaMadeCallable
	public BlockInfo getInfo() {
		return new BlockInfo(ElementKeyMap.getInfo(id));
	}
}
