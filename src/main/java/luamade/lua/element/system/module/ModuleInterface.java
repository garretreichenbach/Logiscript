package luamade.lua.element.system.module;

import luamade.luawrap.LuaMadeCallable;
import org.luaj.vm2.LuaInteger;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public interface ModuleInterface {

	@LuaMadeCallable
	LuaInteger getSize();
}
