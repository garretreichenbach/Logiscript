package luamade.lua.entity;

import luamade.lua.Faction;
import luamade.lua.element.block.Block;
import luamade.lua.element.system.module.ThrustModule;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.entity.ai.EntityAI;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.*;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.PlayerUsableInterface;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.cloaking.StealthAddOn;
import org.schema.schine.network.objects.Sendable;

import java.util.ArrayList;

public class Entity extends LuaMadeUserdata {
	private final SegmentController segmentController;

	public Entity(SegmentController controller) {
		this.segmentController = controller;
	}

	@LuaMadeCallable
	public LuaInteger getId() {
		return LuaInteger.valueOf(segmentController.getId());
	}

	@LuaMadeCallable
	public LuaString getName() {
		return LuaString.valueOf(segmentController.getRealName());
	}

	@LuaMadeCallable
	public void setName(LuaString name) {
		segmentController.setRealName(name.tojstring());
	}

	@LuaMadeCallable
	public Block getBlockAt(LuaInteger[] pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0].v, pos[1].v, pos[2].v));
	}

	@LuaMadeCallable
	public Block getBlockAt(LuaInteger x, LuaInteger y, LuaInteger z) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(x.v, y.v, z.v));
	}

	@LuaMadeCallable
	public EntityAI getAI() {
		return new EntityAI(segmentController);
	}

	@LuaMadeCallable
	public LuaInteger[] getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new LuaInteger[] {LuaInteger.valueOf(sector.x), LuaInteger.valueOf(sector.y), LuaInteger.valueOf(sector.z)};
	}

	@LuaMadeCallable
	public LuaInteger[] getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new LuaInteger[] {LuaInteger.valueOf(system.x), LuaInteger.valueOf(system.y), LuaInteger.valueOf(system.z)};
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
				if(diff.x <= 1 && diff.y <= 1 && diff.z <= 1 && controller.getId() != getId().v) entities.add(new RemoteEntity(controller));
			}
		}
		return entities.toArray(new RemoteEntity[0]);
	}

	@LuaMadeCallable
	public RemoteEntity[] getNearbyEntities(LuaInteger radius) {
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
				if(diff.x <= radius.v && diff.y <= radius.v && diff.z <= radius.v && controller.getId() != getId().v) entities.add(new RemoteEntity(controller));
			}
		}
		return entities.toArray(new RemoteEntity[0]);
	}

	@LuaMadeCallable
	public LuaBoolean hasReactor() {
		return LuaBoolean.valueOf(getMaxReactorHP().v > 0);
	}

	@LuaMadeCallable
	public Reactor getReactor() {
		return new Reactor(segmentController);
	}

	@LuaMadeCallable
	public LuaInteger getMaxReactorHP() {
		return getReactor().getMaxHP();
	}

	@LuaMadeCallable
	public LuaInteger getReactorHP() {
		return getReactor().getHP();
	}

	@LuaMadeCallable
	public ThrustModule getThrustModule() {
		return new ThrustModule(segmentController);
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
	public LuaDouble getSpeed() {
		return (LuaDouble) LuaDouble.valueOf(segmentController.getSpeedCurrent());
	}

	@LuaMadeCallable
	public LuaBoolean isJamming() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_JAM);
			return LuaBoolean.valueOf(playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).isActive());
		}
		return LuaBoolean.valueOf(false);
	}

	@LuaMadeCallable
	public LuaBoolean canJam() {
		if(!isJamming().v) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_JAM);
				return LuaBoolean.valueOf(playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).canExecute());
			}
		}
		return LuaBoolean.valueOf(false);
	}

	@LuaMadeCallable
	public void activateJamming(LuaBoolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_JAM);
			if(playerUsable instanceof StealthAddOn) {
				StealthAddOn stealth = (StealthAddOn) playerUsable;
				if(active.v) if(stealth.canExecute()) stealth.executeModule();
				else if(stealth.isActive()) stealth.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public LuaBoolean isCloaking() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_CLOAK);
			return LuaBoolean.valueOf(playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).isActive());
		}
		return LuaBoolean.valueOf(false);
	}

	@LuaMadeCallable
	public LuaBoolean canCloak() {
		if(!isCloaking().v) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_CLOAK);
				return LuaBoolean.valueOf(playerUsable instanceof StealthAddOn && ((StealthAddOn) playerUsable).canExecute());
			}
		}
		return LuaBoolean.valueOf(false);
	}

	@LuaMadeCallable
	public void activateCloaking(LuaBoolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			PlayerUsableInterface playerUsable = ship.getManagerContainer().getPlayerUsable(PlayerUsableInterface.USABLE_ID_CLOAK);
			if(playerUsable instanceof StealthAddOn) {
				StealthAddOn stealth = (StealthAddOn) playerUsable;
				if(active.v) if(stealth.canExecute()) stealth.executeModule();
				else if(stealth.isActive()) stealth.onRevealingAction();
			}
		}
	}
}
