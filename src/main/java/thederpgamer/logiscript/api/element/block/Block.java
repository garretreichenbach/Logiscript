package thederpgamer.logiscript.api.element.block;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.LuaInterface;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Block extends LuaTable implements LuaInterface {

	private final SegmentPiece segmentPiece;

	public Block(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	public SegmentPiece getSegmentPiece() {
		return segmentPiece;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getPos"};
	}

	@Override
	public LuaFunction getMethod(String name) {
		switch(name) {
			case "getPos":
				return new LuaFunction() {
					@Override
					public LuaTable call() {
						return new LuaTable() {{
							set("x", getPos()[0]);
							set("y", getPos()[1]);
							set("z", getPos()[2]);
						}};
					}
				};
			default:
				return null;
		}
	}

	public int[] getPos() {
		return new int[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
	}
}
