package luamade.lua.entity.ai;

import luamade.LuaMade;
import luamade.lua.data.LuaVec3i;
import luamade.lua.entity.RemoteEntity;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.FleetCommandTypes;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Fleet extends LuaMadeUserdata {

	private final org.schema.game.common.data.fleet.Fleet fleet;

	public Fleet(org.schema.game.common.data.fleet.Fleet fleet) {
		this.fleet = fleet;
	}

	@LuaMadeCallable
	public Long getId() {
		return fleet.dbid;
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(fleet.getFactionId());
	}

	@LuaMadeCallable
	public String getName() {
		return fleet.getName();
	}

	@LuaMadeCallable
	public LuaVec3i getSector() {
		return new LuaVec3i(Vector3i.parseVector3i(fleet.getFlagShipSector()));
	}

	@LuaMadeCallable
	public RemoteEntity getFlagship() {
		return new RemoteEntity(fleet.getFlagShip().getLoaded());
	}

	@LuaMadeCallable
	public RemoteEntity[] getMembers() {
		RemoteEntity[] members = new RemoteEntity[fleet.getMembers().size()];
		for(int i = 0; i < fleet.getMembers().size(); i ++) members[i] = new RemoteEntity(fleet.getMembers().get(i).getLoaded());
		return members;
	}

	@LuaMadeCallable
	public void addMember(RemoteEntity entity) {
		fleet.addMemberFromEntity(entity.getSegmentController());
	}

	@LuaMadeCallable
	public void removeMember(RemoteEntity entity) {
		fleet.removeMemberByEntity(entity.getSegmentController());
	}

	@LuaMadeCallable
	public FleetCommand getCurrentCommand() {
		try {
			Field field = fleet.getClass().getDeclaredField("combatSetting");
			field.setAccessible(true);
			org.schema.game.network.objects.remote.FleetCommand command = (org.schema.game.network.objects.remote.FleetCommand) field.get(fleet);
			assert command != null;
			return new FleetCommand(command);
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to get current fleet command", exception);
			return null;
		}
	}

	@LuaMadeCallable
	public void setCurrentCommand(String command, Object... args) {
		fleet.sendFleetCommand(FleetCommandTypes.valueOf(command.toUpperCase(Locale.ROOT)),  args);
	}
}
