package luamade.lua.vault;

import api.network.packets.PacketUtil;
import luamade.lua.player.Player;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.network.PacketCSVaultScriptOp;
import luamade.system.module.ComputerModule;
import luamade.system.module.VaultModuleContainer;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lua-facing userdata exposed to scripts as the {@code vault} global.
 *
 * <h2>Scope</h2>
 * Scripts can only operate on vaults located on the <em>same entity</em> as the
 * computer executing the script. This is a deliberate simplification: it keeps
 * UUID→block lookup local and avoids a cross-entity discovery protocol. Cross-
 * entity vault interop may be added later via a network-module analogue to the
 * networked data store.
 *
 * <h2>Authorization</h2>
 * Every mutation goes through {@link PacketCSVaultScriptOp}, which re-runs the
 * same {@link VaultAccessManager} rules a player would hit through the interact
 * UI. <em>Scripts grant no elevated authority</em>: if a player couldn't
 * withdraw through the Vault dialog, their script can't either. The convenience
 * is purely ergonomic.
 *
 * <h2>Blocking</h2>
 * Every call here does a server round-trip and blocks the script thread on a
 * {@link CompletableFuture}. Scripts run on a dedicated per-computer executor,
 * so blocking is safe.
 */
public class Vault extends LuaMadeUserdata {

	/** Server responses should arrive in well under a second on a LAN; ten is generous. */
	private static final long SERVER_TIMEOUT_MS = 10_000L;

	private final ComputerModule module;
	private final Player dialogs;

	public Vault(ComputerModule module) {
		this.module = module;
		this.dialogs = new Player();
	}

	/**
	 * Returns the balance of the vault identified by {@code uuid}, in credits.
	 * Throws a Lua error if the vault is not on this computer's entity.
	 */
	@LuaMadeCallable
	public Long getBalance(String uuid) {
		long abs = resolveAbsIndex(uuid);
		VaultScriptRequests.Response resp = blockingOp(PacketCSVaultScriptOp.Op.QUERY, abs, 0L);
		if(!resp.success) throw new LuaError(resp.message);
		return resp.balance;
	}

	/**
	 * Requests a payment from the local player into the vault. Shows the native
	 * OK/Cancel confirm dialog first, then (on OK) performs a server-authoritative
	 * deposit. Returns {@code true} iff the player accepted <em>and</em> the
	 * server accepted the deposit (player has funds, DEPOSIT access allowed, etc.).
	 *
	 * <p>Reason is shown in the confirm dialog body for the player.
	 */
	@LuaMadeCallable
	public Boolean requestPayment(String uuid, Long amount, String reason) {
		if(amount == null || amount <= 0) throw new LuaError("Amount must be positive");
		long abs = resolveAbsIndex(uuid);
		String body = (reason == null || reason.isEmpty() ? "" : reason + "\n\n")
				+ "Pay " + amount + " credits to this vault?";
		Boolean consent = dialogs.confirm("Vault Payment", body);
		if(consent == null || !consent) return false;
		VaultScriptRequests.Response resp = blockingOp(PacketCSVaultScriptOp.Op.DEPOSIT, abs, amount);
		if(!resp.success) throw new LuaError(resp.message);
		return true;
	}

	/**
	 * Pays {@code amount} credits from the vault to the local player. The server
	 * re-validates that this player would be allowed to withdraw through the
	 * interact UI — scripts do not grant elevated access. A typical use is a
	 * faction-bank script or a same-faction reward dispenser.
	 *
	 * <p>Throws on any failure (insufficient balance, access denied, etc.) so
	 * callers can attribute the error; returns {@code true} on success.
	 */
	@LuaMadeCallable
	public Boolean payoutToPlayer(String uuid, Long amount, String reason) {
		if(amount == null || amount <= 0) throw new LuaError("Amount must be positive");
		// reason is not yet forwarded; the param exists so we can surface it in a
		// server-side audit log later without another API change.
		long abs = resolveAbsIndex(uuid);
		VaultScriptRequests.Response resp = blockingOp(PacketCSVaultScriptOp.Op.PAYOUT, abs, amount);
		if(!resp.success) throw new LuaError(resp.message);
		return true;
	}

	/**
	 * Returns the UUIDs of every Vault block on this computer's entity. Does not
	 * check access — filtering is the script's responsibility.
	 */
	@LuaMadeCallable
	public String[] list() {
		VaultModuleContainer container = containerOrThrow();
		return container.listUuids();
	}

	// ---- internals ---------------------------------------------------------

	private long resolveAbsIndex(String uuid) {
		if(uuid == null || uuid.isEmpty()) throw new LuaError("Vault UUID must not be empty");
		VaultModuleContainer container = containerOrThrow();
		long abs = container.getAbsIndexByUuid(uuid);
		if(abs == Long.MIN_VALUE) {
			throw new LuaError("Vault not found on this entity: " + uuid);
		}
		return abs;
	}

	private VaultModuleContainer containerOrThrow() {
		SegmentPiece computer = requireLiveComputerPiece();
		if(!(computer.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("This entity does not support vault storage");
		}
		ManagedUsableSegmentController<?> sc = (ManagedUsableSegmentController<?>) computer.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(sc.getManagerContainer());
		if(container == null) throw new LuaError("Vault module not initialized on this entity");
		return container;
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

	private VaultScriptRequests.Response blockingOp(PacketCSVaultScriptOp.Op op, long absIndex, long amount) {
		int entityId = requireLiveComputerPiece().getSegmentController().getId();
		CompletableFuture<VaultScriptRequests.Response> future = new CompletableFuture<>();
		int requestId = VaultScriptRequests.allocate(future);
		try {
			PacketUtil.sendPacketToServer(new PacketCSVaultScriptOp(requestId, op, entityId, absIndex, amount));
		} catch(Exception ex) {
			VaultScriptRequests.cancel(requestId);
			throw new LuaError("Failed to send vault request: " + ex.getMessage());
		}
		try {
			return future.get(SERVER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			VaultScriptRequests.cancel(requestId);
			Thread.currentThread().interrupt();
			throw new LuaError("Vault op interrupted");
		} catch(TimeoutException e) {
			VaultScriptRequests.cancel(requestId);
			throw new LuaError("Vault op timed out after " + SERVER_TIMEOUT_MS + "ms");
		} catch(ExecutionException e) {
			VaultScriptRequests.cancel(requestId);
			throw new LuaError("Vault op failed: " + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage()));
		}
	}
}
