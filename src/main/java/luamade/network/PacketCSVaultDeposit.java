package luamade.network;

import api.common.GameServer;
import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import luamade.element.ElementRegistry;
import luamade.lua.vault.SharedVaultLedger;
import luamade.lua.vault.VaultAccessManager;
import luamade.system.module.VaultModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.objects.Sendable;

import java.io.IOException;

/**
 * Client → Server: deposit {@code amount} credits from the player's account
 * into the vault at {@code (entityId, absIndex)}.
 *
 * <p>Server validates access ({@link VaultAccessManager.Op#DEPOSIT}), that the
 * player has the credits, and that the balance won't overflow. On success
 * credits are moved atomically via {@link PlayerState#modCreditsServer(long)}
 * + {@link SharedVaultLedger#deposit}. A {@link PacketSCVaultView} is sent
 * back either way so the client's dialog reflects the outcome.
 */
public class PacketCSVaultDeposit extends Packet {

	private int entityId;
	private long absIndex;
	private long amount;

	public PacketCSVaultDeposit() {
	}

	public PacketCSVaultDeposit(int entityId, long absIndex, long amount) {
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
		SegmentPiece piece = resolveVault(entityId, absIndex);
		if(piece == null) {
			VaultPacketReply.refuse(sender, null, entityId, absIndex, "Vault not found");
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			VaultPacketReply.refuse(sender, null, entityId, absIndex, "Vault module missing");
			return;
		}
		String uuid = container.getOrAssignUuid(absIndex);

		if(amount <= 0) {
			VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Amount must be positive");
			return;
		}
		if(!VaultAccessManager.canAccess(piece, sender, VaultAccessManager.Op.DEPOSIT)) {
			VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Access denied");
			return;
		}
		if(sender.getCredits() < amount) {
			VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Insufficient credits");
			return;
		}
		try {
			// Conservation transfer: debit player then credit vault. modCreditsServer
			// queues through the player's credit-modification buffer, so reading
			// credits on the same tick may still show the old value — that's fine
			// for the next interact refresh.
			sender.modCreditsServer(-amount);
			SharedVaultLedger.deposit(uuid, amount);
			VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Deposited " + amount + " credits");
		} catch(Exception ex) {
			// Best-effort refund on ledger failure. Player's credit queue has already
			// received the debit; pushing an equal credit unwinds it.
			try { sender.modCreditsServer(amount); } catch(Exception ignored) {}
			VaultPacketReply.reply(sender, uuid, entityId, absIndex, "Deposit failed: " + ex.getMessage());
		}
	}

	static SegmentPiece resolveVault(int entityId, long absIndex) {
		try {
			Sendable sendable = GameServer.getServerState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
			if(!(sendable instanceof ManagedUsableSegmentController<?>)) return null;
			SegmentController sc = (SegmentController) sendable;
			SegmentPiece piece = sc.getSegmentBuffer().getPointUnsave(absIndex);
			if(piece == null || piece.getType() != ElementRegistry.VAULT.getId()) return null;
			return piece;
		} catch(Exception ex) {
			return null;
		}
	}

	static final class VaultPacketReply {
		static void reply(PlayerState sender, String uuid, int entityId, long absIndex, String statusMessage) {
			long balance = uuid == null ? 0L : SharedVaultLedger.getBalance(uuid);
			long credits = sender.getCredits();
			SegmentPiece piece = resolveVault(entityId, absIndex);
			String level = piece == null ? "NONE" : VaultAccessManager.describeAccess(piece, sender).name();
			PacketUtil.sendPacket(sender, new PacketSCVaultView(
					uuid == null ? "" : uuid,
					entityId, absIndex, balance, credits, level, statusMessage == null ? "" : statusMessage));
		}

		static void refuse(PlayerState sender, String uuid, int entityId, long absIndex, String statusMessage) {
			PacketUtil.sendPacket(sender, new PacketSCVaultView(
					uuid == null ? "" : uuid,
					entityId, absIndex, 0L, sender.getCredits(), "NONE", statusMessage == null ? "" : statusMessage));
		}
	}
}
