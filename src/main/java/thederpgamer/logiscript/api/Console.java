package thederpgamer.logiscript.api;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.element.block.Block;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console extends LuaTable implements LuaInterface {

	private final SegmentPiece segmentPiece;

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	@Override
	public String getName() {
		return "console";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getBlock"};
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
}
