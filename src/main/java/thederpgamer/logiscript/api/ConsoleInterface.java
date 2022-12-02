package thederpgamer.logiscript.api;

import thederpgamer.logiscript.api.element.block.Block;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public interface ConsoleInterface extends LuaInterface {

	@LuaCallable(name = "getBlock", description = "Returns the Block representing this Console.")
	Block getBlock();
}