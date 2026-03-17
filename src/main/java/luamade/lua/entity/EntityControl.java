package luamade.lua.entity;

import luamade.lua.data.LuaVec3i;
import luamade.lua.element.block.Block;
import luamade.lua.entity.ai.EntityAI;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;

public class EntityControl extends LuaMadeUserdata {
	private final Entity entity;

	public EntityControl(SegmentController segmentController) {
		this.entity = new Entity(segmentController);
	}

	@LuaMadeCallable
	public void setName(String name) {
		entity.setName(name);
	}

	@LuaMadeCallable
	public EntityAI getAI() {
		return entity.getAI();
	}

	@LuaMadeCallable
	public void undockAll() {
		entity.undockAll();
	}

	@LuaMadeCallable
	public void undockEntity(RemoteEntity remoteEntity) {
		entity.undockEntity(remoteEntity);
	}

	@LuaMadeCallable
	public void dockTo(RemoteEntity remoteEntity, Block railDocker) {
		entity.dockTo(remoteEntity, railDocker);
	}

	@LuaMadeCallable
	public void dockTo(RemoteEntity remoteEntity, Block railDocker, LuaVec3i dockPos) {
		entity.dockTo(remoteEntity, railDocker, dockPos);
	}

	@LuaMadeCallable
	public void activateJamming(Boolean active) {
		entity.activateJamming(active);
	}

	@LuaMadeCallable
	public void activateCloaking(Boolean active) {
		entity.activateCloaking(active);
	}
}
