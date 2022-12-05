package luamade.lua.element.system.reactor;

import api.utils.game.SegmentControllerUtils;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.power.reactor.tree.ReactorElement;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Reactor extends LuaMadeUserdata {

	private final SegmentController segmentController;

	public Reactor(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public Double getRecharge() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getRechargeRatePowerPerSec();
		} catch(Exception exception) {
			return 0.0;
		}
	}

	@LuaMadeCallable
	public Double getConsumption() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentConsumption();
		} catch(Exception exception) {
			return 0.0;
		}
	}

	@LuaMadeCallable
	public Float getChamberCapacity() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getChamberCapacity();
		} catch(Exception exception) {
			return 0.0f;
		}
	}

	@LuaMadeCallable
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

	@LuaMadeCallable
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

	@LuaMadeCallable
	public Long getMaxHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentMaxHp();
		} catch(Exception exception) {
			return 0L;
		}
	}

	@LuaMadeCallable
	public Long getHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return controller.getManagerContainer().getPowerInterface().getCurrentHp();
		} catch(Exception exception) {
			return 0L;
		}
	}
}
