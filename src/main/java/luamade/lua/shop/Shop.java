package luamade.lua.shop;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import luamade.lua.entity.RemoteEntity;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ShopInterface;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.game.common.data.player.inventory.ShopInventory;
import org.schema.game.network.objects.TradePriceInterface;
import org.schema.game.server.data.GameServerState;
import org.schema.schine.network.StateInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Shop extends LuaMadeUserdata {

	private final ShopInterface shop;

	public Shop(ShopInterface shop) {
		this.shop = shop;
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
		PlayerState player = lookupPlayer(playerName);
		if(player == null) return false;
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
		PlayerState player = lookupPlayer(playerName);
		if(player == null) return false;
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

	private PlayerState lookupPlayer(String name) {
		StateInterface state = shop.getState();
		if(!(state instanceof GameServerState)) return null;
		PlayerState p = ((GameServerState) state).getPlayerStatesByName().get(name);
		if(p == null) p = ((GameServerState) state).getPlayerStatesByNameLowerCase().get(name.toLowerCase());
		return p;
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
