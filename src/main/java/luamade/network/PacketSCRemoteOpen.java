package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.gui.ComputerDialog;
import luamade.gui.RemoteControllerDialog;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/** Server → Client: a Remote Control action was accepted; open the linked computer's terminal or controller mode. */
public class PacketSCRemoteOpen extends Packet {

	private int entityId;
	private long absIndex;
	private boolean controller;
	private String computerName;

	public PacketSCRemoteOpen() {
	}

	public PacketSCRemoteOpen(int entityId, long absIndex, boolean controller, String computerName) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.controller = controller;
		this.computerName = computerName;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		controller = buffer.readBoolean();
		computerName = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeBoolean(controller);
		buffer.writeString(computerName == null ? "" : computerName);
	}

	@Override
	public void processPacketOnClient() {
		if(controller) {
			RemoteControllerDialog.open(entityId, absIndex, computerName);
		} else {
			ComputerDialog.open(entityId, absIndex);
		}
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
