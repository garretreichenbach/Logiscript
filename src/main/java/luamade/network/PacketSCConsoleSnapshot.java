package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: steady-state console text push for a computer session the
 * receiving client currently has open. Sent only when the text actually
 * changed since the last push (see {@code ComputerModuleContainer}'s
 * output-push sweep), not every tick.
 */
public class PacketSCConsoleSnapshot extends Packet {

	private int entityId;
	private long absIndex;
	private String text;
	private boolean keyboardConsumed;
	private boolean mouseConsumed;
	private byte modeOrdinal;
	private String lastOpenFile;
	private boolean passwordInputMode;
	private byte scrollModeOrdinal;
	private String savedTerminalInput;

	public PacketSCConsoleSnapshot() {
	}

	public PacketSCConsoleSnapshot(int entityId, long absIndex, String text, boolean keyboardConsumed, boolean mouseConsumed, byte modeOrdinal, String lastOpenFile, boolean passwordInputMode, byte scrollModeOrdinal, String savedTerminalInput) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.text = text == null ? "" : text;
		this.keyboardConsumed = keyboardConsumed;
		this.mouseConsumed = mouseConsumed;
		this.modeOrdinal = modeOrdinal;
		this.lastOpenFile = lastOpenFile == null ? "" : lastOpenFile;
		this.passwordInputMode = passwordInputMode;
		this.scrollModeOrdinal = scrollModeOrdinal;
		this.savedTerminalInput = savedTerminalInput == null ? "" : savedTerminalInput;
	}

	public int getEntityId() {
		return entityId;
	}

	public long getAbsIndex() {
		return absIndex;
	}

	public String getText() {
		return text;
	}

	public boolean isKeyboardConsumed() {
		return keyboardConsumed;
	}

	public boolean isMouseConsumed() {
		return mouseConsumed;
	}

	public byte getModeOrdinal() {
		return modeOrdinal;
	}

	public String getLastOpenFile() {
		return lastOpenFile;
	}

	public boolean isPasswordInputMode() {
		return passwordInputMode;
	}

	public byte getScrollModeOrdinal() {
		return scrollModeOrdinal;
	}

	public String getSavedTerminalInput() {
		return savedTerminalInput;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		text = buffer.readString();
		keyboardConsumed = buffer.readBoolean();
		mouseConsumed = buffer.readBoolean();
		modeOrdinal = buffer.readByte();
		lastOpenFile = buffer.readString();
		passwordInputMode = buffer.readBoolean();
		scrollModeOrdinal = buffer.readByte();
		savedTerminalInput = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(text);
		buffer.writeBoolean(keyboardConsumed);
		buffer.writeBoolean(mouseConsumed);
		buffer.writeByte(modeOrdinal);
		buffer.writeString(lastOpenFile);
		buffer.writeBoolean(passwordInputMode);
		buffer.writeByte(scrollModeOrdinal);
		buffer.writeString(savedTerminalInput);
	}

	@Override
	public void processPacketOnClient() {
		luamade.gui.ComputerSessionRegistry.handleConsoleSnapshot(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
