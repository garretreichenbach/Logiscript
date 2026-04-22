package luamade.lua.shop;

import luamade.lua.data.Vec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.trade.TradeNodeStub;

import java.util.Set;

/**
 * Lightweight snapshot of a trade node (a shop registered in the galaxy trade network).
 * Not live — data reflects the network's most recent cache.
 */
public class TradeNodeInfo extends LuaMadeUserdata {

	private final TradeNodeStub stub;

	public TradeNodeInfo(TradeNodeStub stub) {
		this.stub = stub;
	}

	@LuaMadeCallable
	public Long getEntityDbId() {
		return stub.getEntityDBId();
	}

	@LuaMadeCallable
	public String getStationName() {
		return stub.getStationName();
	}

	@LuaMadeCallable
	public Integer getFactionId() {
		return stub.getFactionId();
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(stub.getFactionId());
	}

	@LuaMadeCallable
	public Long getCredits() {
		return stub.getCredits();
	}

	@LuaMadeCallable
	public Vec3i getSystem() {
		Vector3i s = stub.getSystem();
		return s == null ? null : new Vec3i(s);
	}

	@LuaMadeCallable
	public Vec3i getSector() {
		Vector3i s = stub.getSector();
		return s == null ? null : new Vec3i(s);
	}

	@LuaMadeCallable
	public Double getVolume() {
		return stub.getVolume();
	}

	@LuaMadeCallable
	public Double getCapacity() {
		return stub.getCapacity();
	}

	@LuaMadeCallable
	public Long getTradePermission() {
		return stub.getTradePermission();
	}

	@LuaMadeCallable
	public String[] getOwners() {
		Set<String> owners = stub.getOwners();
		if(owners == null) return new String[0];
		return owners.toArray(new String[0]);
	}

	public TradeNodeStub getStub() {
		return stub;
	}
}
