package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.gfx.Gfx2d;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: steady-state gfx2d frame push for a computer session the
 * receiving client currently has open. Sent only when {@code Gfx2d.revision}
 * changed since the last push, reusing the same dirty-tracking {@code Gfx2d}
 * already does internally — this just relocates it across the wire.
 */
public class PacketSCGfxSnapshot extends Packet {

	private int entityId;
	private long absIndex;
	private Gfx2d.FrameSnapshot snapshot;

	public PacketSCGfxSnapshot() {
	}

	public PacketSCGfxSnapshot(int entityId, long absIndex, Gfx2d.FrameSnapshot snapshot) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.snapshot = snapshot;
	}

	public int getEntityId() {
		return entityId;
	}

	public long getAbsIndex() {
		return absIndex;
	}

	public Gfx2d.FrameSnapshot getSnapshot() {
		return snapshot;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		snapshot = GfxSnapshotCodec.read(buffer);
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		GfxSnapshotCodec.write(buffer, snapshot);
	}

	@Override
	public void processPacketOnClient() {
		luamade.gui.ComputerSessionRegistry.handleGfxSnapshot(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
