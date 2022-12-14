package luamade.lua.entity;

import api.common.GameServer;
import api.utils.game.SegmentControllerUtils;
import com.bulletphysics.linearmath.Transform;
import luamade.lua.Faction;
import luamade.lua.LuaVec3i;
import luamade.lua.element.block.Block;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.element.system.module.Thrust;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.element.system.shield.ShieldSystem;
import luamade.lua.element.system.shipyard.Shipyard;
import luamade.lua.entity.ai.EntityAI;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.*;
import org.schema.game.common.controller.elements.ElementCollectionManager;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.controller.elements.cloaking.StealthAddOn;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.network.objects.Sendable;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos.getX(), pos.getY(), pos.getZ()));
	}

	@LuaMadeCallable
	public EntityAI getAI() {
		return new EntityAI(segmentController);
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
	public Faction getSystemOwner() {
		try {
			return new Faction(GameServer.getUniverse().getStellarSystemFromSecPos(segmentController.getSector(new Vector3i())).getOwnerFaction());
		} catch(Exception exception) {
			return null;
		}
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
				if(controller.railController.getRoot().equals(segmentController.railController.getRoot())) continue;
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
	public Boolean isEntityDocked(RemoteEntity entity) {
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.getId() == entity.getId()) return true;
		}
		return false;
	}

	@LuaMadeCallable
	public void undockEntity(RemoteEntity entity) {
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.getId() == entity.getId()) {
				controller.railController.disconnect();
				return;
			}
		}
	}

	@LuaMadeCallable
	public void undockAll() {
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController)) controller.railController.disconnect();
		}
	}

	@LuaMadeCallable
	public void dockTo(RemoteEntity entity, Block railDocker) {
		if(!segmentController.getSector(new Vector3i()).equals(entity.getSegmentController().getSector(new Vector3i())) || isEntityDocked(entity) || segmentController.railController.getRoot().equals(entity.getSegmentController().railController.getRoot())) return;
		if(segmentController.getFactionId() == 0 || entity.getSegmentController().getFactionId() == 0) return;
		if(getFaction().isSameFaction(entity.getFaction()) || getFaction().isFriend(entity.getFaction())) {
			HashMap<Block, Double> distances = new HashMap<>();
			int searchRadius = 20;
			SegmentBufferInterface thisBuffer = segmentController.getSegmentBuffer();
			SegmentBufferInterface remoteBuffer = entity.getSegmentController().getSegmentBuffer();
			SegmentPiece dockerPiece = thisBuffer.getPointUnsave(railDocker.getPos().getX(), railDocker.getPos().getY(), railDocker.getPos().getZ());
			if(dockerPiece != null) {
				Transform transform = new Transform();
				dockerPiece.getTransform(transform);
				//Go through all the blocks on the remote entity that have a distance from the docker block that is less than the search radius
				Vector3i posTemp = new Vector3i();
				for(int x = -searchRadius; x <= searchRadius; x++) {
					for(int y = -searchRadius; y <= searchRadius; y++) {
						for(int z = -searchRadius; z <= searchRadius; z++) {
							posTemp.set(x, y, z);
							if(remoteBuffer.existsPointUnsave(posTemp.x, posTemp.y, posTemp.z)) {
								SegmentPiece piece = remoteBuffer.getPointUnsave(posTemp.x, posTemp.y, posTemp.z);
								if(piece != null && (piece.getType() == ElementKeyMap.RAIL_BLOCK_BASIC || piece.getType() == ElementKeyMap.RAIL_BLOCK_CW || piece.getType() == ElementKeyMap.RAIL_BLOCK_CCW || piece.getType() == ElementKeyMap.RAIL_LOAD || piece.getType() == ElementKeyMap.RAIL_UNLOAD || piece.getType() == ElementKeyMap.RAIL_BLOCK_TURRET_Y_AXIS)) {
									Transform remoteTransform = new Transform();
									piece.getTransform(remoteTransform);
									Vector3f remotePos = remoteTransform.origin;
									Vector3f dockerPos = transform.origin;
									double distance = Math.sqrt(Math.pow(remotePos.x - dockerPos.x, 2) + Math.pow(remotePos.y - dockerPos.y, 2) + Math.pow(remotePos.z - dockerPos.z, 2));
									if(distance <= searchRadius) {
										Block block = new Block(piece);
										distances.put(block, distance);
									}
								}
							}
						}
					}
				}
				//Find the closest block to the docker block
				Block closestBlock = null;
				double closestDistance = 0;
				for(Block block : distances.keySet()) {
					if(closestBlock == null) {
						closestBlock = block;
						closestDistance = distances.get(block);
					} else {
						if(distances.get(block) < closestDistance) {
							closestBlock = block;
							closestDistance = distances.get(block);
						}
					}
				}
				if(closestBlock != null) segmentController.railController.connectServer(railDocker.getSegmentPiece(), closestBlock.getSegmentPiece());
			}
		}
	}

	@LuaMadeCallable
	public void dockTo(RemoteEntity entity, Block railDocker, LuaVec3i dockPos) {
		if(!segmentController.getSector(new Vector3i()).equals(entity.getSegmentController().getSector(new Vector3i())) || isEntityDocked(entity) || segmentController.railController.getRoot().equals(entity.getSegmentController().railController.getRoot())) return;
		if(segmentController.getFactionId() == 0 || entity.getSegmentController().getFactionId() == 0) return;
		if(getFaction().isSameFaction(entity.getFaction()) || getFaction().isFriend(entity.getFaction())) {
			HashMap<Block, Double> distances = new HashMap<>();
			int searchRadius = 20;
			SegmentBufferInterface thisBuffer = segmentController.getSegmentBuffer();
			SegmentBufferInterface remoteBuffer = entity.getSegmentController().getSegmentBuffer();
			SegmentPiece dockerPiece = thisBuffer.getPointUnsave(railDocker.getPos().getX(), railDocker.getPos().getY(), railDocker.getPos().getZ());
			SegmentPiece dockPiece = remoteBuffer.getPointUnsave(dockPos.getX(), dockPos.getY(), dockPos.getZ());
			if(dockerPiece != null && dockPiece != null) {
				Transform transform = new Transform();
				dockerPiece.getTransform(transform);
				Transform remoteTransform = new Transform();
				dockPiece.getTransform(remoteTransform);
				Vector3f remotePos = remoteTransform.origin;
				Vector3f dockerPos = transform.origin;
				double distance = Math.sqrt(Math.pow(remotePos.x - dockerPos.x, 2) + Math.pow(remotePos.y - dockerPos.y, 2) + Math.pow(remotePos.z - dockerPos.z, 2));
				if(distance <= searchRadius) segmentController.railController.connectServer(dockerPiece, dockPiece);
			}
		}
	}

	@LuaMadeCallable
	public Double getSpeed() {
		return (double) segmentController.getSpeedCurrent();
	}

	@LuaMadeCallable
	public Double getMass() {
		return (double) segmentController.getMass();
	}

	@LuaMadeCallable
	public Boolean isJamming() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
			return playerUsable != null && playerUsable.isActive();
		}
		return false;
	}

	@LuaMadeCallable
	public Boolean canJam() {
		if(!isJamming()) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
				return playerUsable != null && playerUsable.canExecute();
			}
		}
		return false;
	}

	@LuaMadeCallable
	public void activateJamming(Boolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
			if(playerUsable != null) {
				if(active) if(playerUsable.canExecute()) playerUsable.executeModule();
				else if(playerUsable.isActive()) playerUsable.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public Boolean isCloaking() {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
			return playerUsable != null && playerUsable.isActive();
		}
		return false;
	}

	@LuaMadeCallable
	public Boolean canCloak() {
		if(!isCloaking()) {
			if(segmentController instanceof Ship) {
				Ship ship = (Ship) segmentController;
				StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
				return playerUsable != null && playerUsable.canExecute();
			}
		}
		return false;
	}

	@LuaMadeCallable
	public void activateCloaking(Boolean active) {
		if(segmentController instanceof Ship) {
			Ship ship = (Ship) segmentController;
			StealthAddOn playerUsable = SegmentControllerUtils.getAddon(ship, StealthAddOn.class);
			if(playerUsable != null) {
				if(active) if(playerUsable.canExecute()) playerUsable.executeModule();
				else if(playerUsable.isActive()) playerUsable.onRevealingAction();
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

	@LuaMadeCallable
	public String getEntityType() {
		return segmentController.getTypeString();
	}

	@LuaMadeCallable
	public Inventory getNamedInventory(String name) {
		if(segmentController instanceof Ship) return getInventory(name, ((Ship) segmentController).getManagerContainer());
		else if(segmentController instanceof SpaceStation) return getInventory(name, ((SpaceStation) segmentController).getManagerContainer());
		else return null;
	}

	public SegmentController getSegmentController() {
		return segmentController;
	}

	private static Inventory getInventory(String name, ManagerContainer<?> managerContainer) {
		for(Map.Entry<Long, org.schema.game.common.data.player.inventory.Inventory> entry : managerContainer.getInventories().entrySet()) {
			if(entry.getValue().getCustomName().equals(name)) return new Inventory(entry.getValue(), managerContainer.getSegmentController().getSegmentBuffer().getPointUnsave(entry.getKey()));
		}
		return null;
	}
}
