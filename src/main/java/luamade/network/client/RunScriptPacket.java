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
public class RunScriptPacket extends Packet {

	private SegmentController segmentController;
	private long index;
	private String script;

	public RunScriptPacket() {

	}

	public RunScriptPacket(SegmentController segmentController, long index, String script) {
		this.segmentController = segmentController;
		this.index = index;
		this.script = script;
	}

	@Override
	public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {
		segmentController = (SegmentController) packetReadBuffer.readSendable();
		index = packetReadBuffer.readLong();
		script = packetReadBuffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {
		packetWriteBuffer.writeSendable(segmentController);
		packetWriteBuffer.writeLong(index);
		packetWriteBuffer.writeString(script);
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
			if(segmentPiece != null) {
				module.setScript(segmentPiece, script);
				module.flagUpdatedData();
				module.runScript(segmentPiece);
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
