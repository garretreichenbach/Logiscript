package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.vault.SharedVaultLedger;
import luamade.lua.vault.VaultAccessManager;
import luamade.system.module.VaultModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Client → Server: withdraw {@code amount} credits from the vault at
 * {@code (entityId, absIndex)} into the player's account.
 *
 * <p>Mirror of {@link PacketCSVaultDeposit} with access op = WITHDRAW. Replies
 * with {@link PacketSCVaultView} on both success and failure.
 */
public class PacketCSVaultWithdraw extends Packet {

	private int entityId;
	private long absIndex;
	private long amount;

	public PacketCSVaultWithdraw() {
	}

	public PacketCSVaultWithdraw(int entityId, long absIndex, long amount) {
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.amount = amount;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		amount = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeLong(amount);
	}

	@Override
	public void processPacketOnClient() {
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		SegmentPiece piece = PacketCSVaultDeposit.resolveVault(entityId, absIndex);
		if(piece == null) {
			PacketCSVaultDeposit.VaultPacketReply.refuse(sender, null, entityId, absIndex, "Vault not found");
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			PacketCSVaultDeposit.VaultPacketReply.refuse(sender, null, entityId, absIndex, "Vault module missing");
			return;
		}
		String uuid = container.getOrAssignUuid(absIndex);

		if(amount <= 0) {
			PacketCSVaultDeposit.VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Amount must be positive");
			return;
		}
		if(!VaultAccessManager.canAccess(piece, sender, VaultAccessManager.Op.WITHDRAW)) {
			PacketCSVaultDeposit.VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Access denied");
			return;
		}
		try {
			// Conservation transfer: debit vault first so a ledger failure doesn't
			// over-credit the player. withdraw() throws on insufficient balance.
			SharedVaultLedger.withdraw(uuid, amount);
			sender.modCreditsServer(amount);
			PacketCSVaultDeposit.VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Withdrew " + amount + " credits");
		} catch(Exception ex) {
			PacketCSVaultDeposit.VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Withdraw failed: " + ex.getMessage());
		}
	}
}
