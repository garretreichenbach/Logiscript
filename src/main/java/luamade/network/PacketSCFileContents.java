package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/** Server → Client: response to {@link PacketCSFileRead}. */
public class PacketSCFileContents extends Packet {

	private int requestId;
	private boolean success;
	private String message;
	private String content;

	public PacketSCFileContents() {
	}

	private PacketSCFileContents(int requestId, boolean success, String message, String content) {
		this.requestId = requestId;
		this.success = success;
		this.message = message == null ? "" : message;
		this.content = content == null ? "" : content;
	}

	public static PacketSCFileContents success(int requestId, String content) {
		return new PacketSCFileContents(requestId, true, "", content);
	}

	public static PacketSCFileContents failure(int requestId, String message) {
		return new PacketSCFileContents(requestId, false, message, "");
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

	public String getContent() {
		return content;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		success = buffer.readBoolean();
		message = buffer.readString();
		content = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeBoolean(success);
		buffer.writeString(message);
		buffer.writeString(content);
	}

	@Override
	public void processPacketOnClient() {
		luamade.lua.fs.FileIoRequests.complete(requestId, new luamade.lua.fs.FileIoRequests.ReadResult(success, message, content));
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
