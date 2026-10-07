package luamade.lua.shop;

import it.unimi.dsi.fastutil.shorts.Short2IntLinkedOpenHashMap;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.utils.ServerThread;
import org.schema.common.util.LogInterface;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.controller.trade.TradeNodeStub;
import org.schema.game.common.controller.trade.TradeOrder;
import org.schema.game.common.controller.trade.TradingGuildTradeOrderConfig;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.network.objects.TradePriceInterface;
import org.schema.game.network.objects.TradePrices;
import org.schema.game.server.data.GameServerState;

import java.util.Collections;
import java.util.List;

/**
 * A Trading Guild order from the script's own shop to another trade node.
 * Built up in Lua with addBuy/addSell, then priced with quote() or sent with
 * submit(). Validation and execution are the game's own (same checks as the
 * in-game trade dialog), run on the server thread.
 */
public class TradeOrderDraft extends LuaMadeUserdata {

	private final ShopInterface shop;
	private final long targetDbId;
	private final Short2IntLinkedOpenHashMap buys = new Short2IntLinkedOpenHashMap();
	private final Short2IntLinkedOpenHashMap sells = new Short2IntLinkedOpenHashMap();

	private String error;
	private long buyPrice, sellPrice, deliveryCost, totalPrice;
	private double buyVolume, sellVolume;
	private int distance, cargoShips;

	public TradeOrderDraft(ShopInterface shop, long targetDbId) {
		this.shop = shop;
		this.targetDbId = targetDbId;
	}

	/** Buy blocks from the target node. Amount 0 removes the line. */
	@LuaMadeCallable
	public TradeOrderDraft addBuy(Short typeId, Integer amount) {
		put(buys, typeId, amount);
		return this;
	}

	/** Sell blocks from this shop to the target node. Amount 0 removes the line. */
	@LuaMadeCallable
	public TradeOrderDraft addSell(Short typeId, Integer amount) {
		put(sells, typeId, amount);
		return this;
	}

	@LuaMadeCallable
	public void clear() {
		buys.clear();
		sells.clear();
	}

	/** Prices the order and runs the game's trade checks without sending it. */
	@LuaMadeCallable
	public Boolean quote() {
		return ServerThread.call(() -> run(false));
	}

	/** Sends the order to the Trading Guild. Credits are deducted on success. */
	@LuaMadeCallable
	public Boolean submit() {
		return ServerThread.call(() -> run(true));
	}

	/** Reason the last quote()/submit() failed, or nil. */
	@LuaMadeCallable
	public String getError() {
		return error;
	}

	@LuaMadeCallable
	public Long getTargetDbId() {
		return targetDbId;
	}

	@LuaMadeCallable
	public Long getBuyPrice() {
		return buyPrice;
	}

	@LuaMadeCallable
	public Long getSellPrice() {
		return sellPrice;
	}

	/** Trading Guild fee (distance, cargo ships and guild cut). */
	@LuaMadeCallable
	public Long getDeliveryCost() {
		return deliveryCost;
	}

	/** Credits this shop pays: buy price - sell price + delivery cost. */
	@LuaMadeCallable
	public Long getTotalPrice() {
		return totalPrice;
	}

	@LuaMadeCallable
	public Double getBuyVolume() {
		return buyVolume;
	}

	@LuaMadeCallable
	public Double getSellVolume() {
		return sellVolume;
	}

	/** Distance in star systems. */
	@LuaMadeCallable
	public Integer getDistance() {
		return distance;
	}

	@LuaMadeCallable
	public Integer getCargoShips() {
		return cargoShips;
	}

	private static void put(Short2IntLinkedOpenHashMap map, Short typeId, Integer amount) {
		if(typeId == null || amount == null || amount < 0 || !ElementKeyMap.isValidType(typeId)) return;
		if(amount == 0) map.remove((short) typeId);
		else map.put((short) typeId, (int) amount);
	}

	private boolean run(boolean execute) {
		error = null;
		if(!(shop.getState() instanceof GameServerState)) return fail("trade orders are server-side only");
		GameServerState state = (GameServerState) shop.getState();
		if(buys.isEmpty() && sells.isEmpty()) return fail("order is empty");
		if(shop.getSegmentController() == null) return fail("shop is not loaded");
		long ownDbId = shop.getSegmentController().dbId;
		if(ownDbId == targetDbId) return fail("cannot trade with own shop");

		TradeNodeStub target = state.getUniverse().getGalaxyManager().getTradeNodeDataById().get(targetDbId);
		if(target == null) return fail("target trade node not found");

		Vector3i ownSystem = shop.getSegmentController().getSystem(new Vector3i());
		TradeOrder order = new TradeOrder(state, new TradingGuildTradeOrderConfig(), ownDbId, ownSystem, target.getSystem(), target);
		// The game only reports failures through a player chat message (null here) and its log; keep the last error.
		order.logger = (msg, level) -> {
			if(level == LogInterface.LogLevel.ERROR) error = msg;
		};
		for(Short2IntLinkedOpenHashMap.Entry e : buys.short2IntEntrySet()) order.addOrChangeBuy(e.getShortKey(), e.getIntValue(), false);
		for(Short2IntLinkedOpenHashMap.Entry e : sells.short2IntEntrySet()) order.addOrChangeSell(e.getShortKey(), e.getIntValue(), false);
		order.recalc();

		boolean ok;
		if(execute) {
			ok = state.getGameState().getTradeManager().executeTradeOrderServer(order);
		} else {
			List<TradePriceInterface> ownPrices = shop.getShoppingAddOn().getPricesRep();
			ok = state.getGameState().getTradeManager().checkTrade(order, shop.getTradeNode(), target, ownPrices, pricesOf(target, state));
		}
		buyPrice = order.getBuyPrice();
		sellPrice = order.getSellPrice();
		deliveryCost = order.getTradingGuildPrice();
		totalPrice = order.getTotalPrice();
		buyVolume = order.getBuyVolume();
		sellVolume = order.getSellVolume();
		distance = order.getTradeDistance();
		cargoShips = order.getUsedTradeShips();
		if(!ok && error == null) error = "trade order rejected";
		return ok;
	}

	private static List<TradePriceInterface> pricesOf(TradeNodeStub stub, GameServerState state) {
		try {
			TradePrices tp = stub.getTradePricesInstance(state);
			return tp == null ? Collections.emptyList() : tp.getPrices();
		} catch(Exception e) {
			return Collections.emptyList();
		}
	}

	private boolean fail(String msg) {
		error = msg;
		return false;
	}
}
