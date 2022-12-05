package luamade.lua;

import api.common.GameCommon;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Faction extends LuaMadeUserdata {

	private final int factionId;

	public Faction(int factionId) {
		this.factionId = factionId;
	}

	@LuaCallable
	public String getName() {
		if(factionId != 0) return GameCommon.getGameState().getFactionManager().getFactionName(factionId);
		else return "Neutral";
	}

	@LuaCallable
	public Boolean isSameFaction(Faction faction) {
		return factionId == faction.factionId;
	}

	@LuaCallable
	public Boolean isFriend(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isFriend(factionId, faction.factionId);
	}

	@LuaCallable
	public Boolean isEnemy(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isEnemy(factionId, faction.factionId);
	}

	@LuaCallable
	public Boolean isNeutral(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isNeutral(factionId, faction.factionId);
	}
}
