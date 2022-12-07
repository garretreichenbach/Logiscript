package luamade.lua.element.system.module;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ThrustSystem extends LuaMadeUserdata implements ModuleInterface {

	private final SegmentController segmentController;

	public ThrustSystem(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public Float getTMR() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getThrustMassRatio();
		else return 0.0f;
	}

	@LuaMadeCallable
	public Float getThrust() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getActualThrust();
		else return 0.0f;
	}

	@LuaMadeCallable
	public Float getMaxSpeed() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getMaxSpeedAbsolute();
		else return 0.0f;
	}

	@LuaMadeCallable
	@Override
	public Integer getSize() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrust().getElementManager().totalSize;
		else return 0;
	}

	private ShipManagerContainer getContainer() {
		try {
			return ((Ship) segmentController).getManagerContainer();
		} catch(Exception exception) {
			return null;
		}
	}
}
