package luamade.lua.entity;

import luamade.lua.Faction;
import luamade.lua.element.block.Block;
import luamade.lua.element.system.module.ThrustModule;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.entity.ai.EntityAI;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.schine.network.objects.Sendable;

import java.util.ArrayList;

public class Entity extends LuaMadeUserdata {
	private final SegmentController segmentController;

	public Entity(SegmentController controller) {
		this.segmentController = controller;
	}

	@LuaCallable
	public Integer getId() {
		return segmentController.getId();
	}

	@LuaCallable
	public String getName() {
		return segmentController.getRealName();
	}

	@LuaCallable
	public void setName(String name) {
		segmentController.setRealName(name);
	}

	@LuaCallable
	public Block getBlockAt(Integer[] pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
	}

	@LuaCallable
	public Block getBlockAt(Integer x, Integer y, Integer z) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(x, y, z));
	}

	@LuaCallable
	public EntityAI getAI() {
		return new EntityAI(segmentController);
	}

	@LuaCallable
	public Integer[] getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new Integer[] {sector.x, sector.y, sector.z};
	}

	@LuaCallable
	public Integer[] getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new Integer[] {system.x, system.y, system.z};
	}

	@LuaCallable
	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

	@LuaCallable
	public RemoteEntity[] getNearbyEntities() {
		ArrayList<RemoteEntity> entities = new ArrayList<>();
		Vector3i thisSector = segmentController.getSector(new Vector3i());
		for(Sendable sendable : segmentController.getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
			if(sendable instanceof SegmentController) {
				SegmentController controller = (SegmentController) sendable;
				if(controller instanceof Ship) {
					Ship ship = (Ship) controller;
					if(ship.getManagerContainer().isJamming() || ship.getManagerContainer().isCloaked()) continue;
				}
				Vector3i sector = controller.getSector(new Vector3i());
				Vector3i diff = new Vector3i(thisSector);
				diff.sub(sector);
				diff.absolute();
				if(diff.x <= 1 && diff.y <= 1 && diff.z <= 1 && controller.getId() != getId()) entities.add(new RemoteEntity(controller));
			}
		}
		return entities.toArray(new RemoteEntity[0]);
	}

	@LuaCallable
	public RemoteEntity[] getNearbyEntities(int radius) {
		ArrayList<RemoteEntity> entities = new ArrayList<>();
		Vector3i thisSector = segmentController.getSector(new Vector3i());
		for(Sendable sendable : segmentController.getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
			if(sendable instanceof SegmentController) {
				SegmentController controller = (SegmentController) sendable;
				if(controller instanceof Ship) {
					Ship ship = (Ship) controller;
					if(ship.getManagerContainer().isJamming() || ship.getManagerContainer().isCloaked()) continue;
				}
				Vector3i sector = controller.getSector(new Vector3i());
				Vector3i diff = new Vector3i(thisSector);
				diff.sub(sector);
				diff.absolute();
				if(diff.x <= radius && diff.y <= radius && diff.z <= radius && controller.getId() != getId()) entities.add(new RemoteEntity(controller));
			}
		}
		return entities.toArray(new RemoteEntity[0]);
	}

	@LuaCallable
	public Boolean hasReactor() {
		return getMaxReactorHP() > 0;
	}

	@LuaCallable
	public Reactor getReactor() {
		return new Reactor(segmentController);
	}

	@LuaCallable
	public Long getMaxReactorHP() {
		return getReactor().getMaxHP();
	}

	@LuaCallable
	public Long getReactorHP() {
		return getReactor().getHP();
	}

	@LuaCallable
	public ThrustModule getThrustModule() {
		return new ThrustModule(segmentController);
	}

	@LuaCallable
	public Entity[] getTurrets() {
		ArrayList<Entity> turrets = new ArrayList<>();
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.railController.isTurretDocked()) turrets.add(new Entity(controller));
		}
		return turrets.toArray(new Entity[0]);
	}

	@LuaCallable
	public Entity[] getDocked() {
		ArrayList<Entity> docked = new ArrayList<>();
		ArrayList<SegmentController> dockedControllers = new ArrayList<>();
		segmentController.railController.getDockedRecusive(dockedControllers);
		for(SegmentController controller : dockedControllers) {
			if(controller.railController.isChildDock(segmentController)) docked.add(new Entity(controller));
		}
		return docked.toArray(new Entity[0]);
	}

	@LuaCallable
	public Float getSpeed() {
		return segmentController.getSpeedCurrent();
	}
}
