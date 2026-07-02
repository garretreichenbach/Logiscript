package luamade.lua.element.block;

import luamade.lua.gfx.Gfx3d;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import luamade.system.module.ProjectorModuleContainer;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

/**
 * Lua-facing wrapper for a Projector block, obtained via
 * {@code peripherals.wrap(block, "projector")}.
 *
 * <p>Usage is build-then-publish:
 * <pre>{@code
 * local frame = proj.newFrame()
 * frame.line3d(0,0,0, 0,3,0, 1,1,1,1)
 * proj.publish(frame)   -- now visible to every player near the ship
 * }</pre>
 *
 * <p>Because scripts execute server-side, {@code publish} is a local handoff into
 * the block's {@link ProjectorModuleContainer} — it snapshots the frame, the
 * container flags updated data, and StarMade's built-in module sync pushes it to
 * every nearby client and persists it to the world save. There is no request /
 * response packet round-trip.
 */
@LuaMadeClass("Projector")
public class ProjectorBlock extends Block {

	public ProjectorBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
	}

	/** Returns a fresh, empty frame to draw into. */
	@LuaMadeCallable
	public Gfx3d newFrame() {
		return new Gfx3d();
	}

	/** Publishes a frame; it becomes visible to every nearby player and persists. */
	@LuaMadeCallable
	public Boolean publish(Gfx3d frame) {
		if(frame == null) {
			return false;
		}
		container().publishFrame(absIndex(), frame.snapshot());
		return true;
	}

	/** Clears this projector so nothing renders. */
	@LuaMadeCallable
	public Boolean clear() {
		container().clearFrame(absIndex());
		return true;
	}

	/** The maximum per-axis offset (in blocks) that coordinates are clamped to. */
	@LuaMadeCallable
	public Double getMaxOffset() {
		return ConfigManager.getProjectorMaxOffsetBlocks();
	}

	private long absIndex() {
		return getSegmentPiece().getAbsoluteIndex();
	}

	private ProjectorModuleContainer container() {
		SegmentPiece piece = getSegmentPiece();
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("Projector is not available on this structure type");
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		ProjectorModuleContainer container = ProjectorModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			throw new LuaError("Projector module is not initialized");
		}
		return container;
	}
}
