package luamade.lua.element.system.reactor;

import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.PlayerUsableInterface;
import org.schema.game.common.controller.elements.ManagerReloadInterface;
import org.schema.game.common.controller.elements.RecharchableSingleModule;
import org.schema.game.common.controller.elements.power.reactor.tree.ReactorElement;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.Map;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class UsableChamber extends Chamber {

	public UsableChamber(ReactorElement reactorElement, ManagedUsableSegmentController<?> segmentController, Reactor reactor) {
		super(reactorElement, segmentController, reactor);
	}

	@LuaMadeCallable
	public void activate() {
		if(isUsable()) {
			ManagerReloadInterface reloadInterface = getReloadInterface();
			if(reloadInterface instanceof RecharchableSingleModule) {
				RecharchableSingleModule recharchableSingleModule = (RecharchableSingleModule) reloadInterface;
				recharchableSingleModule.executeModule();
			}
		}
	}

	@LuaMadeCallable
	public void deactivate() {
		getReactorElement().convertToClientRequest((short) ElementKeyMap.getInfo(getReactorElement().type).chamberRoot);
		getController().getManagerContainer().getPowerInterface().requestRecalibrate();
	}

	@LuaMadeCallable
	public Float getCharge() {
		ManagerReloadInterface reloadInterface = getReloadInterface();
		if(reloadInterface instanceof RecharchableSingleModule) {
			RecharchableSingleModule recharchableSingleModule = (RecharchableSingleModule) reloadInterface;
			return recharchableSingleModule.getCharge();
		} else return 0.0f;
	}

	private long getUsableId() {
		for(PlayerUsableInterface usableInterface : getController().getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == getReactorElement().type && (entry.getKey() == usableInterface.getUsableId())) return usableInterface.getUsableId();
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == getReactorElement().type) return usableInterface.getUsableId();
		}
		return -1;
	}

	private ManagerReloadInterface getReloadInterface() {
		for(PlayerUsableInterface usableInterface : getController().getManagerContainer().getPlayerUsable()) {
			for(Map.Entry<Long, Short> entry : PlayerUsableInterface.ICONS.entrySet()) {
				if(entry.getValue() == getReactorElement().type && (entry.getKey() == usableInterface.getUsableId())) return usableInterface.getReloadInterface();
			}
			if(usableInterface.isPlayerUsable() && usableInterface.getUsableId() == getReactorElement().type) return usableInterface.getReloadInterface();
		}
		return null;
	}
}
