package luamade.lua;

import api.common.GameCommon;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Faction {

	private final int factionId;

	public Faction(int factionId) {
		this.factionId = factionId;
	}

	public String getName() {
		if(factionId != 0) return GameCommon.getGameState().getFactionManager().getFactionName(factionId);
		else return "Neutral";
	}

	public boolean isSameFaction(Faction faction) {
		return factionId == faction.factionId;
	}

	public boolean isFriend(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isFriend(factionId, faction.factionId);
	}

	public boolean isEnemy(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isEnemy(factionId, faction.factionId);
	}

	public boolean isNeutral(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isNeutral(factionId, faction.factionId);
	}
}
