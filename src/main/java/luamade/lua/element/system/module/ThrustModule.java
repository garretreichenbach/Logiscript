package luamade.lua.element.system.module;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaInteger;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.elements.ShipManagerContainer;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ThrustModule extends LuaMadeUserdata implements ModuleInterface {

	private final SegmentController segmentController;

	public ThrustModule(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public LuaDouble getTMR() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return (LuaDouble) LuaDouble.valueOf(managerContainer.getThrusterElementManager().getThrustMassRatio());
		else return (LuaDouble) LuaDouble.valueOf(0.0f);
	}

	@LuaMadeCallable
	public LuaDouble getThrust() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return (LuaDouble) LuaDouble.valueOf(managerContainer.getThrusterElementManager().getActualThrust());
		else return (LuaDouble) LuaDouble.valueOf(0.0f);
	}

	@LuaMadeCallable
	public LuaDouble getMaxSpeed() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return (LuaDouble) LuaDouble.valueOf(managerContainer.getThrusterElementManager().getMaxSpeedAbsolute());
		else return (LuaDouble) LuaDouble.valueOf(0.0f);
	}

	@LuaMadeCallable
	@Override
	public LuaInteger getSize() {
		ShipManagerContainer managerContainer = getContainer();
		if(managerContainer != null) return LuaInteger.valueOf(managerContainer.getThrust().getElementManager().totalSize);
		else return LuaInteger.valueOf(0);
	}

	private ShipManagerContainer getContainer() {
		try {
			return ((Ship) segmentController).getManagerContainer();
		} catch(Exception exception) {
			return null;
		}
	}
}
