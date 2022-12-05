package luamade.lua;

import api.common.GameCommon;
import luamade.luawrap.LuaMadeCallable;
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

	@LuaMadeCallable
	public String getName() {
		if(factionId != 0) return GameCommon.getGameState().getFactionManager().getFactionName(factionId);
		else return "Neutral";
	}

	@LuaMadeCallable
	public Boolean isSameFaction(Faction faction) {
		return factionId == faction.factionId;
	}

	@LuaMadeCallable
	public Boolean isFriend(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isFriend(factionId, faction.factionId);
	}

	@LuaMadeCallable
	public Boolean isEnemy(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isEnemy(factionId, faction.factionId);
	}

	@LuaMadeCallable
	public Boolean isNeutral(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isNeutral(factionId, faction.factionId);
	}
}
