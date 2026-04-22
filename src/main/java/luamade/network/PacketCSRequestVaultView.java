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
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.network.objects.Sendable;

import java.io.IOException;

/**
 * Client → Server: the player interacted with a Vault block. The server
 * resolves the vault, runs access checks, and replies with the current view
 * via {@link PacketSCVaultView}.
 *
 * <p>If the player has no access at all the server still replies (with
 * {@code accessLevel="none"} and {@code balance=0}) so the client can show a
 * "no permission" message rather than silently failing.
 */
public class PacketCSRequestVaultView extends Packet {

	private int entityId;
	private long absIndex;

	/** Required no-arg constructor for packet instantiation. */
	public PacketCSRequestVaultView() {
	}

	public PacketCSRequestVaultView(int entityId, long absIndex) {
		this.entityId = entityId;
		this.absIndex = absIndex;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		SegmentPiece piece = VaultPacketSupport.resolveVault(entityId, absIndex);
		if(piece == null) {
			// Silently ignore — client is out of sync or block was just destroyed.
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) return;
		String uuid = container.getOrAssignUuid(absIndex);

		long balance = SharedVaultLedger.getBalance(uuid);
		long playerCredits = sender.getCredits();
		VaultAccessManager.AccessLevel level = VaultAccessManager.describeAccess(piece, sender);

		PacketUtil.sendPacket(sender, new PacketSCVaultView(uuid, entityId, absIndex, balance, playerCredits, level.name()));
	}

	/** Resolves {@code (entityId, absIndex)} on the server to the Vault SegmentPiece, or {@code null}. */
	static final class VaultPacketSupport {
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

		/** Overload used when only a Vector3i-style absolute lookup is available. */
		@SuppressWarnings("unused")
		static SegmentPiece resolveVault(int entityId, Vector3i absPos) {
			try {
				Sendable sendable = GameServer.getServerState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
				if(!(sendable instanceof ManagedUsableSegmentController<?>)) return null;
				SegmentController sc = (SegmentController) sendable;
				SegmentPiece piece = sc.getSegmentBuffer().getPointUnsave(absPos);
				if(piece == null || piece.getType() != ElementRegistry.VAULT.getId()) return null;
				return piece;
			} catch(Exception ex) {
				return null;
			}
		}
	}
}
