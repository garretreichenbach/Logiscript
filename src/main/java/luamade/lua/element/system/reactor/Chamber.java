package luamade.lua.element.system.reactor;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaBoolean;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaString;
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

	@LuaMadeCallable
	public LuaString getName() {
		return LuaString.valueOf(ElementKeyMap.getInfo(reactorElement.type).getName());
	}

	@LuaMadeCallable
	public BlockInfo getBlockInfo() {
		return new BlockInfo(ElementKeyMap.getInfo(reactorElement.type));
	}

	@LuaMadeCallable
	public Reactor getReactor() {
		return reactor;
	}

	@LuaMadeCallable
	public void specify(LuaString name) {
		LuaString n = LuaString.valueOf(name.tojstring().toLowerCase().trim());
		ElementInformation thisInfo = ElementKeyMap.getInfo(reactorElement.type);
		for(short id : reactorElement.getPossibleSpecifications()) {
			ElementInformation info = ElementKeyMap.getInfo(id);
			LuaString compare = LuaString.valueOf(info.getName().toLowerCase().trim());
			if(compare.equals(n) && id != thisInfo.getId() && info.isChamberPermitted(controller.getType()) && (getReactor().getChamberCapacity() + info.chamberCapacity <= 1.0f)) {
				reactorElement.convertToClientRequest(id);
				controller.getManagerContainer().getPowerInterface().requestRecalibrate();
				return;
			}
		}
	}

	@LuaMadeCallable
	public LuaString[] getValidSpecifications() {
		ArrayList<LuaString> validSpecifications = new ArrayList<>();
		for(short id : reactorElement.getPossibleSpecifications()) {
			ElementInformation info = ElementKeyMap.getInfo(id);
			if(info.isChamberPermitted(controller.getType()) && (getReactor().getChamberCapacity() + info.chamberCapacity <= 1.0f)) validSpecifications.add(LuaString.valueOf(info.getName()));
		}
		return validSpecifications.toArray(new LuaString[0]);
	}

	@LuaMadeCallable
	public void deactivate() {
		reactorElement.convertToClientRequest((short) ElementKeyMap.getInfo(reactorElement.type).chamberRoot);
		controller.getManagerContainer().getPowerInterface().requestRecalibrate();
	}

	@LuaMadeCallable
	public LuaBoolean canTrigger() {
		for(PlayerUsableInterface usableInterface : controller.getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == reactorElement.type && usableInterface.isPlayerUsable() && (entry.getKey() == usableInterface.getUsableId())) return LuaBoolean.valueOf(true);
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == reactorElement.type) return LuaBoolean.valueOf(true);
		}
		return LuaBoolean.valueOf(false);
	}

	@LuaMadeCallable
	public LuaDouble getCharge() {
		ManagerReloadInterface reloadInterface = getReloadInterface();
		if(reloadInterface instanceof RecharchableSingleModule) {
			RecharchableSingleModule recharchableSingleModule = (RecharchableSingleModule) reloadInterface;
			return (LuaDouble) LuaDouble.valueOf(recharchableSingleModule.getCharge());
		} else return (LuaDouble) LuaDouble.valueOf(0.0f);
	}

	@LuaMadeCallable
	public void trigger() {
		if(canTrigger().v) {
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