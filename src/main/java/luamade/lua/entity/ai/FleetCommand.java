package luamade.lua.entity.ai;

import luamade.lua.data.Vec3i;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.fleet.FleetCommandTypes;

public class FleetCommand extends LuaMadeUserdata {

	private final org.schema.game.network.objects.remote.FleetCommand fleetCommand;

	public FleetCommand(org.schema.game.network.objects.remote.FleetCommand fleetCommand) {
		this.fleetCommand = fleetCommand;
	}

	@LuaMadeCallable
	public String getCommand() {
		return FleetCommandTypes.values()[fleetCommand.getCommand()].name();
	}

	@LuaMadeCallable
	public Vec3i getTarget() {
		for(String arg : getArgs()) {
			try {
				return new Vec3i(Vector3i.parseVector3i(arg));
			} catch(Exception ignored) {}
		}
		return new Vec3i(0, 0, 0);
	}

	@LuaMadeCallable
	public String[] getArgs() {
		return (String[]) fleetCommand.getArgs();
	}

	public org.schema.game.network.objects.remote.FleetCommand getFleetCommand() {
		return fleetCommand;
	}
}
