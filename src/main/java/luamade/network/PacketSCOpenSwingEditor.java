package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: open the Swing GUI editor for a file, on behalf of the
 * {@code edit} terminal command. The editor is a real OS window, so it must
 * be opened on whichever client ran the command — the server (where the
 * {@code edit} command now executes) only knows who to tell.
 */
public class PacketSCOpenSwingEditor extends Packet {

	private int entityId;
	private long absIndex;
	private String path;
	private String content;

	public PacketSCOpenSwingEditor() {
	}

	public PacketSCOpenSwingEditor(int entityId, long absIndex, String path, String content) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.path = path == null ? "" : path;
		this.content = content == null ? "" : content;
	}

	public int getEntityId() {
		return entityId;
	}

	public long getAbsIndex() {
		return absIndex;
	}

	public String getPath() {
		return path;
	}

	public String getContent() {
		return content;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		path = buffer.readString();
		content = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(path);
		buffer.writeString(content);
	}

	@Override
	public void processPacketOnClient() {
		luamade.gui.editor.SwingEditorFrame.openFromServer(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
