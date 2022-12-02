package thederpgamer.logiscript.api.entity;

import org.luaj.vm2.LuaFunction;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.schema.game.common.controller.SegmentController;
import thederpgamer.logiscript.api.LuaInterface;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Entity extends LuaTable implements LuaInterface {

	private final SegmentController segmentController;

	public Entity(SegmentController segmentController) {
		this.segmentController = segmentController;
	}

	public SegmentController getSegmentController() {
		return segmentController;
	}

	@Override
	public String getName() {
		return "entity";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getName", "setName"};
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
}
