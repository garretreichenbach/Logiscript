package luamade.lua.element.inventory;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementKeyMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ItemStack extends LuaMadeUserdata {

	private short id;
	private int count;

	public ItemStack(short id, int count) {
		this.id = id;
		this.count = count;
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
