package luamade.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.manager.LuaManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class TerminateScriptPacket extends Packet {

	private SegmentController segmentController;
	private long index;

	public TerminateScriptPacket() {

	}

	public TerminateScriptPacket(SegmentController segmentController, long index) {
		this.segmentController = segmentController;
		this.index = index;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		segmentController = (SegmentController) packetReadBuffer.readSendable();
		index = packetReadBuffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeSendable(segmentController);
		packetWriteBuffer.writeLong(index);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		try {
			SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(index);
			if(segmentPiece != null) LuaManager.terminate(segmentPiece);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
