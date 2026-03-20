package luamade.lua.entity;

import api.common.GameServer;
import api.utils.game.SegmentControllerUtils;
import com.bulletphysics.linearmath.Transform;
import luamade.LuaMade;
import luamade.lua.data.Vec3f;
import luamade.lua.data.Vec3i;
import luamade.lua.element.block.Block;
import luamade.lua.element.inventory.Inventory;
import luamade.lua.element.system.module.Thrust;
import luamade.lua.element.system.reactor.Reactor;
import luamade.lua.element.system.shield.ShieldSystem;
import luamade.lua.element.system.shipyard.Shipyard;
import luamade.lua.entity.ai.EntityAI;
import luamade.lua.entity.ai.Fleet;
import luamade.lua.faction.Faction;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentBufferInterface;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.elements.ElementCollectionManager;
import org.schema.game.common.controller.elements.ManagerContainer;
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
		segmentController = controller;
	}

	public static Entity wrap(SegmentController controller) {
		if(controller == null) return null;
		if(controller instanceof org.schema.game.common.controller.Ship) return new Ship(controller);
		if(controller instanceof SpaceStation) return new Station(controller);
		return new Entity(controller);
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
	public Block getBlockAt(Vec3i pos) {
		return Block.wrap(segmentController.getSegmentBuffer().getPointUnsave(pos.getX(), pos.getY(), pos.getZ()));
	}

	@LuaMadeCallable
	public EntityAI getAI() {
		return new EntityAI(segmentController);
	}

	@LuaMadeCallable
	public Vec3f getPos() {
		return new Vec3f(segmentController.getWorldTransform().origin);
	}

	@LuaMadeCallable
	public Vec3i getSector() {
		return new Vec3i(segmentController.getSector(new Vector3i()));
	}

	@LuaMadeCallable
	public Vec3i getSystem() {
		return new Vec3i(segmentController.getSystem(new Vector3i()));
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
				if(controller instanceof org.schema.game.common.controller.Ship) {
					org.schema.game.common.controller.Ship ship = (org.schema.game.common.controller.Ship) controller;
					if(ship.getManagerContainer().isJamming() || ship.getManagerContainer().isCloaked()) {
						continue;
					}
				}
				if(controller.railController.getRoot().equals(segmentController.railController.getRoot())) {
					continue;
				}
				Vector3i sector = controller.getSector(new Vector3i());
				Vector3i diff = new Vector3i(thisSector);
				diff.sub(sector);
				diff.absolute();
				if(diff.x <= 1 && diff.y <= 1 && diff.z <= 1 && controller.getId() != getId())
					entities.add(new RemoteEntity(controller));
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
				if(controller instanceof org.schema.game.common.controller.Ship) {
					org.schema.game.common.controller.Ship ship = (org.schema.game.common.controller.Ship) controller;
					if(ship.getManagerContainer().isJamming() || ship.getManagerContainer().isCloaked()) {
						continue;
					}
				}
				Vector3i sector = controller.getSector(new Vector3i());
				Vector3i diff = new Vector3i(thisSector);
				diff.sub(sector);
				diff.absolute();
				if(diff.x <= radius && diff.y <= radius && diff.z <= radius && controller.getId() != getId()) {
					entities.add(new RemoteEntity(controller));
				}
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
			if(controller.railController.isChildDock(segmentController) && controller.railController.isTurretDocked()) {
				turrets.add(wrap(controller));
			}
		}
		return turrets.toArray(new Entity[0]);
	}

	@LuaMadeCallable
	public Entity[] getDocked() {
		ArrayList<Entity> docked = new ArrayList<>();
		ArrayList<SegmentController> dockedControllers = new ArrayList<>();
		segmentController.railController.getDockedRecusive(dockedControllers);
		for(SegmentController controller : dockedControllers) {
			if(controller.railController.isChildDock(segmentController)) {
				docked.add(wrap(controller));
			}
		}
		return docked.toArray(new Entity[0]);
	}

	@LuaMadeCallable
	public Entity[] getDockedEntities() {
		return getDocked();
	}

	@LuaMadeCallable
	public Entity getParent() {
		if(segmentController.railController == null) return null;
		for(Sendable sendable : segmentController.getState().getLocalAndRemoteObjectContainer().getLocalObjects().values()) {
			if(sendable instanceof SegmentController) {
				SegmentController controller = (SegmentController) sendable;
				if(controller.getId() != segmentController.getId() && segmentController.railController.isChildDock(controller)) {
					return wrap(controller);
				}
			}
		}
		return null;
	}

	@LuaMadeCallable
	public Entity getRoot() {
		if(segmentController.railController == null || segmentController.railController.getRoot() == null) return null;
		return wrap(segmentController.railController.getRoot());
	}

	@LuaMadeCallable
	public Boolean isEntityDocked(RemoteEntity entity) {
		ArrayList<SegmentController> docked = new ArrayList<>();
		segmentController.railController.getDockedRecusive(docked);
		for(SegmentController controller : docked) {
			if(controller.railController.isChildDock(segmentController) && controller.getId() == entity.getId()) {
				return true;
			}
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
		if(!canDockWith(entity)) return;
		int searchRadius = ConfigManager.getDockingSnapRadius();
		if(hasDockingPermission(entity)) {
			HashMap<Block, Double> distances = new HashMap<>();
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
										Block block = Block.wrap(piece);
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
				if(closestBlock != null)
					segmentController.railController.connectServer(railDocker.getSegmentPiece(), closestBlock.getSegmentPiece());
			}
		}
	}

	@LuaMadeCallable
	public void dockTo(RemoteEntity entity, Block railDocker, Vec3i dockPos) {
		if(!canDockWith(entity)) return;
		int searchRadius = ConfigManager.getDockingSnapRadius();
		if(hasDockingPermission(entity)) {
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
	public void dockToNearestLoadDock(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.RAIL_LOAD);
	}

	@LuaMadeCallable
	public void dockToNearestUnloadDock(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.RAIL_UNLOAD);
	}

	@LuaMadeCallable
	public void dockToNearestBasicRail(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.RAIL_BLOCK_BASIC);
	}

	@LuaMadeCallable
	public void dockToNearestDockerRail(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.RAIL_BLOCK_DOCKER);
	}

	@LuaMadeCallable
	public void dockToNearestPickupArea(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.PICKUP_AREA);
	}

	@LuaMadeCallable
	public void dockToNearestPickupRail(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.PICKUP_RAIL);
	}

	@LuaMadeCallable
	public void dockToNearestExitShootRail(RemoteEntity station, Block railDocker) {
		dockToRailType(station, railDocker, ElementKeyMap.EXIT_SHOOT_RAIL);
	}

	private void dockToRailType(RemoteEntity station, Block railDocker, short railType) {
		if(!canDockWith(station)) return;
		if(!hasDockingPermission(station)) return;
		int searchRadius = ConfigManager.getDockingSnapRadius();
		SegmentBufferInterface thisBuffer = segmentController.getSegmentBuffer();
		SegmentBufferInterface remoteBuffer = station.getSegmentController().getSegmentBuffer();
		SegmentPiece dockerPiece = thisBuffer.getPointUnsave(railDocker.getPos().getX(), railDocker.getPos().getY(), railDocker.getPos().getZ());
		if(dockerPiece == null) return;
		Transform transform = new Transform();
		dockerPiece.getTransform(transform);
		Block closestBlock = null;
		double closestDistance = Double.MAX_VALUE;
		Vector3i posTemp = new Vector3i();
		for(int x = -searchRadius; x <= searchRadius; x++) {
			for(int y = -searchRadius; y <= searchRadius; y++) {
				for(int z = -searchRadius; z <= searchRadius; z++) {
					posTemp.set(x, y, z);
					if(remoteBuffer.existsPointUnsave(posTemp.x, posTemp.y, posTemp.z)) {
						SegmentPiece piece = remoteBuffer.getPointUnsave(posTemp.x, posTemp.y, posTemp.z);
						if(piece != null && piece.getType() == railType) {
							Transform remoteTransform = new Transform();
							piece.getTransform(remoteTransform);
							Vector3f remotePos = remoteTransform.origin;
							Vector3f dockerPos = transform.origin;
							double distance = Math.sqrt(Math.pow(remotePos.x - dockerPos.x, 2) + Math.pow(remotePos.y - dockerPos.y, 2) + Math.pow(remotePos.z - dockerPos.z, 2));
							if(distance < closestDistance && distance <= searchRadius) {
								closestBlock = Block.wrap(piece);
								closestDistance = distance;
							}
						}
					}
				}
			}
		}
		if(closestBlock != null)
			segmentController.railController.connectServer(dockerPiece, closestBlock.getSegmentPiece());
	}

	private boolean canDockWith(RemoteEntity entity) {
		if(entity == null || entity.getSegmentController() == null || segmentController.railController == null || entity.getSegmentController().railController == null) {
			return false;
		}
		if(!segmentController.getSector(new Vector3i()).equals(entity.getSegmentController().getSector(new Vector3i()))) {
			return false;
		}
		if(isEntityDocked(entity)) {
			return false;
		}
		return !segmentController.railController.getRoot().equals(entity.getSegmentController().railController.getRoot());
	}

	private boolean hasDockingPermission(RemoteEntity entity) {
		if(!ConfigManager.isDockingPermissionRequired()) {
			return true;
		}
		if(segmentController.getFactionId() == 0 || entity.getSegmentController().getFactionId() == 0) {
			return false;
		}
		if(getFaction().isSameFaction(entity.getFaction())) {
			return true;
		}
		return ConfigManager.isDockingFriendFactionsAllowed() && getFaction().isFriend(entity.getFaction());
	}

	@LuaMadeCallable
	public Double getSpeed() {
		return (double) segmentController.getSpeedCurrent();
	}

	@LuaMadeCallable
	public Vec3f getHeading() {
		Vector3f forward = new Vector3f(
				segmentController.getWorldTransform().basis.m02,
				segmentController.getWorldTransform().basis.m12,
				segmentController.getWorldTransform().basis.m22
		);
		if(forward.lengthSquared() == 0) return new Vec3f(0, 0, 0);
		forward.normalize();
		return new Vec3f(forward);
	}

	@LuaMadeCallable
	public Double getMass() {
		return (double) segmentController.getMass();
	}

	@LuaMadeCallable
	public ShieldSystem getShieldSystem() {
		return new ShieldSystem(segmentController);
	}

	@LuaMadeCallable
	public Shipyard[] getShipyards() {
		ArrayList<Shipyard> shipyards = new ArrayList<>();
		if(segmentController instanceof ManagedUsableSegmentController<?>) {
			for(ElementCollectionManager<?, ?, ?> manager : SegmentControllerUtils.getCollectionManagers((ManagedUsableSegmentController<?>) segmentController, ShipyardCollectionManager.class)) {
				shipyards.add(new Shipyard(segmentController, (ShipyardCollectionManager) manager));
			}
		}
		return shipyards.toArray(new Shipyard[0]);
	}

	@LuaMadeCallable
	public String getEntityType() {
		return segmentController.getTypeString();
	}

	@LuaMadeCallable
	public Inventory getNamedInventory(String name) {
		if(segmentController instanceof org.schema.game.common.controller.Ship) {
			return getInventory(name, ((org.schema.game.common.controller.Ship) segmentController).getManagerContainer());
		} else if(segmentController instanceof SpaceStation) {
			return getInventory(name, ((SpaceStation) segmentController).getManagerContainer());
		} else {
			return null;
		}
	}

	@LuaMadeCallable
	public Boolean isInFleet() {
		return getFleet() != null;
	}

	@LuaMadeCallable
	public Fleet getFleet() {
		try {
			if(segmentController.isInFleet()) return new Fleet(segmentController.getFleet());
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Error getting fleet for entity ID " + getId(), exception);
		}
		return null;
	}

	public SegmentController getSegmentController() {
		return segmentController;
	}

	private static Inventory getInventory(String name, ManagerContainer<?> managerContainer) {
		for(Map.Entry<Long, org.schema.game.common.data.player.inventory.Inventory> entry : managerContainer.getInventories().entrySet()) {
			if(entry.getValue().getCustomName().equals(name)) {
				return new Inventory(entry.getValue(), managerContainer.getSegmentController().getSegmentBuffer().getPointUnsave(entry.getKey()));
			}
		}
		return null;
	}
}
