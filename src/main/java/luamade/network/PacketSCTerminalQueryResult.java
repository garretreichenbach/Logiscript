package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Server → Client: response to {@link PacketCSTerminalQuery}. Routed by
 * {@code requestId} through {@code ComputerSessionRegistry}'s pending-query
 * map (these responses don't carry entityId/absIndex — the requestId alone
 * identifies which {@code ComputerSessionView} is waiting).
 */
public class PacketSCTerminalQueryResult extends Packet {

	private int requestId;
	private boolean historyKind;
	private String resultText;
	private List<String> resultList;
	private boolean pathMode;

	public PacketSCTerminalQueryResult() {
	}

	private PacketSCTerminalQueryResult(int requestId, boolean historyKind, String resultText, List<String> resultList, boolean pathMode) {
		this.requestId = requestId;
		this.historyKind = historyKind;
		this.resultText = resultText == null ? "" : resultText;
		this.resultList = resultList == null ? new ArrayList<>() : resultList;
		this.pathMode = pathMode;
	}

	public static PacketSCTerminalQueryResult historyResult(int requestId, String command) {
		return new PacketSCTerminalQueryResult(requestId, true, command, Collections.emptyList(), false);
	}

	public static PacketSCTerminalQueryResult suggestionsResult(int requestId, List<String> suggestions, boolean pathMode) {
		return new PacketSCTerminalQueryResult(requestId, false, "", suggestions, pathMode);
	}

	public int getRequestId() {
		return requestId;
	}

	public boolean isHistoryKind() {
		return historyKind;
	}

	public String getResultText() {
		return resultText;
	}

	public List<String> getResultList() {
		return resultList;
	}

	public boolean isPathMode() {
		return pathMode;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		historyKind = buffer.readBoolean();
		resultText = buffer.readString();
		resultList = buffer.readStringList();
		pathMode = buffer.readBoolean();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeBoolean(historyKind);
		buffer.writeString(resultText);
		buffer.writeStringList(resultList == null ? new ArrayList<>() : resultList);
		buffer.writeBoolean(pathMode);
	}

	@Override
	public void processPacketOnClient() {
		luamade.gui.ComputerSessionRegistry.handleTerminalQueryResult(this);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
