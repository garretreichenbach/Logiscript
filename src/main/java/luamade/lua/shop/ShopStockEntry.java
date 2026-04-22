package luamade.lua.shop;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementKeyMap;

public class ShopStockEntry extends LuaMadeUserdata {

	private final short typeId;
	private final int count;
	private final Integer buyPrice;
	private final Integer sellPrice;
	private final Integer buyLimit;
	private final Integer sellLimit;

	public ShopStockEntry(short typeId, int count, Integer buyPrice, Integer sellPrice, Integer buyLimit, Integer sellLimit) {
		this.typeId = typeId;
		this.count = count;
		this.buyPrice = buyPrice;
		this.sellPrice = sellPrice;
		this.buyLimit = buyLimit;
		this.sellLimit = sellLimit;
	}

	@LuaMadeCallable
	public Short getId() {
		return typeId;
	}

	@LuaMadeCallable
	public Integer getCount() {
		return count;
	}

	@LuaMadeCallable
	public Integer getBuyPrice() {
		return buyPrice;
	}

	@LuaMadeCallable
	public Integer getSellPrice() {
		return sellPrice;
	}

	@LuaMadeCallable
	public Integer getBuyLimit() {
		return buyLimit;
	}

	@LuaMadeCallable
	public Integer getSellLimit() {
		return sellLimit;
	}

	@LuaMadeCallable
	public BlockInfo getInfo() {
		return new BlockInfo(ElementKeyMap.getInfo(typeId));
	}
}
