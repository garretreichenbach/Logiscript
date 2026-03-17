package luamade.lua.entity;

import api.common.GameServer;
import com.bulletphysics.linearmath.Transform;
import luamade.lua.data.LuaVec3i;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;

public class EntityInfo extends LuaMadeUserdata {
	private final SegmentController segmentController;

	public EntityInfo(SegmentController controller) {
		this.segmentController = controller;
	}

	@LuaMadeCallable
	public Integer getId() {
		return segmentController.getId();
	}

	@LuaMadeCallable
	public String getName() {
		return segmentController.getRealName();
	}

	@LuaMadeCallable
	public LuaVec3i getPos() {
		Transform transform = segmentController.getWorldTransform();
		Vector3i pos = new Vector3i(transform.origin);
		return new LuaVec3i(pos.x, pos.y, pos.z);
	}

	@LuaMadeCallable
	public LuaVec3i getSector() {
		return new LuaVec3i(segmentController.getSector(new Vector3i()));
	}

	@LuaMadeCallable
	public LuaVec3i getSystem() {
		return new LuaVec3i(segmentController.getSystem(new Vector3i()));
	}

	@LuaMadeCallable
	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

	@LuaMadeCallable
	public Faction getSystemOwner() {
		try {
			return new Faction(GameServer.getUniverse().getStellarSystemFromSecPos(segmentController.getSector(new Vector3i())).getOwnerFaction());
		} catch(Exception exception) {
			return null;
		}
	}
}
