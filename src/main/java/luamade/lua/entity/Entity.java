package luamade.lua.entity;

import api.utils.game.SegmentControllerUtils;
import luamade.lua.Faction;
import luamade.lua.LuaVec3i;
import luamade.lua.element.block.Block;
import luamade.lua.element.system.module.Thrust;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.element.system.shield.ShieldSystem;
import luamade.lua.element.system.shipyard.Shipyard;
import luamade.lua.entity.ai.EntityAI;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.PlayerUsableInterface;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ElementCollectionManager;
import org.schema.game.common.controller.elements.cloaking.StealthAddOn;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.schine.network.objects.Sendable;

import java.util.ArrayList;

public class Entity extends LuaMadeUserdata {
	private final SegmentController segmentController;

	public Entity(SegmentController controller) {
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
	public void setName(String name) {
		segmentController.setRealName(name);
	}

	@LuaMadeCallable
	public Block getBlockAt(LuaVec3i pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos.x(), pos.y(), pos.z()));
	}

	@LuaMadeCallable
	public EntityAI getAI() {
		return new EntityAI(segmentController);
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

	@LuaMadeCallable
	public RemoteEntity[] getNearbyEntities(Integer radius) {
		radius = Math.min(Math.abs(radius), 3);
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

	@LuaMadeCallable
	public Boolean hasReactor() {
		return getMaxReactorHP() > 0;
	}

	@LuaMadeCallable
	public Reactor getReactor() {
		return new Reactor(segmentController);
	}

	@LuaMadeCallable
	public Long getMaxReactorHP() {
		return getReactor().getMaxHP();
	}

	@LuaMadeCallable
	public Long getReactorHP() {
		return getReactor().getHP();
	}

	@LuaMadeCallable
	public Thrust getThrust() {
		return new Thrust(segmentController);
	}

	@LuaMadeCallable
	public Entity[] getTurrets() {
		ArrayList<Entity> turrets = new ArrayList<>();
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.railController.isTurretDocked()) turrets.add(new Entity(controller));
		}
		return turrets.toArray(new Entity[0]);
	}

	@LuaMadeCallable
	public Entity[] getDocked() {
		ArrayList<Entity> docked = new ArrayList<>();
		ArrayList<SegmentController> dockedControllers = new ArrayList<>();
		segmentController.railController.getDockedRecusive(dockedControllers);
		for(SegmentController controller : dockedControllers) {
			if(controller.railController.isChildDock(segmentController)) docked.add(new Entity(controller));
		}
		return docked.toArray(new Entity[0]);
	}

	@LuaMadeCallable
	public Float getSpeed() {
		return segmentController.getSpeedCurrent();
	}

	@LuaMadeCallable
	public Float getMass() {
		return segmentController.getMass();
	}

	@LuaMadeCallable
	public Boolean isJamming() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_JAM);
			return playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).isActive();
		}
		return false;
	}

	@LuaMadeCallable
	public Boolean canJam() {
		if(!isJamming()) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_JAM);
				return playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).canExecute();
			}
		}
		return false;
	}

	@LuaMadeCallable
	public void activateJamming(Boolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_JAM);
			if(playerUsable instanceof StealthAddOn) {
				StealthAddOn stealth = (StealthAddOn) playerUsable;
				if(active) if(stealth.canExecute()) stealth.executeModule();
				else if(stealth.isActive()) stealth.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public Boolean isCloaking() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_CLOAK);
			return playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).isActive();
		}
		return false;
	}

	@LuaMadeCallable
	public Boolean canCloak() {
		if(!isCloaking()) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_CLOAK);
				return playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).canExecute();
			}
		}
		return false;
	}

	@LuaMadeCallable
	public void activateCloaking(Boolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = SegmentControllerUtils.getAddon(ship, PlayerUsableInterface.USABLE_ID_CLOAK);
			if(playerUsable instanceof StealthAddOn) {
				StealthAddOn stealth = (StealthAddOn) playerUsable;
				if(active) if(stealth.canExecute()) stealth.executeModule();
				else if(stealth.isActive()) stealth.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public ShieldSystem getShieldSystem() {
		return new ShieldSystem(segmentController);
	}

	@LuaMadeCallable
	public Shipyard[] getShipyards() {
		ArrayList<Shipyard> shipyards = new ArrayList<>();
		if(segmentController instanceof ManagedUsableSegmentController<?>) {
			for(ElementCollectionManager<?, ?, ?> manager : SegmentControllerUtils.getCollectionManagers((ManagedUsableSegmentController<?>) segmentController, ShipyardCollectionManager.class)) shipyards.add(new Shipyard(segmentController, (ShipyardCollectionManager) manager));
		}
		return shipyards.toArray(new Shipyard[0]);
	}
}
