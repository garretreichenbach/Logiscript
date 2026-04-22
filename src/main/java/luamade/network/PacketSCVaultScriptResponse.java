package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.vault.VaultScriptRequests;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: response for a {@link PacketCSVaultScriptOp}. The client
 * locates the waiting script future by {@code requestId} and unblocks it.
 */
public class PacketSCVaultScriptResponse extends Packet {

	private int requestId;
	private boolean success;
	private String message;
	private long balance;

	public PacketSCVaultScriptResponse() {
	}

	public PacketSCVaultScriptResponse(int requestId, boolean success, String message, long balance) {
		this.requestId = requestId;
		this.success = success;
		this.message = message == null ? "" : message;
		this.balance = balance;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		success = buffer.readBoolean();
		message = buffer.readString();
		balance = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeBoolean(success);
		buffer.writeString(message);
		buffer.writeLong(balance);
	}

	@Override
	public void processPacketOnClient() {
		VaultScriptRequests.complete(requestId, new VaultScriptRequests.Response(success, message, balance));
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
