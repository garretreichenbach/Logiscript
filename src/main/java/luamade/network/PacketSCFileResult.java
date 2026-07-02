package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/** Server → Client: response to {@link PacketCSFileWrite}. */
public class PacketSCFileResult extends Packet {

	private int requestId;
	private boolean success;
	private String message;

	public PacketSCFileResult() {
	}

	private PacketSCFileResult(int requestId, boolean success, String message) {
		this.requestId = requestId;
		this.success = success;
		this.message = message == null ? "" : message;
	}

	public static PacketSCFileResult success(int requestId) {
		return new PacketSCFileResult(requestId, true, "");
	}

	public static PacketSCFileResult failure(int requestId, String message) {
		return new PacketSCFileResult(requestId, false, message);
	}

	public int getRequestId() {
		return requestId;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		success = buffer.readBoolean();
		message = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeBoolean(success);
		buffer.writeString(message);
	}

	@Override
	public void processPacketOnClient() {
		luamade.lua.fs.FileIoRequests.complete(requestId, new luamade.lua.fs.FileIoRequests.ReadResult(success, message, null));
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
