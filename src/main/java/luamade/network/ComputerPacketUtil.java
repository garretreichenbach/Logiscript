package luamade.network;

import api.common.GameServer;
import com.bulletphysics.linearmath.Transform;
import luamade.element.ElementRegistry;
import luamade.system.module.ComputerModuleContainer;
import org.schema.common.util.linAlg.Vector3fTools;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.schine.network.objects.Sendable;

import javax.vecmath.Vector3f;

/**
 * Shared server-side resolution helpers for the computer-session packets
 * (input forwarding, file I/O). Mirrors the resolve-then-validate pattern
 * already used by {@link PacketCSVaultScriptOp}.
 */
final class ComputerPacketUtil {

	/**
	 * Max distance (in blocks) a player may be from a computer to open a
	 * session against it. Direct block interaction used to be gated by the
	 * client's own raycast+reach system before {@code onInteract} ever fired;
	 * a raw network packet has no such gate for free, so this is the
	 * server-side replacement. Generous enough to cover interacting from
	 * anywhere aboard a mid-size ship, not just standing on the block.
	 */
	static final float MAX_CONNECT_DISTANCE_BLOCKS = 50f;

	private ComputerPacketUtil() {
	}

	/** True if {@code sender} is currently within {@link #MAX_CONNECT_DISTANCE_BLOCKS} of {@code piece}'s world position. */
	static boolean isWithinConnectRange(PlayerState sender, SegmentPiece piece) {
		if(sender == null || piece == null) {
			return false;
		}
		try {
			SimpleTransformableSendableObject controlled = sender.getFirstControlledTransformableWOExc();
			if(controlled == null) {
				return false;
			}
			Vector3f playerPos = controlled.getWorldTransform().origin;

			Transform blockTransform = new Transform();
			piece.getTransform(blockTransform);
			Vector3f blockPos = blockTransform.origin;

			float distance = Vector3fTools.distance(playerPos.x, playerPos.y, playerPos.z, blockPos.x, blockPos.y, blockPos.z);
			return distance <= MAX_CONNECT_DISTANCE_BLOCKS;
		} catch(Exception ex) {
			return false;
		}
	}

	/** Resolves a live Computer block by entity + absolute index, or null if it no longer exists / isn't a Computer. */
	static SegmentPiece resolveComputerPiece(int entityId, long absIndex) {
		try {
			Sendable sendable = GameServer.getServerState().getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
			if(!(sendable instanceof ManagedUsableSegmentController<?>)) {
				return null;
			}
			SegmentController sc = (SegmentController) sendable;
			SegmentPiece piece = sc.getSegmentBuffer().getPointUnsave(absIndex);
			if(piece == null || piece.getType() != ElementRegistry.COMPUTER.getId()) {
				return null;
			}
			return piece;
		} catch(Exception ex) {
			return null;
		}
	}

	/** Resolves the {@link ComputerModuleContainer} that owns the given live Computer piece, or null. */
	static ComputerModuleContainer resolveContainer(SegmentPiece computerPiece) {
		if(computerPiece == null || !(computerPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return null;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) computerPiece.getSegmentController();
		return ComputerModuleContainer.getContainer(controller.getManagerContainer());
	}
}
