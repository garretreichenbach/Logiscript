package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.entity.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-entity module that owns the server-authoritative {@link ProjectorFrame}
 * for each Projector block on that entity. Frames are pushed to nearby clients
 * and persisted to the world save through the exact same tag path that drives
 * {@link VaultModuleContainer}: a mutation calls {@link #flagUpdatedData()},
 * StarMade serializes the module ({@link #onTagSerialize}) and syncs it via the
 * built-in {@code PacketSCSyncMCModule}; {@link #onTagDeserialize} rebuilds the
 * frame map on every client (and on server load from disk).
 *
 * <p>Because scripts now execute server-side, a frame built by a running script
 * is already server truth — {@code proj.publish(frame)} is a local handoff into
 * {@link #publishFrame}, with no request/response packet round-trip.
 *
 * <p>{@link #ACTIVE_CONTAINERS} lets {@link luamade.gui.ProjectorWorldDrawer}
 * enumerate every live projector each frame (the base game has no per-segment
 * draw event), mirroring {@link ComputerModuleContainer}.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) ProjectorFrame] * count}
 */
public class ProjectorModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	public static final Set<ProjectorModuleContainer> ACTIVE_CONTAINERS = ConcurrentHashMap.newKeySet();

	private final Object frameLock = new Object();
	private final Long2ObjectOpenHashMap<ProjectorFrame> frames = new Long2ObjectOpenHashMap<>();

	public ProjectorModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.PROJECTOR.getId());
		ACTIVE_CONTAINERS.add(this);
	}

	public static ProjectorModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.PROJECTOR.getId()) instanceof ProjectorModuleContainer) {
			return (ProjectorModuleContainer) managerContainer.getModMCModule(ElementRegistry.PROJECTOR.getId());
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// Frame publishing (server-side handoff from a running script)
	// -------------------------------------------------------------------------

	/** Replaces the frame for a projector block and syncs it to nearby clients. */
	public void publishFrame(long absIndex, ProjectorFrame frame) {
		if(frame == null || frame.isEmpty()) {
			clearFrame(absIndex);
			return;
		}
		synchronized(frameLock) {
			frames.put(absIndex, frame);
		}
		flagUpdatedData();
	}

	/** Removes a projector's frame (nothing renders) and syncs the removal. */
	public void clearFrame(long absIndex) {
		boolean changed;
		synchronized(frameLock) {
			changed = frames.remove(absIndex) != null;
		}
		if(changed) {
			flagUpdatedData();
		}
	}

	public ProjectorFrame getFrame(long absIndex) {
		synchronized(frameLock) {
			return frames.get(absIndex);
		}
	}

	/** Snapshot of the current frames for the render thread (copied under the lock). */
	public List<FrameEntry> copyFrames() {
		synchronized(frameLock) {
			List<FrameEntry> out = new ArrayList<>(frames.size());
			for(long abs : frames.keySet().toLongArray()) {
				out.add(new FrameEntry(abs, frames.get(abs)));
			}
			return out;
		}
	}

	public void removeBlock(long absIndex) {
		clearFrame(absIndex);
	}

	// -------------------------------------------------------------------------
	// SystemModule boilerplate
	// -------------------------------------------------------------------------

	@Override
	public void handlePlace(long abs, byte orientation) {
		// A projector starts empty; its frame is created on the first publish().
	}

	@Override
	public void handleRemove(long abs) {
		// Mirror VaultModuleContainer: drop in-memory state on unload/reload. The
		// saved tag still holds it (written before unload) and onTagDeserialize
		// repopulates on reload. Genuine destruction goes through removeBlock().
		synchronized(frameLock) {
			frames.remove(abs);
		}
	}

	@Override
	public double getPowerConsumedPerSecondResting() {
		return 0;
	}

	@Override
	public double getPowerConsumedPerSecondCharging() {
		return 0;
	}

	@Override
	public String getName() {
		return "Projector";
	}

	@Override
	public void handle(Timer timer) {
		// No per-tick work in v1 (frames change only on publish). A stale-frame
		// TTL sweep could live here as a follow-up.
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		synchronized(frameLock) {
			buffer.writeByte(VERSION);
			buffer.writeInt(frames.size());
			for(long abs : frames.keySet().toLongArray()) {
				buffer.writeLong(abs);
				frames.get(abs).writeTo(buffer);
			}
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		synchronized(frameLock) {
			frames.clear();
			byte version = buffer.readByte();
			if(version != VERSION) {
				return;
			}
			int count = buffer.readInt();
			for(int i = 0; i < count; i++) {
				long abs = buffer.readLong();
				ProjectorFrame frame = ProjectorFrame.readFrom(buffer);
				if(!frame.isEmpty()) {
					frames.put(abs, frame);
				}
			}
		}
	}

	/** A projector block's absolute index paired with its current frame. */
	public static final class FrameEntry {
		public final long absIndex;
		public final ProjectorFrame frame;

		FrameEntry(long absIndex, ProjectorFrame frame) {
			this.absIndex = absIndex;
			this.frame = frame;
		}
	}
}
