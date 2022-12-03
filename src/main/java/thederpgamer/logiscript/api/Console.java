package thederpgamer.logiscript.api;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.element.block.Block;
import thederpgamer.logiscript.api.element.block.LuaBlock;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console extends LuaTable implements LuaInterface {

	private final SegmentPiece segmentPiece;

	public Console(Globals globals, SegmentPiece segmentPiece) {
		initialize(globals);
		this.segmentPiece = segmentPiece;
	}

	@Override
	public void initialize(Globals globals) {
		globals.set(getName(), this);
		setmetatable(this);
		for(String method : getMethods()) {
			LuaFunction luaFunction = getMethod(method);
			if(luaFunction != null) set(method, luaFunction);
		}
	}

	@Override
	public String getName() {
		return "console";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getBlock", "getLuaBlock"};
	}

	@Override
	public LuaFunction getMethod(String name) {
		switch(name) {
			case "getBlock":
				return new LuaFunction() {
					@Override
					public LuaValue call() {
						return getBlock();
					}
				};
			case "getLuaBlock":
				return new LuaFunction() {
					@Override
					public LuaValue call() {
						System.err.println("Getting Lua block.");
						return CoerceJavaToLua.coerce(getLuaBlock());
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

	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	public LuaBlock getLuaBlock() {
		return new LuaBlock(segmentPiece);
	}
}
