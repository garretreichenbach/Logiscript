package luamade.lua.shop;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementKeyMap;

/**
 * A single price quote from a trade node for one item type, in one direction
 * (the shop is either willing to sell to you or buy from you).
 */
public class TradeOffer extends LuaMadeUserdata {

	private final TradeNodeInfo node;
	private final short typeId;
	private final int price;
	private final int amount;
	private final int limit;
	private final boolean playerBuys;

	public TradeOffer(TradeNodeInfo node, short typeId, int price, int amount, int limit, boolean playerBuys) {
		this.node = node;
		this.typeId = typeId;
		this.price = price;
		this.amount = amount;
		this.limit = limit;
		this.playerBuys = playerBuys;
	}

	@LuaMadeCallable
	public TradeNodeInfo getNode() {
		return node;
	}

	@LuaMadeCallable
	public Short getId() {
		return typeId;
	}

	@LuaMadeCallable
	public BlockInfo getInfo() {
		return new BlockInfo(ElementKeyMap.getInfo(typeId));
	}

	@LuaMadeCallable
	public Integer getPrice() {
		return price;
	}

	@LuaMadeCallable
	public Integer getAmount() {
		return amount;
	}

	@LuaMadeCallable
	public Integer getLimit() {
		return limit;
	}

	/** True if this offer is one the player can buy from; false if the shop is buying (player sells). */
	@LuaMadeCallable
	public Boolean isPlayerBuy() {
		return playerBuys;
	}
}
