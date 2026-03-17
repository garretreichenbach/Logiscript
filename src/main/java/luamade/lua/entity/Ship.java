package luamade.lua.entity;

import api.utils.game.SegmentControllerUtils;
import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.cloaking.StealthAddOn;

/**
 * Entity subtype for player ships. Provides access to ship-specific systems
 * like jamming, cloaking, and pilot information.
 */
public class Ship extends Entity {

	public Ship(SegmentController controller) {
		super(controller);
	}

	private org.schema.game.common.controller.Ship getShip() {
		return (org.schema.game.common.controller.Ship) getSegmentController();
	}

	@LuaMadeCallable
	public Boolean isJamming() {
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		return addon != null && addon.isActive();
	}

	@LuaMadeCallable
	public Boolean canJam() {
		if(isJamming()) return false;
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		return addon != null && addon.canExecute();
	}

	@LuaMadeCallable
	public void activateJamming(Boolean active) {
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		if(addon != null) {
			if(active) {
				if(addon.canExecute()) addon.executeModule();
			} else {
				if(addon.isActive()) addon.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public Boolean isCloaking() {
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		return addon != null && addon.isActive();
	}

	@LuaMadeCallable
	public Boolean canCloak() {
		if(isCloaking()) return false;
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		return addon != null && addon.canExecute();
	}

	@LuaMadeCallable
	public void activateCloaking(Boolean active) {
		StealthAddOn addon = SegmentControllerUtils.getAddon(getShip(), StealthAddOn.class);
		if(addon != null) {
			if(active) {
				if(addon.canExecute()) addon.executeModule();
			} else {
				if(addon.isActive()) addon.onRevealingAction();
			}
		}
	}

	@LuaMadeCallable
	public String getPilot() {
		if(getSegmentController().isConrolledByActivePlayer()) {
			return SegmentControllerUtils.getAttachedPlayers(getSegmentController()).get(0).getName();
		}
		return null;
	}
}
