package luamade.lua.element.system.module;

import luamade.lua.element.system.module.ModuleInterface;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ThrustModule implements ModuleInterface {

	private final SegmentController segmentController;

	public ThrustModule(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public double getTMR() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getThrustMassRatio();
		else return 0;
	}

	public double getThrust() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getActualThrust();
		else return 0;
	}

	public double getMaxSpeed() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return managerContainer.getThrusterElementManager().getMaxSpeedAbsolute();
		else return 0;
	}

	@Override
	public int getSize() {
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
