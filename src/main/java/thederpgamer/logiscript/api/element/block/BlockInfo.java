package thederpgamer.logiscript.api.element.block;

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

	private final ElementInformation elementInfo;

	public BlockInfo(SegmentPiece segmentPiece) {
		elementInfo = segmentPiece.getInfo();
	}

	public BlockInfo(ElementInformation elementInfo) {
		this.elementInfo = elementInfo;
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
