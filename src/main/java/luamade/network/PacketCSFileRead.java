package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Client → Server: read a file from a computer's server-side {@code FileSystem}.
 * Needed the moment script execution moved server-side — the in-terminal
 * {@code edit} command and the Swing {@link luamade.gui.editor.SwingEditorFrame}
 * must read/write the same {@code FileSystem} the server's {@code Terminal}
 * actually runs scripts against, or edits would silently not apply.
 */
public class PacketCSFileRead extends Packet {

	private int requestId;
	private int entityId;
	private long absIndex;
	private String path;

	public PacketCSFileRead() {
	}

	public PacketCSFileRead(int requestId, int entityId, long absIndex, String path) {
		this.requestId = requestId;
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.path = path == null ? "" : path;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		path = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(path);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		SegmentPiece piece = ComputerPacketUtil.resolveComputerPiece(entityId, absIndex);
		ComputerModuleContainer container = piece == null ? null : ComputerPacketUtil.resolveContainer(piece);
		if(container == null || !container.isViewer(absIndex, sender)) {
			PacketUtil.sendPacket(sender, PacketSCFileContents.failure(requestId, "No active session for this computer"));
			return;
		}

		ComputerModule module = container.getModule(piece);
		if(module == null) {
			PacketUtil.sendPacket(sender, PacketSCFileContents.failure(requestId, "Computer unavailable"));
			return;
		}

		String normalized = module.getFileSystem().normalizePath(path);
		if(!module.getFileSystem().exists(normalized) || module.getFileSystem().isDir(normalized)) {
			PacketUtil.sendPacket(sender, PacketSCFileContents.failure(requestId, "File not found: " + normalized));
			return;
		}

		String content = module.getFileSystem().read(normalized);
		PacketUtil.sendPacket(sender, PacketSCFileContents.success(requestId, content == null ? "" : content));
	}
}
