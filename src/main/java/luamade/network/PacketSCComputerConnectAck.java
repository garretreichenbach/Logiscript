package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.gfx.Gfx2d;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: result of a {@link PacketCSComputerInput.Kind#CONNECT}
 * request. On success, bundles the current console text and gfx2d frame so
 * the dialog isn't blank until the next steady-state sync push.
 */
public class PacketSCComputerConnectAck extends Packet {

	private int requestId;
	private int entityId;
	private long absIndex;
	private boolean success;
	private String message;
	private String consoleText;
	private Gfx2d.FrameSnapshot gfxSnapshot;
	private boolean keyboardConsumed;
	private boolean mouseConsumed;
	private byte modeOrdinal;
	private String lastOpenFile;
	private boolean passwordInputMode;
	private byte scrollModeOrdinal;
	private String savedTerminalInput;

	public PacketSCComputerConnectAck() {
	}

	private PacketSCComputerConnectAck(int requestId, int entityId, long absIndex, boolean success, String message, String consoleText, Gfx2d.FrameSnapshot gfxSnapshot, boolean keyboardConsumed, boolean mouseConsumed, byte modeOrdinal, String lastOpenFile, boolean passwordInputMode, byte scrollModeOrdinal, String savedTerminalInput) {
		this.requestId = requestId;
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.success = success;
		this.message = message == null ? "" : message;
		this.consoleText = consoleText == null ? "" : consoleText;
		this.gfxSnapshot = gfxSnapshot;
		this.keyboardConsumed = keyboardConsumed;
		this.mouseConsumed = mouseConsumed;
		this.modeOrdinal = modeOrdinal;
		this.lastOpenFile = lastOpenFile == null ? "" : lastOpenFile;
		this.passwordInputMode = passwordInputMode;
		this.scrollModeOrdinal = scrollModeOrdinal;
		this.savedTerminalInput = savedTerminalInput == null ? "" : savedTerminalInput;
	}

	public static PacketSCComputerConnectAck success(int requestId, int entityId, long absIndex, String consoleText, Gfx2d.FrameSnapshot gfxSnapshot, boolean keyboardConsumed, boolean mouseConsumed, byte modeOrdinal, String lastOpenFile, boolean passwordInputMode, byte scrollModeOrdinal, String savedTerminalInput) {
		return new PacketSCComputerConnectAck(requestId, entityId, absIndex, true, "", consoleText, gfxSnapshot, keyboardConsumed, mouseConsumed, modeOrdinal, lastOpenFile, passwordInputMode, scrollModeOrdinal, savedTerminalInput);
	}

	public static PacketSCComputerConnectAck failure(int requestId, int entityId, long absIndex, String message) {
		return new PacketSCComputerConnectAck(requestId, entityId, absIndex, false, message, "", null, false, false, (byte) 0, "", false, (byte) 0, "");
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

	public int getRequestId() {
		return requestId;
	}

	public int getEntityId() {
		return entityId;
	}

	public long getAbsIndex() {
		return absIndex;
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public String getConsoleText() {
		return consoleText;
	}

	public Gfx2d.FrameSnapshot getGfxSnapshot() {
		return gfxSnapshot;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		success = buffer.readBoolean();
		message = buffer.readString();
		consoleText = buffer.readString();
		boolean hasGfx = buffer.readBoolean();
		gfxSnapshot = hasGfx ? GfxSnapshotCodec.read(buffer) : null;
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
		buffer.writeInt(requestId);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeBoolean(success);
		buffer.writeString(message);
		buffer.writeString(consoleText);
		buffer.writeBoolean(gfxSnapshot != null);
		if(gfxSnapshot != null) {
			GfxSnapshotCodec.write(buffer, gfxSnapshot);
		}
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
		luamade.gui.ComputerSessionRegistry.handleConnectAck(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
