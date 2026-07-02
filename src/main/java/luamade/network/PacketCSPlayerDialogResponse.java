package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.player.PlayerDialogRequests;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/** Client → Server: response to {@link PacketSCPlayerDialogRequest}. */
public class PacketCSPlayerDialogResponse extends Packet {

	private int requestId;
	private boolean result;

	public PacketCSPlayerDialogResponse() {
	}

	public PacketCSPlayerDialogResponse(int requestId, boolean result) {
		this.requestId = requestId;
		this.result = result;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		result = buffer.readBoolean();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeBoolean(result);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		PlayerDialogRequests.complete(requestId, result);
	}
}
