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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Client → Server: command history / autocomplete queries against a
 * computer's server-side {@code Terminal}. Split out from
 * {@link PacketCSComputerInput} because these need an async reply
 * ({@link PacketSCTerminalQueryResult}) rather than being fire-and-forget.
 *
 * <p>Note: history navigation shares one {@code historyIndex} cursor per
 * {@code Terminal} (not per-viewer) — if two players have the same computer
 * open and both press Up/Down, they interact with the same cursor. This
 * matches the shared-session model the rest of this migration already
 * established (one true server-side session, not one per viewer).
 */
public class PacketCSTerminalQuery extends Packet {

	public enum Kind { HISTORY_PREV, HISTORY_NEXT, COMMAND_SUGGESTIONS, PATH_SUGGESTIONS }

	private int requestId;
	private String kind;
	private int entityId;
	private long absIndex;
	private String inputText;

	public PacketCSTerminalQuery() {
	}

	private PacketCSTerminalQuery(int requestId, Kind kind, int entityId, long absIndex, String inputText) {
		this.requestId = requestId;
		this.kind = kind.name();
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.inputText = inputText == null ? "" : inputText;
	}

	public static PacketCSTerminalQuery historyPrev(int requestId, int entityId, long absIndex, String currentInput) {
		return new PacketCSTerminalQuery(requestId, Kind.HISTORY_PREV, entityId, absIndex, currentInput);
	}

	public static PacketCSTerminalQuery historyNext(int requestId, int entityId, long absIndex) {
		return new PacketCSTerminalQuery(requestId, Kind.HISTORY_NEXT, entityId, absIndex, "");
	}

	public static PacketCSTerminalQuery commandSuggestions(int requestId, int entityId, long absIndex, String partialInput) {
		return new PacketCSTerminalQuery(requestId, Kind.COMMAND_SUGGESTIONS, entityId, absIndex, partialInput);
	}

	public static PacketCSTerminalQuery pathSuggestions(int requestId, int entityId, long absIndex, String partialInput) {
		return new PacketCSTerminalQuery(requestId, Kind.PATH_SUGGESTIONS, entityId, absIndex, partialInput);
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		kind = buffer.readString();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		inputText = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeString(kind);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(inputText);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		Kind parsedKind;
		try {
			parsedKind = Kind.valueOf(kind);
		} catch(Exception ex) {
			return;
		}

		SegmentPiece piece = ComputerPacketUtil.resolveComputerPiece(entityId, absIndex);
		ComputerModuleContainer container = piece == null ? null : ComputerPacketUtil.resolveContainer(piece);
		if(container == null || !container.isViewer(absIndex, sender)) {
			return;
		}
		ComputerModule module = container.getModule(piece);
		if(module == null || module.getTerminal() == null) {
			return;
		}

		switch(parsedKind) {
			case HISTORY_PREV: {
				module.getTerminal().setCurrentInput(inputText == null ? "" : inputText);
				String command = module.getTerminal().getPreviousCommand();
				PacketUtil.sendPacket(sender, PacketSCTerminalQueryResult.historyResult(requestId, command));
				break;
			}
			case HISTORY_NEXT: {
				String command = module.getTerminal().getNextCommand();
				PacketUtil.sendPacket(sender, PacketSCTerminalQueryResult.historyResult(requestId, command));
				break;
			}
			case COMMAND_SUGGESTIONS: {
				List<String> suggestions = module.getTerminal().getCommandSuggestions(inputText);
				PacketUtil.sendPacket(sender, PacketSCTerminalQueryResult.suggestionsResult(requestId, suggestions == null ? Collections.emptyList() : suggestions, false));
				break;
			}
			case PATH_SUGGESTIONS: {
				List<String> suggestions = module.getTerminal().getPathSuggestions(inputText);
				PacketUtil.sendPacket(sender, PacketSCTerminalQueryResult.suggestionsResult(requestId, suggestions == null ? new ArrayList<>() : suggestions, true));
				break;
			}
			default:
				break;
		}
	}
}
