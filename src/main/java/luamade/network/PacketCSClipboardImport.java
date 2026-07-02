package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client → Server: import clipboard-pasted files into a computer's
 * server-side {@code FileSystem}, via the same
 * {@code Terminal#importClipboardFiles}/{@code importClipboardProtocol}
 * entry points used when the terminal ran client-side (bundle parsing, size
 * limits, and status messages all still live there — this just carries the
 * raw clipboard payload to them instead of calling them in-process).
 *
 * <p>Fire-and-forget: any status/error feedback reaches the client through
 * the normal console-sync push, since those methods already print to the
 * (now server-side) console.
 */
public class PacketCSClipboardImport extends Packet {

	private int entityId;
	private long absIndex;
	/** Raw clipboard text for the bundle-protocol path; empty when importing a discrete file map. */
	private String protocolText;
	private Map<String, String> files;

	public PacketCSClipboardImport() {
	}

	public static PacketCSClipboardImport ofProtocolText(int entityId, long absIndex, String protocolText) {
		PacketCSClipboardImport packet = new PacketCSClipboardImport();
		packet.entityId = entityId;
		packet.absIndex = absIndex;
		packet.protocolText = protocolText == null ? "" : protocolText;
		packet.files = new LinkedHashMap<>();
		return packet;
	}

	public static PacketCSClipboardImport ofFiles(int entityId, long absIndex, Map<String, String> files) {
		PacketCSClipboardImport packet = new PacketCSClipboardImport();
		packet.entityId = entityId;
		packet.absIndex = absIndex;
		packet.protocolText = "";
		packet.files = files == null ? new LinkedHashMap<>() : new LinkedHashMap<>(files);
		return packet;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		protocolText = buffer.readString();
		int count = buffer.readInt();
		files = new LinkedHashMap<>();
		for(int i = 0; i < count; i++) {
			String path = buffer.readString();
			String content = buffer.readString();
			files.put(path, content);
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(protocolText == null ? "" : protocolText);
		Map<String, String> safeFiles = files == null ? new LinkedHashMap<>() : files;
		buffer.writeInt(safeFiles.size());
		for(Map.Entry<String, String> entry : safeFiles.entrySet()) {
			buffer.writeString(entry.getKey());
			buffer.writeString(entry.getValue() == null ? "" : entry.getValue());
		}
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
			return;
		}

		ComputerModule module = container.getModule(piece);
		if(module == null) {
			return;
		}
		module.setTouched();

		if(protocolText != null && !protocolText.isEmpty()) {
			if(module.getTerminal().importClipboardProtocol(protocolText, true)) {
				return;
			}
		}
		if(files != null && !files.isEmpty()) {
			module.getTerminal().importClipboardFiles(files, true, "clipboard");
		}
	}
}
