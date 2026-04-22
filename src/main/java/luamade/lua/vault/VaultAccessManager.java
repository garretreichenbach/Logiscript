package luamade.lua.vault;

import luamade.element.ElementRegistry;
import luamade.manager.PasswordAuthManager;
import luamade.utils.SegmentPieceUtils;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.player.PlayerState;

import java.util.ArrayList;

/**
 * Access rules for Vault interact ops, evaluated against the permission modules
 * placed physically adjacent to the vault block.
 *
 * <h2>Semantics</h2>
 * <ul>
 *   <li><b>Adjacent {@code PUBLIC_PERMISSION_MODULE} (346)</b> — deposit allowed
 *       for anyone. <em>Withdraw is not granted by PUBLIC alone</em>, since that
 *       would let any passerby drain the vault.</li>
 *   <li><b>Adjacent {@code FACTION_PERMISSION_MODULE} (936)</b> — deposit +
 *       withdraw allowed for same-faction players.</li>
 *   <li><b>Adjacent {@code PASSWORD_PERMISSION_MODULE}</b> — deposit + withdraw
 *       allowed for players whose faction has authenticated on that module via
 *       {@link PasswordAuthManager} (scripts auth a faction; the auth persists
 *       for the default TTL).</li>
 *   <li><b>No permission modules</b> — deposit + withdraw allowed only for
 *       players in the same faction as the vault's entity. Unowned
 *       (faction 0) vaults with no modules are inaccessible.</li>
 * </ul>
 *
 * <p>Rules combine additively: if any applicable rule grants access for an op,
 * access is granted. PUBLIC-for-deposit + FACTION-for-withdraw is a supported
 * configuration (any depositor, faction-only withdrawer).
 */
public final class VaultAccessManager {

	public enum Op { DEPOSIT, WITHDRAW }

	public enum AccessLevel { PUBLIC_DEPOSIT, FACTION, PASSWORD, OWNER, NONE }

	private VaultAccessManager() {
	}

	public static boolean canAccess(SegmentPiece vaultPiece, PlayerState actor, Op op) {
		if(vaultPiece == null || actor == null) return false;
		int vaultFaction = vaultPiece.getSegmentController().getFactionId();
		int actorFaction = actor.getFactionId();

		// PUBLIC only counts as a deposit-grant.
		if(op == Op.DEPOSIT
				&& !SegmentPieceUtils.getMatchingAdjacent(vaultPiece, ElementKeyMap.FACTION_PUBLIC_EXCEPTION_ID).isEmpty()) {
			return true;
		}

		// FACTION: same-faction, both sides must be in a real faction.
		if(!SegmentPieceUtils.getMatchingAdjacent(vaultPiece, ElementKeyMap.FACTION_FACTION_EXCEPTION_ID).isEmpty()) {
			if(vaultFaction != 0 && vaultFaction == actorFaction) return true;
		}

		// PASSWORD: actor's faction has authed on any adjacent password module.
		ArrayList<SegmentPiece> pwdModules = SegmentPieceUtils.getMatchingAdjacent(
				vaultPiece, ElementRegistry.PASSWORD_PERMISSION_MODULE.getId());
		for(SegmentPiece pwd : pwdModules) {
			if(PasswordAuthManager.isAuthed(actorFaction, pwd.getSegmentController(), pwd.getAbsoluteIndex())) {
				return true;
			}
		}

		// DEFAULT (no perm modules): owner-faction match.
		boolean anyPermModule = !pwdModules.isEmpty()
				|| !SegmentPieceUtils.getMatchingAdjacent(vaultPiece, ElementKeyMap.FACTION_PUBLIC_EXCEPTION_ID).isEmpty()
				|| !SegmentPieceUtils.getMatchingAdjacent(vaultPiece, ElementKeyMap.FACTION_FACTION_EXCEPTION_ID).isEmpty();
		if(!anyPermModule && vaultFaction != 0 && vaultFaction == actorFaction) {
			return true;
		}

		return false;
	}

	/**
	 * Describes which rule grants the strongest access the actor has, primarily
	 * for display purposes in the interact UI.
	 */
	public static AccessLevel describeAccess(SegmentPiece vaultPiece, PlayerState actor) {
		if(canAccess(vaultPiece, actor, Op.WITHDRAW)) {
			// Distinguish by rule: FACTION vs PASSWORD vs OWNER.
			int vaultFaction = vaultPiece.getSegmentController().getFactionId();
			int actorFaction = actor.getFactionId();
			if(!SegmentPieceUtils.getMatchingAdjacent(vaultPiece, ElementKeyMap.FACTION_FACTION_EXCEPTION_ID).isEmpty()
					&& vaultFaction != 0 && vaultFaction == actorFaction) {
				return AccessLevel.FACTION;
			}
			ArrayList<SegmentPiece> pwdModules = SegmentPieceUtils.getMatchingAdjacent(
					vaultPiece, ElementRegistry.PASSWORD_PERMISSION_MODULE.getId());
			for(SegmentPiece pwd : pwdModules) {
				if(PasswordAuthManager.isAuthed(actorFaction, pwd.getSegmentController(), pwd.getAbsoluteIndex())) {
					return AccessLevel.PASSWORD;
				}
			}
			return AccessLevel.OWNER;
		}
		if(canAccess(vaultPiece, actor, Op.DEPOSIT)) return AccessLevel.PUBLIC_DEPOSIT;
		return AccessLevel.NONE;
	}
}
