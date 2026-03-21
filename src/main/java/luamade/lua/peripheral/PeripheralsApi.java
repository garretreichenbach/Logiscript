package luamade.lua.peripheral;

import luamade.lua.data.Vec3i;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import luamade.utils.SegmentPieceUtils;
import org.luaj.vm2.LuaError;
import org.schema.game.common.data.SegmentPiece;

import java.util.Locale;

public class PeripheralsApi extends LuaMadeUserdata {
	private final ComputerModule module;

	public PeripheralsApi(ComputerModule module) {
		this.module = module;
	}

	@LuaMadeCallable
	public Block getCurrentBlock() {
		return Block.wrap(requireLiveComputerPiece(), module);
	}

	@LuaMadeCallable
	public Block wrap(Block block) {
		if(block == null) {
			return null;
		}
		return Block.wrap(block.getSegmentPiece(), module);
	}

	@LuaMadeCallable
	public Block wrap(Block block, String asType) {
		if(block == null) {
			return null;
		}
		return Block.wrapAs(block.getSegmentPiece(), asType, module);
	}

	@LuaMadeCallable
	public Block wrapCurrent() {
		return Block.wrap(requireLiveComputerPiece(), module);
	}

	@LuaMadeCallable
	public Block wrapCurrent(String asType) {
		return Block.wrapAs(requireLiveComputerPiece(), asType, module);
	}

	@LuaMadeCallable
	public Block getAt(Vec3i position) {
		if(position == null) {
			return null;
		}

		SegmentPiece computerPiece = requireLiveComputerPiece();
		SegmentPiece piece = computerPiece.getSegmentController().getSegmentBuffer().getPointUnsave(
			position.getX(),
			position.getY(),
			position.getZ()
		);
		if(piece == null) {
			return null;
		}
		return Block.wrap(piece, module);
	}

	@LuaMadeCallable
	public Block getRelative(String side) {
		String normalized = normalizeSide(side);
		if(normalized == null) {
			return null;
		}

		SegmentPiece adjacent = SegmentPieceUtils.getAdjacentDir(requireLiveComputerPiece(), normalized);
		if(adjacent == null) {
			return null;
		}
		return Block.wrap(adjacent, module);
	}

	private SegmentPiece requireLiveComputerPiece() {
		if(module == null || module.getSegmentPiece() == null) {
			throw new LuaError("Computer block reference is not available");
		}
		SegmentPiece modulePiece = module.getSegmentPiece();
		if(modulePiece.getSegmentController() == null || modulePiece.getSegmentController().getSegmentBuffer() == null) {
			throw new LuaError("Computer block reference is no longer valid");
		}
		SegmentPiece livePiece = modulePiece.getSegmentController().getSegmentBuffer().getPointUnsave(modulePiece.getAbsoluteIndex());
		if(livePiece == null) {
			throw new LuaError("Computer block no longer exists");
		}
		if(livePiece.getType() != modulePiece.getType()) {
			throw new LuaError("Computer block type changed since initialization");
		}
		return livePiece;
	}

	@LuaMadeCallable
	public Block wrapRelative(String side) {
		Block relative = getRelative(side);
		if(relative == null) {
			return null;
		}
		return wrap(relative);
	}

	@LuaMadeCallable
	public Block wrapRelative(String side, String asType) {
		Block relative = getRelative(side);
		if(relative == null) {
			return null;
		}
		return wrap(relative, asType);
	}

	@LuaMadeCallable
	public Block wrapAt(Vec3i position) {
		Block block = getAt(position);
		if(block == null) {
			return null;
		}
		return wrap(block);
	}

	@LuaMadeCallable
	public Block wrapAt(Vec3i position, String asType) {
		Block block = getAt(position);
		if(block == null) {
			return null;
		}
		return wrap(block, asType);
	}

	@LuaMadeCallable
	public Boolean hasRelative(String side) {
		return getRelative(side) != null;
	}

	@LuaMadeCallable
	public String[] getSides() {
		return new String[] {"front", "back", "left", "right", "top", "bottom"};
	}

	private String normalizeSide(String side) {
		if(side == null) {
			return null;
		}

		String value = side.trim().toLowerCase(Locale.ROOT);
		switch(value) {
			case "up":
			case "top":
				return "up";
			case "down":
			case "bottom":
				return "down";
			case "left":
			case "right":
			case "front":
			case "back":
				return value;
			default:
				return null;
		}
	}
}
