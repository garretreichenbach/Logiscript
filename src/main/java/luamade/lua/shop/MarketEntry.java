package luamade.lua.shop;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.data.element.ElementKeyMap;

/**
 * Aggregate statistics for one item type across the entire trade network.
 * Produced by {@link TradeNetwork#getMarketSnapshot()}.
 */
public class MarketEntry extends LuaMadeUserdata {

	private final short typeId;
	private final int buyOfferCount;   // shops selling to player
	private final int sellOfferCount;  // shops buying from player
	private final Integer avgBuy;       // mean price player pays to buy
	private final Integer avgSell;      // mean price player receives to sell
	private final Integer minBuy;
	private final Integer maxBuy;
	private final Integer minSell;
	private final Integer maxSell;
	private final long totalStock;

	public MarketEntry(short typeId,
	                   int buyOfferCount, int sellOfferCount,
	                   Integer avgBuy, Integer avgSell,
	                   Integer minBuy, Integer maxBuy,
	                   Integer minSell, Integer maxSell,
	                   long totalStock) {
		this.typeId = typeId;
		this.buyOfferCount = buyOfferCount;
		this.sellOfferCount = sellOfferCount;
		this.avgBuy = avgBuy;
		this.avgSell = avgSell;
		this.minBuy = minBuy;
		this.maxBuy = maxBuy;
		this.minSell = minSell;
		this.maxSell = maxSell;
		this.totalStock = totalStock;
	}

	@LuaMadeCallable
	public Short getId() { return typeId; }

	@LuaMadeCallable
	public BlockInfo getInfo() { return new BlockInfo(ElementKeyMap.getInfo(typeId)); }

	@LuaMadeCallable
	public String getName() {
		return ElementKeyMap.getInfo(typeId).getName();
	}

	@LuaMadeCallable
	public Integer getBuyOfferCount() { return buyOfferCount; }

	@LuaMadeCallable
	public Integer getSellOfferCount() { return sellOfferCount; }

	/** Mean price the player pays to buy across all shops that sell this item. */
	@LuaMadeCallable
	public Integer getAvgBuy() { return avgBuy; }

	/** Mean price the player receives when selling across all shops that buy this item. */
	@LuaMadeCallable
	public Integer getAvgSell() { return avgSell; }

	@LuaMadeCallable
	public Integer getMinBuy() { return minBuy; }

	@LuaMadeCallable
	public Integer getMaxBuy() { return maxBuy; }

	@LuaMadeCallable
	public Integer getMinSell() { return minSell; }

	@LuaMadeCallable
	public Integer getMaxSell() { return maxSell; }

	@LuaMadeCallable
	public Long getTotalStock() { return totalStock; }
}
