package luamade.lua.element.system.module;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.SegmentController;

/**
 * Base class for all segment controller module wrappers (thrust, etc.).
 */
public abstract class Module extends LuaMadeUserdata {

	protected final SegmentController segmentController;

	protected Module(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	@LuaMadeCallable
	public abstract Integer getSize();
}
