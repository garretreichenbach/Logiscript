package thederpgamer.logiscript.api.element.block;

import org.luaj.vm2.*;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.LuaInterface;
import thederpgamer.logiscript.api.entity.Entity;

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
		return "block";
	}

	@Override
	public String[] getMethods() {
		return new String[] {"getPos", "getId", "getInfo", "isActive", "setActive", "getEntity"};
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
			case "getId":
				return new LuaFunction() {
					@Override
					public LuaNumber call() {
						return LuaValue.valueOf(getId());
					}
				};
			case "getInfo":
				return new LuaFunction() {
					@Override
					public BlockInfo call() {
						return getInfo();
					}
				};
			case "isActive":
				return new LuaFunction() {
					@Override
					public LuaBoolean call() {
						return LuaValue.valueOf(isActive());
					}
				};
			case "setActive":
				return new LuaFunction() {
					@Override
					public LuaValue call(LuaValue active) {
						setActive(active.checkboolean());
						return LuaValue.NIL;
					}
				};
			case "getEntity":
				return new LuaFunction() {
					@Override
					public Entity call() {
						return getEntity();
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

	public int[] getPos() {
		return new int[] {segmentPiece.getAbsolutePosX(), segmentPiece.getAbsolutePosY(), segmentPiece.getAbsolutePosZ()};
	}

	public int getId() {
		return segmentPiece.getType();
	}

	public BlockInfo getInfo() {
		return new BlockInfo(segmentPiece);
	}

	public boolean isActive() {
		return segmentPiece.isActive();
	}

	public void setActive(boolean bool) {
		segmentPiece.setActive(bool);
		segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
	}

	public Entity getEntity() {
		return new Entity(segmentPiece.getSegmentController());
	}
}
