package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

public class AccessPointModuleContainer extends SystemModule {

	private static final byte VERSION = 1;
	private final Long2ObjectOpenHashMap<String> linkedComputerUuids = new Long2ObjectOpenHashMap<>();

	public AccessPointModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.REMOTE_ACCESS_POINT.getId());
	}

	public static AccessPointModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.REMOTE_ACCESS_POINT.getId()) instanceof AccessPointModuleContainer) {
			return (AccessPointModuleContainer) managerContainer.getModMCModule(ElementRegistry.REMOTE_ACCESS_POINT.getId());
		}
		return null;
	}

	@Override
	public void handlePlace(long abs, byte orientation) {
	}

	@Override
	public void handleRemove(long abs) {
		linkedComputerUuids.remove(abs);
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
		return "Remote Access Point";
	}

	@Override
	public void handle(Timer timer) {
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		buffer.writeInt(linkedComputerUuids.size());
		for(long abs : linkedComputerUuids.keySet().toLongArray()) {
			buffer.writeLong(abs);
			buffer.writeString(safeString(linkedComputerUuids.get(abs)));
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		linkedComputerUuids.clear();
		byte version = buffer.readByte();
		if(version != VERSION) {
			return;
		}
		int count = buffer.readInt();
		for(int index = 0; index < count; index++) {
			linkedComputerUuids.put(buffer.readLong(), buffer.readString());
		}
	}

	public void linkToComputer(SegmentPiece accessPoint, String computerUuid) {
		if(accessPoint == null || computerUuid == null || computerUuid.trim().isEmpty()) {
			return;
		}
		linkedComputerUuids.put(accessPoint.getAbsoluteIndex(), computerUuid.trim());
		flagUpdatedData();
	}

	public void unlink(SegmentPiece accessPoint) {
		if(accessPoint == null) {
			return;
		}
		linkedComputerUuids.remove(accessPoint.getAbsoluteIndex());
		flagUpdatedData();
	}

	public void removeAccessPoint(long absIndex) {
		if(linkedComputerUuids.remove(absIndex) != null) {
			flagUpdatedData();
		}
	}

	public String getLinkedComputerUUID(SegmentPiece accessPoint) {
		if(accessPoint == null) {
			return null;
		}
		return linkedComputerUuids.get(accessPoint.getAbsoluteIndex());
	}

	private static String safeString(String value) {
		return value == null ? "" : value;
	}
}
