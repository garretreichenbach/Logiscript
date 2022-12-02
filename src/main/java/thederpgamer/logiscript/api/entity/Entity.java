package thederpgamer.logiscript.api.entity;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.controller.SegmentController;
import thederpgamer.logiscript.api.LuaInterface;
import thederpgamer.logiscript.api.element.block.Block;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Entity extends LuaTable implements LuaInterface {

	private SegmentController segmentController;

	public Entity() {

	}

	public Entity(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public SegmentController getSegmentController() {
		return segmentController;
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
		return "entity";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getName", "setName", "getBlockAt"};
	}

	@Override
	public LuaFunction getMethod(String name) {
		switch(name) {
			case "getName":
				return new LuaFunction() {
					@Override
					public LuaValue call() {
						return LuaValue.valueOf(getEntityName());
					}
				};
			case "setName":
				return new LuaFunction() {
					@Override
					public LuaValue call(LuaValue name) {
						setName(String.valueOf(name.checkstring()));
						return LuaValue.NIL;
					}
				};
			case "getBlockAt":
				return new LuaFunction() {
					@Override
					public LuaValue call(LuaValue x, LuaValue y, LuaValue z) {
						return getBlockAt(new int[] {x.checkint(), y.checkint(), z.checkint()});
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

	public String getEntityName() {
		return segmentController.getRealName();
	}

	public void setName(String name) {
		segmentController.setRealName(name);
	}

	public Block getBlockAt(int[] pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
	}
}
