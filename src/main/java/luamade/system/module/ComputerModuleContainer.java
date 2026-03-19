package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends SystemModule {

	private final byte VERSION = 6;
	private static final Set<ComputerModuleContainer> ACTIVE_CONTAINERS = ConcurrentHashMap.newKeySet();
	private final Long2ObjectOpenHashMap<ComputerModule> computerModules = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<PendingModuleState> pendingModuleStates = new Long2ObjectOpenHashMap<>();
	/** Guards all reads and writes to {@code pendingModuleStates} across the game-loop and network threads. */
	private final Object pendingLock = new Object();

	public ComputerModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, LuaMade.getInstance(), ElementRegistry.COMPUTER.getId());
		ACTIVE_CONTAINERS.add(this);
	}

	public static ComputerModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.COMPUTER.getId()) instanceof ComputerModuleContainer) {
			return (ComputerModuleContainer) managerContainer.getModMCModule(ElementRegistry.COMPUTER.getId());
		}
		return null;
	}

	public static void saveAndCleanupAll() {
		for(ComputerModuleContainer container : ACTIVE_CONTAINERS) {
			container.saveAndCleanupAllModules();
		}
		ACTIVE_CONTAINERS.clear();
	}

	public static Set<String> snapshotActiveComputerUUIDs() {
		Set<String> uuids = new HashSet<>();
		for(ComputerModuleContainer container : ACTIVE_CONTAINERS) {
			container.collectKnownUUIDs(uuids);
		}
		return uuids;
	}

	@Override
	public void handlePlace(long abs, byte orientation) {
		//Do nothing, because we are handling it ourselves with events
	}

	@Override
	public void handleRemove(long abs) {
		//Do nothing, because we are handling it ourselves with events
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
		return "Computer";
	}

	@Override
	public void handle(Timer timer) {
		synchronized(pendingLock) {
			if(!pendingModuleStates.isEmpty()) {
				restorePendingModules();
			}
		}
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		Map<Long, PendingModuleState> statesToSerialize = new LinkedHashMap<>();

		synchronized(pendingLock) {
			for(long abs : pendingModuleStates.keySet().toLongArray()) {
				PendingModuleState pending = pendingModuleStates.get(abs);
				if(pending != null) {
					statesToSerialize.put(abs, pending);
				}
			}
		}

		for(ComputerModule module : computerModules.values()) {
			if(module == null || module.getSegmentPiece() == null) {
				continue;
			}

			long abs = module.getSegmentPiece().getAbsoluteIndex();
			statesToSerialize.put(abs, PendingModuleState.fromModule(module));
		}

		buffer.writeInt(statesToSerialize.size());
		for(Map.Entry<Long, PendingModuleState> entry : statesToSerialize.entrySet()) {
			long abs = entry.getKey();
			PendingModuleState state = entry.getValue();

			buffer.writeLong(abs);
			buffer.writeString(safeString(state.stableUUID));
			buffer.writeByte(state.modeOrdinal);
			buffer.writeString(safeString(state.lastOpenFile));
			buffer.writeString(safeString(state.savedTerminalInput));
			buffer.writeString(safeString(state.hostname));
			buffer.writeString(safeString(state.displayName));
			buffer.writeString(safeString(state.lastDocsTopicPath));

			Set<String> collapsedSections = state.collapsedDocsSections == null ? new HashSet<String>() : state.collapsedDocsSections;
			buffer.writeInt(collapsedSections.size());
			for(String sectionKey : collapsedSections) {
				buffer.writeString(safeString(sectionKey));
			}
		}
	}

	public ComputerModule getModule(SegmentPiece segmentPiece) {
		if(computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			return computerModules.get(segmentPiece.getAbsoluteIndex());
		}
		return null;
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		synchronized(pendingLock) {
			computerModules.clear();
			pendingModuleStates.clear();

			byte version = buffer.readByte();
			if(version < 1 || version > VERSION) {
				LuaMade.getInstance().logWarning("Unsupported ComputerModuleContainer tag version " + version + " (expected 1.." + VERSION + ")");
				return;
			}

			int count = buffer.readInt();
			for(int i = 0; i < count; i++) {
				long abs = buffer.readLong();
				String stableUUID = version >= 5 ? buffer.readString() : ComputerModule.generateLegacyComputerUUID(abs);
				byte modeOrdinal = buffer.readByte();
				String lastOpenFile = buffer.readString();
				String savedTerminalInput = buffer.readString();
				String hostname = buffer.readString();
				String displayName = version >= 2 ? buffer.readString() : "";
				String lastDocsTopicPath = version >= 3 ? buffer.readString() : "";
				Set<String> collapsedDocsSections = new HashSet<>();
				if(version >= 4) {
					int collapsedCount = buffer.readInt();
					for(int collapsedIndex = 0; collapsedIndex < collapsedCount; collapsedIndex++) {
						collapsedDocsSections.add(buffer.readString());
					}
				}

				pendingModuleStates.put(abs, new PendingModuleState(stableUUID, modeOrdinal, lastOpenFile, savedTerminalInput, hostname, displayName, lastDocsTopicPath, collapsedDocsSections));
			}
			// Do NOT call restorePendingModules() here – the segment buffer may not be
			// fully populated yet during deserialization.  handle(Timer) will pick it up.
		}
	}

	public void addModule(SegmentPiece segmentPiece) {
		if(!computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			String stableUUID = ComputerModule.generateComputerUUID(segmentPiece);
			ComputerModule module = new ComputerModule(segmentPiece, stableUUID);
			applyPendingState(segmentPiece.getAbsoluteIndex(), module);
			computerModules.put(segmentPiece.getAbsoluteIndex(), module);
			flagUpdatedData();
		}
	}

	public ComputerModule getOrCreateModule(SegmentPiece segmentPiece) {
		ComputerModule module = getModule(segmentPiece);
		if(module == null) {
			addModule(segmentPiece);
			module = getModule(segmentPiece);
		}
		if(module != null) {
			module.setTouched();
		}
		return module;
	}

	public void removeModule(SegmentPiece segmentPiece) {
		if(computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			ComputerModule module = computerModules.get(segmentPiece.getAbsoluteIndex());
			if(module != null) {
				module.saveAndCleanup();
			}
			computerModules.remove(segmentPiece.getAbsoluteIndex());
			flagUpdatedData();
		}
	}

	public void updateModule(ComputerModule module) {
		if(computerModules.containsKey(module.getSegmentPiece().getAbsoluteIndex())) {
			computerModules.put(module.getSegmentPiece().getAbsoluteIndex(), module);
			module.setTouched();
			flagUpdatedData();
		}
	}

	public void saveAndCleanupAllModules() {
		for(ComputerModule module : computerModules.values()) {
			module.saveAndCleanup();
		}
	}

	private void collectKnownUUIDs(Set<String> uuids) {
		for(ComputerModule module : computerModules.values()) {
			if(module != null && module.getUUID() != null && !module.getUUID().isEmpty()) {
				uuids.add(module.getUUID());
			}
		}

		synchronized(pendingLock) {
			for(long abs : pendingModuleStates.keySet().toLongArray()) {
				PendingModuleState pendingState = pendingModuleStates.get(abs);
				if(pendingState != null && pendingState.stableUUID != null && !pendingState.stableUUID.isEmpty()) {
					uuids.add(pendingState.stableUUID);
				} else {
					uuids.add(ComputerModule.generateLegacyComputerUUID(abs));
				}
			}
		}
	}

	private void restorePendingModules() {
		if(pendingModuleStates.isEmpty() || segmentController == null) {
			return;
		}

		// Snapshot the key set into a plain array – this avoids holding a live
		// fastutil iterator across any map mutations that may happen concurrently.
		long[] keys = pendingModuleStates.keySet().toLongArray();

		for(long abs : keys) {
			PendingModuleState state = pendingModuleStates.get(abs);
			if(state == null) {
				continue; // Already removed by a concurrent addModule() call
			}

			SegmentPiece piece;
			try {
				piece = segmentController.getSegmentBuffer().getPointUnsave(abs);
			} catch(Exception e) {
				// Segment buffer not ready – leave the entry in the map and retry next tick
				continue;
			}

			if(piece == null || piece.getType() != ElementRegistry.COMPUTER.getId()) {
				// Segment not yet loaded or not a computer block – try again next tick
				continue;
			}

			// Remove from pending *before* constructing the module so that any
			// re-entrant call (e.g. from addModule) sees the entry as gone.
			pendingModuleStates.remove(abs);

			if(!computerModules.containsKey(abs)) {
				String stableUUID = state.stableUUID == null || state.stableUUID.isEmpty()
					? ComputerModule.generateComputerUUID(piece)
					: state.stableUUID;
				ComputerModule module = new ComputerModule(piece, stableUUID);
				applyStateToModule(module, state);
				computerModules.put(abs, module);
			}
		}
	}

	private void applyPendingState(long abs, ComputerModule module) {
		synchronized(pendingLock) {
			PendingModuleState state = pendingModuleStates.remove(abs);
			if(state != null) {
				applyStateToModule(module, state);
			}
		}
	}

	private void applyStateToModule(ComputerModule module, PendingModuleState state) {
		if(module == null || state == null) {
			return;
		}

		ComputerModule.ComputerMode[] modes = ComputerModule.ComputerMode.values();
		ComputerModule.ComputerMode mode = ComputerModule.ComputerMode.OFF;
		if(state.modeOrdinal >= 0 && state.modeOrdinal < modes.length) {
			mode = modes[state.modeOrdinal];
		}

		module.restoreSerializedState(mode, state.lastOpenFile, state.savedTerminalInput, state.hostname, state.displayName, state.lastDocsTopicPath, state.collapsedDocsSections);
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private static final class PendingModuleState {
		private final String stableUUID;
		private final byte modeOrdinal;
		private final String lastOpenFile;
		private final String savedTerminalInput;
		private final String hostname;
		private final String displayName;
		private final String lastDocsTopicPath;
		private final Set<String> collapsedDocsSections;

		private PendingModuleState(String stableUUID, byte modeOrdinal, String lastOpenFile, String savedTerminalInput, String hostname, String displayName, String lastDocsTopicPath, Set<String> collapsedDocsSections) {
			this.stableUUID = stableUUID == null ? "" : stableUUID;
			this.modeOrdinal = modeOrdinal;
			this.lastOpenFile = lastOpenFile;
			this.savedTerminalInput = savedTerminalInput;
			this.hostname = hostname;
			this.displayName = displayName;
			this.lastDocsTopicPath = lastDocsTopicPath;
			this.collapsedDocsSections = collapsedDocsSections == null ? new HashSet<>() : new HashSet<>(collapsedDocsSections);
		}

		private static PendingModuleState fromModule(ComputerModule module) {
			if(module == null) {
				return new PendingModuleState("", (byte) ComputerModule.ComputerMode.OFF.ordinal(), "", "", "", "", "", new HashSet<String>());
			}

			return new PendingModuleState(
				module.getUUID(),
				(byte) module.getLastMode().ordinal(),
				module.getLastOpenFile(),
				module.getSavedTerminalInput(),
				module.getNetworkInterface() == null ? "" : module.getNetworkInterface().getHostname(),
				module.getDisplayName(),
				module.getLastDocsTopicPath(),
				module.getCollapsedDocsSections()
			);
		}
	}
}
