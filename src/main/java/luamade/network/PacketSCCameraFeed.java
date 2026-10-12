package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.gui.ComputerSessionRegistry;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/** Server → Client: the Camera block a computer's remote controllers should see through ({@code cameraEntityId} -1 = none). */
public class PacketSCCameraFeed extends Packet {

	private int entityId;
	private long absIndex;
	private int cameraEntityId;
	private long cameraAbsIndex;

	public PacketSCCameraFeed() {
	}

	public PacketSCCameraFeed(int entityId, long absIndex, int cameraEntityId, long cameraAbsIndex) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.cameraEntityId = cameraEntityId;
		this.cameraAbsIndex = cameraAbsIndex;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		cameraEntityId = buffer.readInt();
		cameraAbsIndex = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeInt(cameraEntityId);
		buffer.writeLong(cameraAbsIndex);
	}

	@Override
	public void processPacketOnClient() {
		ComputerSessionRegistry.handleCameraFeed(entityId, absIndex, cameraEntityId, cameraAbsIndex);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
