package luamade.lua.vault;

import luamade.element.ElementRegistry;
import luamade.lua.player.Player;
import luamade.lua.terminal.ScriptInvoker;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import luamade.system.module.VaultModuleContainer;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

/**
 * Lua-facing userdata exposed to scripts as the {@code vault} global. Vaults are addressed by UUID and must be on the
 * same entity as the computer; {@link luamade.lua.element.block.VaultBlock} wraps a single vault block directly.
 *
 * <h2>Authorization</h2>
 * Scripts run server-side, so these ops call the ledger directly, re-running the same {@link VaultAccessManager} rules
 * the interact UI uses against the player who started the script ({@link ScriptInvoker}). <em>Scripts grant no
 * elevated authority</em>: if that player couldn't withdraw through the Vault dialog, their script can't either.
 * Scripts with no invoking player (e.g. startup scripts) can read balances but not move credits.
 */
public class Vault extends LuaMadeUserdata {

	private final ComputerModule module;

	public Vault(ComputerModule module) {
		this.module = module;
	}

	@LuaMadeCallable
	public Long getBalance(String uuid) {
		return balanceOf(resolve(uuid));
	}

	@LuaMadeCallable
	public Boolean requestPayment(String uuid, Long amount, String reason) {
		return requestPayment(resolve(uuid), amount, reason);
	}

	@LuaMadeCallable
	public Boolean payoutToPlayer(String uuid, Long amount, String reason) {
		return payout(resolve(uuid), amount);
	}

	/** Returns the UUIDs of every Vault block on this computer's entity. Does not check access. */
	@LuaMadeCallable
	public String[] list() {
		return containerOf(requireLiveComputerPiece()).listUuids();
	}

	// ---- shared with VaultBlock -------------------------------------------

	public static String uuidOf(SegmentPiece vault) {
		return containerOf(vault).getOrAssignUuid(vault.getAbsoluteIndex());
	}

	public static long balanceOf(SegmentPiece vault) {
		return SharedVaultLedger.getBalance(uuidOf(vault));
	}

	/**
	 * Shows the invoking player an OK/Cancel dialog, then on OK moves {@code amount} credits from them into the vault.
	 * Returns false if they cancel; throws on any server-side refusal so callers can attribute the error.
	 */
	public static boolean requestPayment(SegmentPiece vault, Long amount, String reason) {
		requirePositive(amount);
		PlayerState player = requireInvoker();
		String body = (reason == null || reason.isEmpty() ? "" : reason + "\n\n") + "Pay " + amount + " credits to this vault?";
		Boolean consent = new Player().confirm("Vault Payment", body);
		if(consent == null || !consent) {
			return false;
		}
		if(!VaultAccessManager.canAccess(vault, player, VaultAccessManager.Op.DEPOSIT)) {
			throw new LuaError("Access denied");
		}
		String uuid = uuidOf(vault);
		synchronized(player) {
			if(player.getCredits() < amount) {
				throw new LuaError("Insufficient credits");
			}
			player.modCreditsServer(-amount);
			try {
				SharedVaultLedger.deposit(uuid, amount);
			} catch(Exception exception) {
				player.modCreditsServer(amount);
				throw new LuaError("Deposit failed: " + exception.getMessage());
			}
		}
		return true;
	}

	/** Pays {@code amount} credits from the vault to the invoking player, if they could withdraw through the UI. */
	public static boolean payout(SegmentPiece vault, Long amount) {
		requirePositive(amount);
		PlayerState player = requireInvoker();
		if(!VaultAccessManager.canAccess(vault, player, VaultAccessManager.Op.WITHDRAW)) {
			throw new LuaError("Access denied");
		}
		try {
			SharedVaultLedger.withdraw(uuidOf(vault), amount);
		} catch(Exception exception) {
			throw new LuaError("Payout failed: " + exception.getMessage());
		}
		player.modCreditsServer(amount);
		return true;
	}

	public static String accessLevelOf(SegmentPiece vault) {
		PlayerState player = ScriptInvoker.get();
		return (player == null ? VaultAccessManager.AccessLevel.NONE : VaultAccessManager.describeAccess(vault, player)).name();
	}

	// ---- internals ---------------------------------------------------------

	private SegmentPiece resolve(String uuid) {
		if(uuid == null || uuid.isEmpty()) throw new LuaError("Vault UUID must not be empty");
		SegmentPiece computer = requireLiveComputerPiece();
		long abs = containerOf(computer).getAbsIndexByUuid(uuid);
		SegmentPiece vault = abs == Long.MIN_VALUE ? null : computer.getSegmentController().getSegmentBuffer().getPointUnsave(abs);
		if(vault == null || vault.getType() != ElementRegistry.VAULT.getId()) {
			throw new LuaError("Vault not found on this entity: " + uuid);
		}
		return vault;
	}

	private static VaultModuleContainer containerOf(SegmentPiece piece) {
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?> sc)) {
			throw new LuaError("This entity does not support vault storage");
		}
		VaultModuleContainer container = VaultModuleContainer.getContainer(sc.getManagerContainer());
		if(container == null) throw new LuaError("Vault module not initialized on this entity");
		return container;
	}

	private static void requirePositive(Long amount) {
		if(amount == null || amount <= 0) throw new LuaError("Amount must be positive");
	}

	private static PlayerState requireInvoker() {
		PlayerState player = ScriptInvoker.get();
		if(player == null) throw new LuaError("No player associated with this script invocation — cannot move credits");
		return player;
	}

	private SegmentPiece requireLiveComputerPiece() {
		if(module == null || module.getSegmentPiece() == null) {
			throw new LuaError("Computer block reference is not available");
		}
		SegmentPiece modulePiece = module.getSegmentPiece();
		if(modulePiece.getSegmentController() == null || modulePiece.getSegmentController().getSegmentBuffer() == null) {
			throw new LuaError("Computer block reference is no longer valid");
		}
		SegmentPiece livePiece = modulePiece.getSegmentController().getSegmentBuffer().getPointUnsave(modulePiece.getAbsoluteIndex());
		if(livePiece == null) throw new LuaError("Computer block no longer exists");
		if(livePiece.getType() != modulePiece.getType()) throw new LuaError("Computer block type changed since initialization");
		return livePiece;
	}
}
