package luamade.lua.element.system.reactor;

import api.utils.game.SegmentControllerUtils;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.power.reactor.tree.ReactorElement;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Reactor {

	private final SegmentController segmentController;

	public Reactor(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public double getRecharge() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getRechargeRatePowerPerSec();
		} catch(Exception exception) {
			return 0;
		}
	}

	public double getConsumption() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentConsumption();
		} catch(Exception exception) {
			return 0;
		}
	}

	public float getChamberCapacity() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getChamberCapacity();
		} catch(Exception exception) {
			return 0;
		}
	}

	public Chamber[] getChambers() {
		try {
			ArrayList<Chamber> chambers = new ArrayList<>();
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			for(ReactorElement element : SegmentControllerUtils.getAllChambers(controller)) chambers.add(new Chamber(element, controller, this));
			return chambers.toArray(new Chamber[0]);
		} catch(Exception exception) {
			return new Chamber[0];
		}
	}

	public Chamber[] getActiveChambers() {
		try {
			ArrayList<Chamber> chambers = new ArrayList<>();
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			for(ReactorElement element : SegmentControllerUtils.getAllChambers(controller)) {
				if(element.isAllValidOrUnspecified()) chambers.add(new Chamber(element, controller, this));
			}
			return chambers.toArray(new Chamber[0]);
		} catch(Exception exception) {
			return new Chamber[0];
		}
	}

	public double getMaxHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentMaxHp();
		} catch(Exception exception) {
			return 0;
		}
	}

	public double getHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentHp();
		} catch(Exception exception) {
			return 0;
		}
	}
}
