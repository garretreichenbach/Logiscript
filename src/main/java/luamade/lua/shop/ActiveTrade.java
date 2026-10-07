package luamade.lua.shop;

import luamade.lua.data.Vec3i;
import luamade.lua.element.inventory.ItemStack;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.trade.TradeActive;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;

/**
 * A Trading Guild shipment currently in flight between two trade nodes.
 * Snapshot of the server's active-trade list at the time it was read.
 */
public class ActiveTrade extends LuaMadeUserdata {

	private final TradeActive trade;

	public ActiveTrade(TradeActive trade) {
		this.trade = trade;
	}

	@LuaMadeCallable
	public Long getFromDbId() {
		return trade.getFromId();
	}

	@LuaMadeCallable
	public Long getToDbId() {
		return trade.getToId();
	}

	@LuaMadeCallable
	public String getFromStation() {
		return trade.getFromStation();
	}

	@LuaMadeCallable
	public String getToStation() {
		return trade.getToStation();
	}

	@LuaMadeCallable
	public Integer getFromFactionId() {
		return trade.getFromFactionId();
	}

	@LuaMadeCallable
	public Integer getToFactionId() {
		return trade.getToFactionId();
	}

	@LuaMadeCallable
	public ItemStack[] getItems() {
		ElementCountMap blocks = trade.getBlocks();
		ArrayList<ItemStack> out = new ArrayList<>();
		for(short type : ElementKeyMap.keySet) {
			int count = blocks.get(type);
			if(count > 0) out.add(new ItemStack(type, count));
		}
		return out.toArray(new ItemStack[0]);
	}

	@LuaMadeCallable
	public Long getBlockPrice() {
		return trade.getBlockPrice();
	}

	@LuaMadeCallable
	public Long getDeliveryPrice() {
		return trade.getDeliveryPrice();
	}

	@LuaMadeCallable
	public Double getVolume() {
		return trade.getVolume();
	}

	/** Server time (ms) the shipment departed. */
	@LuaMadeCallable
	public Long getStartTime() {
		return trade.getStartTime();
	}

	@LuaMadeCallable
	public Long getEstimatedDuration() {
		return trade.getEstimatedDuration();
	}

	@LuaMadeCallable
	public Vec3i getCurrentSector() {
		return trade.getCurrentSector() == null ? null : new Vec3i(trade.getCurrentSector());
	}

	@LuaMadeCallable
	public Vec3i getStartSystem() {
		return new Vec3i(trade.getStartSystem());
	}

	@LuaMadeCallable
	public Vec3i getTargetSystem() {
		return new Vec3i(trade.getTargetSystem());
	}

	@LuaMadeCallable
	public Boolean isCargoLoaded() {
		return trade.isCargoLoaded();
	}
}
