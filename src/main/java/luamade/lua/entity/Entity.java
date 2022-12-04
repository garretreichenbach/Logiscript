package luamade.lua.entity;

import luamade.lua.Faction;
import luamade.lua.element.block.Block;
import luamade.lua.element.system.module.ThrustModule;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.entity.ai.EntityAI;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.schine.network.objects.Sendable;

import java.util.ArrayList;

public class Entity {
	private final SegmentController segmentController;

	public Entity(SegmentController controller) {
		this.segmentController = controller;
	}

	public int getId() {
		return segmentController.getId();
	}

	public String getName() {
		return segmentController.getRealName();
	}

	public void setName(String name) {
		segmentController.setRealName(name);
	}

	public Block getBlockAt(int[] pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
	}

	public Block getBlockAt(int x, int y, int z) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(x, y, z));
	}

	public EntityAI getAI() {
		return new EntityAI(segmentController);
	}

	public int[] getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new int[] {sector.x, sector.y, sector.z};
	}

	public int[] getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new int[] {system.x, system.y, system.z};
	}

	public Faction getFaction() {
		return new Faction(segmentController.getFactionId());
	}

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

	public boolean hasReactor() {
		return getMaxReactorHP() > 0;
	}

	public Reactor getReactor() {
		return new Reactor(segmentController);
	}

	public double getMaxReactorHP() {
		return getReactor().getMaxHP();
	}

	public double getReactorHP() {
		return getReactor().getHP();
	}

	public ThrustModule getThrustModule() {
		return new ThrustModule(segmentController);
	}

	public Entity[] getTurrets() {
		ArrayList<Entity> turrets = new ArrayList<>();
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.railController.isTurretDocked()) turrets.add(new Entity(controller));
		}
		return turrets.toArray(new Entity[0]);
	}

	public Entity[] getDocked() {
		ArrayList<Entity> docked = new ArrayList<>();
		ArrayList<SegmentController> dockedControllers = new ArrayList<>();
		segmentController.railController.getDockedRecusive(dockedControllers);
		for(SegmentController controller : dockedControllers) {
			if(controller.railController.isChildDock(segmentController)) docked.add(new Entity(controller));
		}
		return docked.toArray(new Entity[0]);
	}
}
