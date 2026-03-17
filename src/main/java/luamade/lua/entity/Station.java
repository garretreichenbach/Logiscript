package luamade.lua.entity;

import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SpaceStation;

/**
 * Entity subtype for space stations. Provides access to station-specific
 * systems and properties distinct from mobile ships.
 */
public class Station extends Entity {

	public Station(SegmentController controller) {
		super(controller);
	}

	private SpaceStation getStation() {
		return (SpaceStation) getSegmentController();
	}

	@LuaMadeCallable
	public Boolean isHomeBase() {
		return getStation().isHomeBase();
	}
}
