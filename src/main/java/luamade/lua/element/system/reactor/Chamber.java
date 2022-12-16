package luamade.lua.element.system.reactor;

import luamade.lua.element.block.BlockInfo;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.PlayerUsableInterface;
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
	public String getName() {
		return ElementKeyMap.getInfo(reactorElement.type).getName();
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

	@LuaMadeCallable
	public String[] getValidSpecifications() {
		ArrayList<String> validSpecifications = new ArrayList<>();
		for(short id : reactorElement.getPossibleSpecifications()) {
			ElementInformation info = ElementKeyMap.getInfo(id);
			if(info.isChamberPermitted(controller.getType()) && (getReactor().getChamberCapacity() + info.chamberCapacity <= 1.0f)) validSpecifications.add(info.getName());
		}
		return validSpecifications.toArray(new String[0]);
	}

	@LuaMadeCallable
	public Boolean isUsable() {
		for(PlayerUsableInterface usableInterface : getController().getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == getReactorElement().type && usableInterface.isPlayerUsable() && (entry.getKey() == usableInterface.getUsableId())) return true;
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == getReactorElement().type) return true;
		}
		return false;
	}

	@LuaMadeCallable
	public UsableChamber getUsable() {
		if(isUsable()) return new UsableChamber(getReactorElement(), getController(), getReactor());
		else return null;
	}

	public ReactorElement getReactorElement() {
		return reactorElement;
	}

	public ManagedUsableSegmentController<?> getController() {
		return controller;
	}
}