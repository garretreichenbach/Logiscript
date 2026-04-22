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
 * Unified client → server packet for Lua-initiated vault operations.
 *
 * <p>Three ops share a packet shape so the script side only needs one
 * request-id registry. Each op echoes back the same request id via
 * {@link PacketSCVaultScriptResponse}.
 *
 * <ul>
 *   <li>{@link Op#QUERY}  — read balance. No mutation. {@code amount} ignored.</li>
 *   <li>{@link Op#DEPOSIT} — player → vault, using standard DEPOSIT access rules
 *       (PUBLIC adjacent permits strangers; faction match otherwise).</li>
 *   <li>{@link Op#PAYOUT} — vault → sender, using standard WITHDRAW access
 *       rules (sender's faction must pass; PUBLIC does not grant payout).</li>
 * </ul>
 *
 * <p>No elevated "script authority" exists: scripts can't drain vaults their
 * invoking player couldn't drain through the interact UI. The script
 * convenience is purely ergonomic — no extra access is granted.
 */
public class PacketCSVaultScriptOp extends Packet {

	public enum Op { QUERY, DEPOSIT, PAYOUT }

	private int requestId;
	private String op;
	private int entityId;
	private long absIndex;
	private long amount;

	public PacketCSVaultScriptOp() {
	}

	public PacketCSVaultScriptOp(int requestId, Op op, int entityId, long absIndex, long amount) {
		this.requestId = requestId;
		this.op = op.name();
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.amount = amount;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		op = buffer.readString();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		amount = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeString(op);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeLong(amount);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		SegmentPiece piece = resolveVault(entityId, absIndex);
		if(piece == null) {
			reply(sender, false, "Vault not found", 0L);
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			reply(sender, false, "Vault module missing", 0L);
			return;
		}
		String uuid = container.getOrAssignUuid(absIndex);
		Op parsed;
		try {
			parsed = Op.valueOf(op);
		} catch(Exception ex) {
			reply(sender, false, "Unknown op", SharedVaultLedger.getBalance(uuid));
			return;
		}

		switch(parsed) {
			case QUERY:
				reply(sender, true, "", SharedVaultLedger.getBalance(uuid));
				return;
			case DEPOSIT:
				handleDeposit(sender, piece, uuid);
				return;
			case PAYOUT:
				handlePayout(sender, piece, uuid);
				return;
			default:
				reply(sender, false, "Unhandled op", SharedVaultLedger.getBalance(uuid));
		}
	}

	private void handleDeposit(PlayerState sender, SegmentPiece piece, String uuid) {
		if(amount <= 0) {
			reply(sender, false, "Amount must be positive", SharedVaultLedger.getBalance(uuid));
			return;
		}
		if(!VaultAccessManager.canAccess(piece, sender, VaultAccessManager.Op.DEPOSIT)) {
			reply(sender, false, "Access denied", SharedVaultLedger.getBalance(uuid));
			return;
		}
		if(sender.getCredits() < amount) {
			reply(sender, false, "Insufficient credits", SharedVaultLedger.getBalance(uuid));
			return;
		}
		try {
			sender.modCreditsServer(-amount);
			long newBal = SharedVaultLedger.deposit(uuid, amount);
			reply(sender, true, "", newBal);
		} catch(Exception ex) {
			try { sender.modCreditsServer(amount); } catch(Exception ignored) {}
			reply(sender, false, "Deposit failed: " + ex.getMessage(), SharedVaultLedger.getBalance(uuid));
		}
	}

	private void handlePayout(PlayerState sender, SegmentPiece piece, String uuid) {
		if(amount <= 0) {
			reply(sender, false, "Amount must be positive", SharedVaultLedger.getBalance(uuid));
			return;
		}
		if(!VaultAccessManager.canAccess(piece, sender, VaultAccessManager.Op.WITHDRAW)) {
			reply(sender, false, "Access denied", SharedVaultLedger.getBalance(uuid));
			return;
		}
		try {
			long newBal = SharedVaultLedger.withdraw(uuid, amount);
			sender.modCreditsServer(amount);
			reply(sender, true, "", newBal);
		} catch(Exception ex) {
			reply(sender, false, "Payout failed: " + ex.getMessage(), SharedVaultLedger.getBalance(uuid));
		}
	}

	private void reply(PlayerState sender, boolean success, String message, long balance) {
		PacketUtil.sendPacket(sender, new PacketSCVaultScriptResponse(requestId, success, message, balance));
	}

	private static SegmentPiece resolveVault(int entityId, long absIndex) {
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
}
