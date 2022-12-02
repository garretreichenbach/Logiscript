package thederpgamer.logiscript.api.element.block;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;
import thederpgamer.logiscript.api.LuaInterface;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class BlockInfo extends LuaTable implements LuaInterface {

	private ElementInformation elementInfo;

	public BlockInfo() {

	}

	public BlockInfo(SegmentPiece segmentPiece) {
		elementInfo = segmentPiece.getInfo();
	}

	@Override
	public void initialize(Globals globals) {
		for(String method : getMethods()) {
			LuaFunction luaFunction = getMethod(method);
			if(luaFunction != null) set(method, luaFunction);
		}
	}

	@Override
	public String getName() {
		return "blockInfo";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getName", "getId"};
	}

	@Override
	public LuaFunction getMethod(String name) {
		switch(name) {
			case "getName":
				return new LuaFunction() {
					@Override
					public LuaValue call() {
						return LuaValue.valueOf(getElementName());
					}
				};
			case "getId":
				return new LuaFunction() {
					@Override
					public LuaValue call() {
						return LuaValue.valueOf(getId());
					}
				};
			default:
				return null;
		}
	}

	@Override
	public LuaValue call(String name) {
		return getMethod(name).call();
	}

	public String getElementName() {
		return elementInfo.getName();
	}

	public int getId() {
		return elementInfo.getId();
	}
}
