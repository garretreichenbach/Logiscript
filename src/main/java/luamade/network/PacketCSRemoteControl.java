package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import luamade.element.ElementRegistry;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Client → Server: a Remote Control action. {@link Kind#LINK} pairs the held remote with an access point the player is
 * next to; {@link Kind#TERMINAL} / {@link Kind#CONTROLLER} use a linked remote from anywhere. The server resolves the
 * link from its own copy of the item, answers with {@link PacketSCRemoteOpen}, and the session CONNECT that follows
 * passes the reach check through {@link RemoteControlLinks#hasRemoteFor}.
 */
public class PacketCSRemoteControl extends Packet {

	public enum Kind { LINK, TERMINAL, CONTROLLER }

	private Kind kind;
	private int slot;
	private int entityId;
	private long accessPointIndex;

	public PacketCSRemoteControl() {
	}

	public static PacketCSRemoteControl link(int slot, int entityId, long accessPointIndex) {
		PacketCSRemoteControl packet = new PacketCSRemoteControl();
		packet.kind = Kind.LINK;
		packet.slot = slot;
		packet.entityId = entityId;
		packet.accessPointIndex = accessPointIndex;
		return packet;
	}

	public static PacketCSRemoteControl use(int slot, boolean controller) {
		PacketCSRemoteControl packet = new PacketCSRemoteControl();
		packet.kind = controller ? Kind.CONTROLLER : Kind.TERMINAL;
		packet.slot = slot;
		return packet;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		int ordinal = buffer.readInt();
		kind = ordinal >= 0 && ordinal < Kind.values().length ? Kind.values()[ordinal] : null;
		slot = buffer.readInt();
		entityId = buffer.readInt();
		accessPointIndex = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(kind.ordinal());
		buffer.writeInt(slot);
		buffer.writeInt(entityId);
		buffer.writeLong(accessPointIndex);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		if(kind == null) {
			return;
		}
		if(kind == Kind.LINK) {
			SegmentPiece accessPoint = ComputerPacketUtil.resolvePiece(entityId, accessPointIndex, ElementRegistry.REMOTE_ACCESS_POINT.getId());
			if(accessPoint == null || !ComputerPacketUtil.isWithinConnectRange(sender, accessPoint)) {
				return;
			}
			String error = RemoteControlLinks.link(sender, slot, accessPoint);
			if(error != null) {
				notify(sender, error);
				return;
			}
		}
		SegmentPiece computer = RemoteControlLinks.resolveComputer(sender, slot);
		if(computer == null) {
			notify(sender, "This remote isn't linked, or its computer is missing or not loaded. Use it on a Remote Access Point to link it.");
			return;
		}
		String name = RemoteControlLinks.displayName(computer);
		PacketUtil.sendPacket(sender, new PacketSCRemoteOpen(computer.getSegmentController().getId(), computer.getAbsoluteIndex(), kind == Kind.CONTROLLER, name));
	}

	private static void notify(PlayerState player, String message) {
		player.sendServerMessagePlayerInfo(new Object[] {message});
	}
}
