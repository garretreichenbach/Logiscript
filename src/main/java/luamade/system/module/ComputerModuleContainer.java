package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends SystemModule {

	private final byte VERSION = 2;
	private static final Set<ComputerModuleContainer> ACTIVE_CONTAINERS = ConcurrentHashMap.newKeySet();
	private final Long2ObjectOpenHashMap<ComputerModule> computerModules = new Long2ObjectOpenHashMap<>();
	private final Long2ObjectOpenHashMap<PendingModuleState> pendingModuleStates = new Long2ObjectOpenHashMap<>();

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
		if(!pendingModuleStates.isEmpty()) {
			restorePendingModules();
		}
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		buffer.writeInt(computerModules.size());
		for(ComputerModule module : computerModules.values()) {
			long abs = module.getSegmentPiece().getAbsoluteIndex();
			buffer.writeLong(abs);
			buffer.writeByte((byte) module.getLastMode().ordinal());
			buffer.writeString(safeString(module.getLastOpenFile()));
			buffer.writeString(safeString(module.getSavedTerminalInput()));
			buffer.writeString(safeString(module.getNetworkInterface().getHostname()));
			buffer.writeString(safeString(module.getDisplayName()));
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
			byte modeOrdinal = buffer.readByte();
			String lastOpenFile = buffer.readString();
			String savedTerminalInput = buffer.readString();
			String hostname = buffer.readString();
			String displayName = version >= 2 ? buffer.readString() : "";

			pendingModuleStates.put(abs, new PendingModuleState(modeOrdinal, lastOpenFile, savedTerminalInput, hostname, displayName));
		}

		restorePendingModules();
	}

	public void addModule(SegmentPiece segmentPiece) {
		if(!computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			String stableUUID = ComputerModule.generateComputerUUID(segmentPiece.getAbsoluteIndex());
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

	private void restorePendingModules() {
		Iterator<Long2ObjectMap.Entry<PendingModuleState>> iterator = pendingModuleStates.long2ObjectEntrySet().iterator();
		while(iterator.hasNext()) {
			Long2ObjectMap.Entry<PendingModuleState> entry = iterator.next();
			long abs = entry.getLongKey();
			SegmentPiece piece = segmentController.getSegmentBuffer().getPointUnsave(abs);
			if(piece == null || piece.getType() != ElementRegistry.COMPUTER.getId()) {
				continue;
			}

			if(!computerModules.containsKey(abs)) {
				String stableUUID = ComputerModule.generateComputerUUID(abs);
				ComputerModule module = new ComputerModule(piece, stableUUID);
				applyStateToModule(module, entry.getValue());
				computerModules.put(abs, module);
			}
			iterator.remove();
		}
	}

	private void applyPendingState(long abs, ComputerModule module) {
		PendingModuleState state = pendingModuleStates.remove(abs);
		if(state != null) {
			applyStateToModule(module, state);
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

		module.restoreSerializedState(mode, state.lastOpenFile, state.savedTerminalInput, state.hostname, state.displayName);
	}

	private String safeString(String value) {
		return value == null ? "" : value;
	}

	private static final class PendingModuleState {
		private final byte modeOrdinal;
		private final String lastOpenFile;
		private final String savedTerminalInput;
		private final String hostname;
		private final String displayName;

		private PendingModuleState(byte modeOrdinal, String lastOpenFile, String savedTerminalInput, String hostname, String displayName) {
			this.modeOrdinal = modeOrdinal;
			this.lastOpenFile = lastOpenFile;
			this.savedTerminalInput = savedTerminalInput;
			this.hostname = hostname;
			this.displayName = displayName;
		}
	}
}
