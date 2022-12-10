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
	public Faction[] getFriends() {
		org.schema.game.common.data.player.faction.Faction[] friends = GameCommon.getGameState().getFactionManager().getFaction(factionId).getFriends().toArray(new org.schema.game.common.data.player.faction.Faction[0]);
		Faction[] friendFactions = new Faction[friends.length];
		for(int i = 0; i < friends.length; i++) friendFactions[i] = new Faction(friends[i].getIdFaction());
		return friendFactions;
	}

	@LuaMadeCallable
	public Boolean isEnemy(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isEnemy(factionId, faction.factionId);
	}

	@LuaMadeCallable
	public Faction[] getEnemies() {
		org.schema.game.common.data.player.faction.Faction[] enemies = GameCommon.getGameState().getFactionManager().getFaction(factionId).getEnemies().toArray(new org.schema.game.common.data.player.faction.Faction[0]);
		Faction[] enemyFactions = new Faction[enemies.length];
		for(int i = 0; i < enemies.length; i++) enemyFactions[i] = new Faction(enemies[i].getIdFaction());
		return enemyFactions;
	}

	@LuaMadeCallable
	public Boolean isNeutral(Faction faction) {
		return GameCommon.getGameState().getFactionManager().isNeutral(factionId, faction.factionId);
	}
}
