package luamade.lua.shop;

import api.common.GameServer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.trade.TradeNodeStub;
import org.schema.game.network.objects.TradePriceInterface;
import org.schema.game.network.objects.TradePrices;
import org.schema.game.server.data.GameServerState;

import java.util.ArrayList;
import java.util.List;

/**
 * Galaxy-wide trade network queries. Wraps GalaxyManager's trade node registry.
 * Server-side only — returns empty results on clients.
 */
public class TradeNetwork extends LuaMadeUserdata {

	@LuaMadeCallable
	public TradeNodeInfo[] getNodes() {
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		if(map == null) return new TradeNodeInfo[0];
		ArrayList<TradeNodeInfo> out = new ArrayList<>(map.size());
		for(TradeNodeStub stub : map.values()) {
			if(stub != null && !stub.remove) out.add(new TradeNodeInfo(stub));
		}
		return out.toArray(new TradeNodeInfo[0]);
	}

	@LuaMadeCallable
	public Integer getNodeCount() {
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		return map == null ? 0 : map.size();
	}

	/**
	 * Offers where the player can BUY the given item type (shop sells it).
	 * Sorted cheapest first.
	 */
	@LuaMadeCallable
	public TradeOffer[] findBuyOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, true);
		offers.sort((a, b) -> Integer.compare(a.getPrice(), b.getPrice()));
		return offers.toArray(new TradeOffer[0]);
	}

	/**
	 * Offers where the player can SELL the given item type (shop buys it).
	 * Sorted highest price first.
	 */
	@LuaMadeCallable
	public TradeOffer[] findSellOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, false);
		offers.sort((a, b) -> Integer.compare(b.getPrice(), a.getPrice()));
		return offers.toArray(new TradeOffer[0]);
	}

	/** All offers for the given type, both directions, unsorted. */
	@LuaMadeCallable
	public TradeOffer[] findAllOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, null);
		return offers.toArray(new TradeOffer[0]);
	}

	/**
	 * One-pass aggregate of every item type traded on the network.
	 * Far cheaper than calling findBuyOffers/findSellOffers per type.
	 */
	@LuaMadeCallable
	public MarketEntry[] getMarketSnapshot() {
		Short2ObjectOpenHashMap<Aggregator> buckets = new Short2ObjectOpenHashMap<>();
		GameServerState state = serverState();
		if(state == null) return new MarketEntry[0];
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		if(map == null) return new MarketEntry[0];
		for(TradeNodeStub stub : map.values()) {
			if(stub == null || stub.remove) continue;
			TradePrices prices;
			try {
				prices = stub.getTradePricesInstance(state);
			} catch(Exception e) {
				continue;
			}
			if(prices == null) continue;
			for(TradePriceInterface p : prices.getPrices()) {
				if(p == null) continue;
				short type = p.getType();
				Aggregator agg = buckets.get(type);
				if(agg == null) {
					agg = new Aggregator();
					buckets.put(type, agg);
				}
				int price = p.getPrice();
				int amount = p.getAmount();
				if(p.isBuy()) {
					agg.sellCount++;
					agg.sellSum += price;
					if(agg.minSell == null || price < agg.minSell) agg.minSell = price;
					if(agg.maxSell == null || price > agg.maxSell) agg.maxSell = price;
				} else {
					agg.buyCount++;
					agg.buySum += price;
					if(agg.minBuy == null || price < agg.minBuy) agg.minBuy = price;
					if(agg.maxBuy == null || price > agg.maxBuy) agg.maxBuy = price;
					agg.totalStock += Math.max(0, amount);
				}
			}
		}
		ArrayList<MarketEntry> out = new ArrayList<>(buckets.size());
		for(Short2ObjectOpenHashMap.Entry<Aggregator> e : buckets.short2ObjectEntrySet()) {
			short type = e.getShortKey();
			Aggregator a = e.getValue();
			Integer avgBuy = a.buyCount > 0 ? (int)(a.buySum / a.buyCount) : null;
			Integer avgSell = a.sellCount > 0 ? (int)(a.sellSum / a.sellCount) : null;
			out.add(new MarketEntry(type, a.buyCount, a.sellCount, avgBuy, avgSell,
				a.minBuy, a.maxBuy, a.minSell, a.maxSell, a.totalStock));
		}
		return out.toArray(new MarketEntry[0]);
	}

	private static class Aggregator {
		int buyCount, sellCount;
		long buySum, sellSum;
		Integer minBuy, maxBuy, minSell, maxSell;
		long totalStock;
	}

	private List<TradeOffer> collectOffers(Short typeId, Boolean playerBuys) {
		ArrayList<TradeOffer> out = new ArrayList<>();
		if(typeId == null) return out;
		GameServerState state = serverState();
		if(state == null) return out;
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		if(map == null) return out;
		short type = typeId;
		for(TradeNodeStub stub : map.values()) {
			if(stub == null || stub.remove) continue;
			TradePrices prices;
			try {
				prices = stub.getTradePricesInstance(state);
			} catch(Exception e) {
				continue;
			}
			if(prices == null) continue;
			TradeNodeInfo nodeInfo = new TradeNodeInfo(stub);
			for(TradePriceInterface p : prices.getPrices()) {
				if(p == null || p.getType() != type) continue;
				boolean isPlayerBuy = !p.isBuy();
				if(playerBuys != null && isPlayerBuy != playerBuys) continue;
				out.add(new TradeOffer(nodeInfo, p.getType(), p.getPrice(), p.getAmount(), p.getLimit(), isPlayerBuy));
			}
		}
		return out;
	}

	private static GameServerState serverState() {
		try {
			return GameServer.getServerState();
		} catch(Exception e) {
			return null;
		}
	}

	private static Long2ObjectOpenHashMap<TradeNodeStub> nodeMap() {
		GameServerState state = serverState();
		if(state == null) return null;
		try {
			return state.getUniverse().getGalaxyManager().getTradeNodeDataById();
		} catch(Exception e) {
			return null;
		}
	}
}
