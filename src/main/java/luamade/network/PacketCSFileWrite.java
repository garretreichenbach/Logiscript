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
 * Client → Server: write a file to a computer's server-side {@code FileSystem}.
 * See {@link PacketCSFileRead} for why this exists.
 */
public class PacketCSFileWrite extends Packet {

	private int requestId;
	private int entityId;
	private long absIndex;
	private String path;
	private String content;

	public PacketCSFileWrite() {
	}

	public PacketCSFileWrite(int requestId, int entityId, long absIndex, String path, String content) {
		this.requestId = requestId;
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.path = path == null ? "" : path;
		this.content = content == null ? "" : content;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		path = buffer.readString();
		content = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(path);
		buffer.writeString(content);
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
			PacketUtil.sendPacket(sender, PacketSCFileResult.failure(requestId, "No active session for this computer"));
			return;
		}

		ComputerModule module = container.getModule(piece);
		if(module == null) {
			PacketUtil.sendPacket(sender, PacketSCFileResult.failure(requestId, "Computer unavailable"));
			return;
		}

		String normalized = module.getFileSystem().normalizePath(path);
		boolean wrote = module.getFileSystem().write(normalized, content == null ? "" : content);
		module.setTouched();
		if(wrote) {
			PacketUtil.sendPacket(sender, PacketSCFileResult.success(requestId));
		} else {
			PacketUtil.sendPacket(sender, PacketSCFileResult.failure(requestId, "Failed to write: " + normalized));
		}
	}
}
