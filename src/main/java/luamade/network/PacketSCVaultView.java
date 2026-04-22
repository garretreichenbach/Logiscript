package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.gui.VaultDialog;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Server → Client: delivers the current vault view (balance, player credits,
 * access level) in response to a {@link PacketCSRequestVaultView} or any
 * deposit/withdraw that needs to refresh the open dialog.
 *
 * <p>The client caches the payload in {@link ClientVaultCache} and either
 * opens the {@link VaultDialog} (fresh interact) or nudges the already-open
 * dialog to re-read from the cache.
 */
public class PacketSCVaultView extends Packet {

	private String uuid;
	private int entityId;
	private long absIndex;
	private long balance;
	private long playerCredits;
	private String accessLevel;
	/** Empty when the view is routine; populated on deposit/withdraw failures or success. */
	private String statusMessage;

	/** Required no-arg constructor for packet instantiation. */
	public PacketSCVaultView() {
	}

	public PacketSCVaultView(String uuid, int entityId, long absIndex, long balance, long playerCredits, String accessLevel) {
		this(uuid, entityId, absIndex, balance, playerCredits, accessLevel, "");
	}

	public PacketSCVaultView(String uuid, int entityId, long absIndex, long balance, long playerCredits, String accessLevel, String statusMessage) {
		this.uuid = uuid;
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.balance = balance;
		this.playerCredits = playerCredits;
		this.accessLevel = accessLevel == null ? "NONE" : accessLevel;
		this.statusMessage = statusMessage == null ? "" : statusMessage;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		uuid = buffer.readString();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		balance = buffer.readLong();
		playerCredits = buffer.readLong();
		accessLevel = buffer.readString();
		statusMessage = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeString(uuid);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeLong(balance);
		buffer.writeLong(playerCredits);
		buffer.writeString(accessLevel);
		buffer.writeString(statusMessage);
	}

	@Override
	public void processPacketOnClient() {
		ClientVaultCache.View view = new ClientVaultCache.View(uuid, entityId, absIndex, balance, playerCredits, accessLevel);
		ClientVaultCache.update(view);
		VaultDialog.onViewReceived(view, statusMessage);
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Server → Client only.
	}
}
