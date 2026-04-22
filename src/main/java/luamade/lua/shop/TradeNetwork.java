package luamade.lua.shop;

import api.common.GameServer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.controller.trade.TradeNodeClient;
import org.schema.game.common.controller.trade.TradeNodeStub;
import org.schema.game.network.objects.TradePriceInterface;
import org.schema.game.network.objects.TradePrices;
import org.schema.game.server.data.GameServerState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Galaxy-wide trade network queries. Works server-side (full access) and
 * client-side (cached / synced data from the server). On the client, the first
 * call for a given node's prices schedules an async refresh; retry on later ticks.
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

	/** Diagnostic dump — surfaces which state layers are reachable at runtime. */
	@LuaMadeCallable
	public String getDiagnostic() {
		StringBuilder sb = new StringBuilder();
		sb.append("GameServerState.instance=").append(GameServerState.instance != null).append('\n');
		sb.append("GameClientState.instance=").append(GameClientState.instance != null).append('\n');
		sb.append("GameServer.getServerState()=").append(safeServerState() != null).append('\n');
		Long2ObjectOpenHashMap<TradeNodeStub> server = serverNodeMap();
		sb.append("server nodeMap=").append(server == null ? "null" : String.valueOf(server.size())).append('\n');
		Long2ObjectOpenHashMap<TradeNodeStub> client = clientNodeMap();
		sb.append("client nodeMap=").append(client == null ? "null" : String.valueOf(client.size())).append('\n');
		return sb.toString();
	}

	@LuaMadeCallable
	public TradeOffer[] findBuyOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, true);
		offers.sort((a, b) -> Integer.compare(a.getPrice(), b.getPrice()));
		return offers.toArray(new TradeOffer[0]);
	}

	@LuaMadeCallable
	public TradeOffer[] findSellOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, false);
		offers.sort((a, b) -> Integer.compare(b.getPrice(), a.getPrice()));
		return offers.toArray(new TradeOffer[0]);
	}

	@LuaMadeCallable
	public TradeOffer[] findAllOffers(Short typeId) {
		List<TradeOffer> offers = collectOffers(typeId, null);
		return offers.toArray(new TradeOffer[0]);
	}

	@LuaMadeCallable
	public MarketEntry[] getMarketSnapshot() {
		Short2ObjectOpenHashMap<Aggregator> buckets = new Short2ObjectOpenHashMap<>();
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		if(map == null) return new MarketEntry[0];
		for(TradeNodeStub stub : map.values()) {
			if(stub == null || stub.remove) continue;
			for(TradePriceInterface p : pricesFor(stub)) {
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
		Long2ObjectOpenHashMap<TradeNodeStub> map = nodeMap();
		if(map == null) return out;
		short type = typeId;
		for(TradeNodeStub stub : map.values()) {
			if(stub == null || stub.remove) continue;
			TradeNodeInfo nodeInfo = null;
			for(TradePriceInterface p : pricesFor(stub)) {
				if(p == null || p.getType() != type) continue;
				boolean isPlayerBuy = !p.isBuy();
				if(playerBuys != null && isPlayerBuy != playerBuys) continue;
				if(nodeInfo == null) nodeInfo = new TradeNodeInfo(stub);
				out.add(new TradeOffer(nodeInfo, p.getType(), p.getPrice(), p.getAmount(), p.getLimit(), isPlayerBuy));
			}
		}
		return out;
	}

	// ---- state lookup --------------------------------------------------------

	private static GameServerState safeServerState() {
		try {
			return GameServer.getServerState();
		} catch(Exception e) {
			return null;
		}
	}

	private static Long2ObjectOpenHashMap<TradeNodeStub> serverNodeMap() {
		GameServerState state = safeServerState();
		if(state == null) return null;
		try {
			return state.getUniverse().getGalaxyManager().getTradeNodeDataById();
		} catch(Exception e) {
			return null;
		}
	}

	private static Long2ObjectOpenHashMap<TradeNodeStub> clientNodeMap() {
		GameClientState cs = GameClientState.instance;
		if(cs == null) return null;
		try {
			return cs.getController().getClientChannel().getGalaxyManagerClient().getTradeNodeDataById();
		} catch(Exception e) {
			return null;
		}
	}

	/** Returns whichever side has the trade node registry, preferring server. */
	private static Long2ObjectOpenHashMap<TradeNodeStub> nodeMap() {
		Long2ObjectOpenHashMap<TradeNodeStub> server = serverNodeMap();
		if(server != null) return server;
		return clientNodeMap();
	}

	/** Returns the current price list for a node, regardless of client/server context. */
	private static List<TradePriceInterface> pricesFor(TradeNodeStub stub) {
		GameServerState ss = safeServerState();
		if(ss != null) {
			try {
				TradePrices tp = stub.getTradePricesInstance(ss);
				return tp == null ? Collections.emptyList() : tp.getPrices();
			} catch(Exception e) {
				return Collections.emptyList();
			}
		}
		if(stub instanceof TradeNodeClient) {
			try {
				List<TradePriceInterface> list = ((TradeNodeClient) stub).getTradePricesClient();
				return list == null ? Collections.emptyList() : list;
			} catch(Exception e) {
				return Collections.emptyList();
			}
		}
		return Collections.emptyList();
	}
}
