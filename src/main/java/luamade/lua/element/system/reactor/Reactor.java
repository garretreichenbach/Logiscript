package luamade.lua.element.system.reactor;

import api.utils.game.SegmentControllerUtils;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
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
	public LuaDouble getRecharge() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return (LuaDouble) LuaDouble.valueOf(controller.getManagerContainer().getPowerInterface().getRechargeRatePowerPerSec());
		} catch(Exception exception) {
			return (LuaDouble) LuaDouble.valueOf(0.0);
		}
	}

	@LuaMadeCallable
	public LuaDouble getConsumption() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return (LuaDouble) LuaDouble.valueOf(controller.getManagerContainer().getPowerInterface().getCurrentConsumption());
		} catch(Exception exception) {
			return (LuaDouble) LuaDouble.valueOf(0.0);
		}
	}

	@LuaMadeCallable
	public LuaDouble getChamberCapacity() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return (LuaDouble) LuaDouble.valueOf(controller.getManagerContainer().getPowerInterface().getChamberCapacity());
		} catch(Exception exception) {
			return (LuaDouble) LuaDouble.valueOf(0.0f);
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
	public LuaInteger getMaxHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return (LuaInteger) LuaInteger.valueOf(controller.getManagerContainer().getPowerInterface().getCurrentMaxHp());
		} catch(Exception exception) {
			return LuaInteger.valueOf(0);
		}
	}

	@LuaMadeCallable
	public LuaInteger getHP() {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentController;
			return (LuaInteger) LuaInteger.valueOf(controller.getManagerContainer().getPowerInterface().getCurrentHp());
		} catch(Exception exception) {
			return LuaInteger.valueOf(0);
		}
	}
}
