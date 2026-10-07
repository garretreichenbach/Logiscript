package luamade.lua.shop;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import luamade.lua.entity.RemoteEntity;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.lua.terminal.ScriptInvoker;
import luamade.luawrap.LuaMadeUserdata;
import luamade.utils.ServerThread;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.game.common.data.player.inventory.ShopInventory;
import org.schema.game.network.objects.TradePriceInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Shop extends LuaMadeUserdata {

	private final ShopInterface shop;
	/** True when this is the script's own entity; only then may it place trade orders. */
	private final boolean own;

	public Shop(ShopInterface shop, boolean own) {
		this.shop = shop;
		this.own = own;
	}

	/**
	 * Starts a Trading Guild order from this shop to another trade node.
	 * Only available on the script's own shop (entity.asShop(), not a remote one).
	 */
	@LuaMadeCallable
	public TradeOrderDraft createOrder(Long targetDbId) {
		if(!own) throw new LuaError("trade orders can only be placed from the computer's own shop");
		if(targetDbId == null) return null;
		return new TradeOrderDraft(shop, targetDbId);
	}

	/** Trading Guild shipments currently in flight to or from this shop. */
	@LuaMadeCallable
	public ActiveTrade[] getActiveTrades() {
		Long dbId = getDbId();
		return dbId == null ? new ActiveTrade[0] : TradeNetwork.activeTrades(dbId);
	}

	@LuaMadeCallable
	public Long getCredits() {
		return shop.getCredits();
	}

	@LuaMadeCallable
	public Integer getFactionId() {
		return shop.getFactionId();
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(shop.getFactionId());
	}

	@LuaMadeCallable
	public Boolean isAiShop() {
		return shop.isAiShop();
	}

	@LuaMadeCallable
	public Boolean isInfiniteSupply() {
		return shop.isInfiniteSupply();
	}

	@LuaMadeCallable
	public Boolean isValid() {
		return shop.isValidShop();
	}

	@LuaMadeCallable
	public String[] getOwners() {
		Set<String> owners = shop.getShopOwners();
		if(owners == null) return new String[0];
		return owners.toArray(new String[0]);
	}

	@LuaMadeCallable
	public Long getPurchasePermission() {
		return shop.getPermissionToPurchase();
	}

	@LuaMadeCallable
	public Long getTradePermission() {
		return shop.getPermissionToTrade();
	}

	@LuaMadeCallable
	public ShopStockEntry[] getStock() {
		ShopInventory inventory = shop.getShopInventory();
		if(inventory == null) return new ShopStockEntry[0];
		ArrayList<ShopStockEntry> entries = new ArrayList<>();
		for(int slotIdx : inventory.getSlots()) {
			InventorySlot slot = inventory.getSlot(slotIdx);
			if(slot == null || slot.count() <= 0) continue;
			short type = slot.getType();
			entries.add(buildEntry(type, slot.count()));
		}
		return entries.toArray(new ShopStockEntry[0]);
	}

	@LuaMadeCallable
	public ShopStockEntry getStockFor(Short typeId) {
		if(typeId == null) return null;
		ShopInventory inventory = shop.getShopInventory();
		if(inventory == null) return null;
		int count = 0;
		for(int slotIdx : inventory.getSlots()) {
			InventorySlot slot = inventory.getSlot(slotIdx);
			if(slot != null && slot.getType() == typeId) count += slot.count();
		}
		return buildEntry(typeId, count);
	}

	/**
	 * Price the player pays to buy this item from the shop (nil if the shop doesn't sell it).
	 */
	@LuaMadeCallable
	public Integer getBuyPrice(Short typeId) {
		if(typeId == null) return null;
		TradePriceInterface price = shop.getPrice(typeId, false);
		return price == null ? null : price.getPrice();
	}

	/**
	 * Price the player receives when selling this item to the shop (nil if the shop doesn't buy it).
	 */
	@LuaMadeCallable
	public Integer getSellPrice(Short typeId) {
		if(typeId == null) return null;
		TradePriceInterface price = shop.getPrice(typeId, true);
		return price == null ? null : price.getPrice();
	}

	@LuaMadeCallable
	public Short[] getBuyableTypes() {
		TreeSet<Short> set = new TreeSet<>();
		List<TradePriceInterface> prices = shop.getShoppingAddOn().getPricesRep();
		if(prices != null) {
			for(TradePriceInterface p : prices) {
				if(p != null && p.isSell()) set.add(p.getType());
			}
		}
		return set.toArray(new Short[0]);
	}

	@LuaMadeCallable
	public Short[] getSellableTypes() {
		TreeSet<Short> set = new TreeSet<>();
		List<TradePriceInterface> prices = shop.getShoppingAddOn().getPricesRep();
		if(prices != null) {
			for(TradePriceInterface p : prices) {
				if(p != null && p.isBuy()) set.add(p.getType());
			}
		}
		return set.toArray(new Short[0]);
	}

	/**
	 * Execute a purchase on behalf of the named player. Server-side only.
	 * Returns true if the player's inventory gained any of the item.
	 */
	@LuaMadeCallable
	public Boolean buy(String playerName, Short typeId, Integer quantity) {
		if(playerName == null || typeId == null || quantity == null || quantity <= 0) return false;
		PlayerState player = requireInvoker(playerName);
		return ServerThread.call(() -> buyOnServer(player, typeId, quantity));
	}

	private boolean buyOnServer(PlayerState player, short typeId, int quantity) {
		int before = player.getInventory().getOverallQuantity(typeId);
		IntOpenHashSet invMod = new IntOpenHashSet();
		IntOpenHashSet shopHash = new IntOpenHashSet();
		try {
			shop.getShoppingAddOn().buy(player, typeId, quantity, shop, invMod, shopHash);
		} catch(Exception e) {
			return false;
		}
		if(!invMod.isEmpty()) player.getInventory().sendInventoryModification(invMod);
		if(!shopHash.isEmpty()) shop.getShopInventory().sendInventoryModification(shopHash);
		return player.getInventory().getOverallQuantity(typeId) > before;
	}

	/**
	 * Execute a sale on behalf of the named player. Server-side only.
	 * Returns true if the player's inventory lost any of the item.
	 */
	@LuaMadeCallable
	public Boolean sell(String playerName, Short typeId, Integer quantity) {
		if(playerName == null || typeId == null || quantity == null || quantity <= 0) return false;
		PlayerState player = requireInvoker(playerName);
		return ServerThread.call(() -> sellOnServer(player, typeId, quantity));
	}

	private boolean sellOnServer(PlayerState player, short typeId, int quantity) {
		int before = player.getInventory().getOverallQuantity(typeId);
		IntOpenHashSet invMod = new IntOpenHashSet();
		IntOpenHashSet shopHash = new IntOpenHashSet();
		try {
			shop.getShoppingAddOn().sell(player, typeId, quantity, shop, invMod, shopHash);
		} catch(Exception e) {
			return false;
		}
		if(!invMod.isEmpty()) player.getInventory().sendInventoryModification(invMod);
		if(!shopHash.isEmpty()) shop.getShopInventory().sendInventoryModification(shopHash);
		return player.getInventory().getOverallQuantity(typeId) < before;
	}

	@LuaMadeCallable
	public Long getDbId() {
		return shop.getSegmentController() == null ? null : shop.getSegmentController().dbId;
	}

	@LuaMadeCallable
	public RemoteEntity getEntity() {
		return shop.getSegmentController() == null ? null : new RemoteEntity(shop.getSegmentController());
	}

	/** Scripts may only trade for the player who ran them, never for someone else by name. */
	private static PlayerState requireInvoker(String playerName) {
		PlayerState invoker = ScriptInvoker.get();
		if(invoker == null) throw new LuaError("shop transactions need a player to have run the script");
		if(!invoker.getName().equalsIgnoreCase(playerName)) throw new LuaError("shop transactions are limited to the player running the script");
		return invoker;
	}

	private ShopStockEntry buildEntry(short type, int count) {
		TradePriceInterface buyFromShop = shop.getPrice(type, false);
		TradePriceInterface sellToShop = shop.getPrice(type, true);
		Integer buyPrice = buyFromShop == null ? null : buyFromShop.getPrice();
		Integer sellPrice = sellToShop == null ? null : sellToShop.getPrice();
		Integer buyLimit = buyFromShop == null ? null : buyFromShop.getLimit();
		Integer sellLimit = sellToShop == null ? null : sellToShop.getLimit();
		return new ShopStockEntry(type, count, buyPrice, sellPrice, buyLimit, sellLimit);
	}
}
