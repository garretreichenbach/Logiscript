package luamade.lua.galaxy;

import luamade.lua.data.Vec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;

/**
 * One faction-claimed star system. Obtained via {@link Galaxy#getClaimedSystems(Vec3i, Integer)}.
 */
public class SystemClaim extends LuaMadeUserdata {

	private final Vector3i systemPos;
	private final int factionId;

	public SystemClaim(Vector3i systemPos, int factionId) {
		this.systemPos = systemPos;
		this.factionId = factionId;
	}

	/** Absolute system-grid position of the claimed system, as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getSystemPos() {
		return new Vec3i(systemPos);
	}

	/** Owning faction id. */
	@LuaMadeCallable
	public Integer getFactionId() {
		return factionId;
	}

	/** Owning {@code Faction}. */
	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(factionId);
	}
}
