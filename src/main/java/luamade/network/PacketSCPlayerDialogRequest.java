package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: ask a specific player to see an OK/Cancel dialog on behalf
 * of a running script ({@code player.confirm()} / {@code player.message()}).
 * The client replies with {@link PacketCSPlayerDialogResponse} using the same
 * {@code requestId}.
 */
public class PacketSCPlayerDialogRequest extends Packet {

	private int requestId;
	private String title;
	private String body;

	public PacketSCPlayerDialogRequest() {
	}

	public PacketSCPlayerDialogRequest(int requestId, String title, String body) {
		this.requestId = requestId;
		this.title = title == null ? "" : title;
		this.body = body == null ? "" : body;
	}

	public int getRequestId() {
		return requestId;
	}

	public String getTitle() {
		return title;
	}

	public String getBody() {
		return body;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		title = buffer.readString();
		body = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeString(title);
		buffer.writeString(body);
	}

	@Override
	public void processPacketOnClient() {
		luamade.gui.PlayerDialogPresenter.present(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
