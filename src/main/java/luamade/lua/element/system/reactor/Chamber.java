package luamade.lua.element.system.reactor;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.PlayerUsableInterface;
import org.schema.game.common.controller.elements.ManagerReloadInterface;
import org.schema.game.common.controller.elements.RecharchableSingleModule;
import org.schema.game.common.controller.elements.power.reactor.tree.ReactorElement;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;
import java.util.Map;

/**
 * Lua wrapper class for ReactorElement.java
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Chamber extends LuaMadeUserdata {

	private final ReactorElement reactorElement;
	private final ManagedUsableSegmentController<?> controller;
	private final Reactor reactor;

	public Chamber(ReactorElement reactorElement, ManagedUsableSegmentController<?> segmentController, Reactor reactor) {
		this.reactorElement = reactorElement;
		this.controller = segmentController;
		this.reactor = reactor;
	}

	@LuaCallable
	public String getName() {
		return ElementKeyMap.getInfo(reactorElement.type).getName();
	}

	@LuaCallable
	public BlockInfo getBlockInfo() {
		return new BlockInfo(ElementKeyMap.getInfo(reactorElement.type));
	}

	@LuaCallable
	public Reactor getReactor() {
		return reactor;
	}

	@LuaCallable
	public void specify(String name) {
		name = name.trim().toLowerCase();
		ElementInformation thisInfo = ElementKeyMap.getInfo(reactorElement.type);
		for(short id : reactorElement.getPossibleSpecifications()) {
			ElementInformation info = ElementKeyMap.getInfo(id);
			if(info.getName().toLowerCase().equals(name) && id != thisInfo.getId() && info.isChamberPermitted(controller.getType()) && (getReactor().getChamberCapacity() + info.chamberCapacity <= 1.0f)) {
				reactorElement.convertToClientRequest(id);
				controller.getManagerContainer().getPowerInterface().requestRecalibrate();
				return;
			}
		}
	}

	@LuaCallable
	public String[] getValidSpecifications() {
		ArrayList<String> validSpecifications = new ArrayList<>();
		for(short id : reactorElement.getPossibleSpecifications()) {
			ElementInformation info = ElementKeyMap.getInfo(id);
			if(info.isChamberPermitted(controller.getType()) && (getReactor().getChamberCapacity() + info.chamberCapacity <= 1.0f)) validSpecifications.add(info.getName());
		}
		return validSpecifications.toArray(new String[0]);
	}

	@LuaCallable
	public void deactivate() {
		reactorElement.convertToClientRequest((short) ElementKeyMap.getInfo(reactorElement.type).chamberRoot);
		controller.getManagerContainer().getPowerInterface().requestRecalibrate();
	}

	@LuaCallable
	public Boolean canTrigger() {
		for(PlayerUsableInterface usableInterface : controller.getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == reactorElement.type && usableInterface.isPlayerUsable() && (entry.getKey() == usableInterface.getUsableId())) return true;
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == reactorElement.type) return true;
		}
		return false;
	}

	@LuaCallable
	public Float getCharge() {
		ManagerReloadInterface reloadInterface = getReloadInterface();
		if(reloadInterface instanceof RecharchableSingleModule) {
			RecharchableSingleModule recharchableSingleModule = (RecharchableSingleModule) reloadInterface;
			return recharchableSingleModule.getCharge();
		} else return 0.0f;
	}

	@LuaCallable
	public void trigger() {
		if(canTrigger()) {
			ManagerReloadInterface reloadInterface = getReloadInterface();
			if(reloadInterface instanceof RecharchableSingleModule) {
				RecharchableSingleModule recharchableSingleModule = (RecharchableSingleModule) reloadInterface;
				recharchableSingleModule.executeModule();
			}
		}
	}

	private long getUsableId() {
		for(PlayerUsableInterface usableInterface : controller.getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == reactorElement.type && (entry.getKey() == usableInterface.getUsableId())) return usableInterface.getUsableId();
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == reactorElement.type) return usableInterface.getUsableId();
		}
		return -1;
	}

	private ManagerReloadInterface getReloadInterface() {
		for(PlayerUsableInterface usableInterface : controller.getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == reactorElement.type && (entry.getKey() == usableInterface.getUsableId())) return usableInterface.getReloadInterface();
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == reactorElement.type) return usableInterface.getReloadInterface();
		}
		return null;
	}
}