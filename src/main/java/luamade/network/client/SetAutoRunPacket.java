package luamade.network.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.element.ElementManager;
import luamade.system.module.ComputerModule;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SetAutoRunPacket extends Packet {

	private SegmentController segmentController;
	private long index;
	private boolean autoRun;

	public SetAutoRunPacket() {

	}

	public SetAutoRunPacket(SegmentController segmentController, long index, boolean autoRun) {
		this.segmentController = segmentController;
		this.index = index;
		this.autoRun = autoRun;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		segmentController = (SegmentController) packetReadBuffer.readSendable();
		index = packetReadBuffer.readLong();
		autoRun = packetReadBuffer.readBoolean();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeSendable(segmentController);
		packetWriteBuffer.writeLong(index);
		packetWriteBuffer.writeBoolean(autoRun);
	}

	@Override
	public void processPacketOnClient() {

	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		try {
			ManagedSegmentController<?> controller = (ManagedSegmentController<?>) segmentController;
			ComputerModule module = (ComputerModule) controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId());
			SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(index);
			if(segmentPiece != null) module.setAutoRun(segmentPiece, autoRun);
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
