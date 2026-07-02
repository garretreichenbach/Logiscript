package luamade.lua.player;

import api.network.packets.PacketUtil;
import luamade.lua.terminal.ScriptInvoker;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.network.PacketSCPlayerDialogRequest;
import org.luaj.vm2.LuaError;
import org.schema.game.common.data.player.PlayerState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Lua-facing userdata for "whichever player caused this script to run".
 * Scripts reach it via the {@code player} global.
 *
 * <p>Scripts now execute server-side, so there is no single "local player" —
 * {@link ScriptInvoker} tracks which connected {@link PlayerState} sent the
 * input that started the current script (set by {@link luamade.lua.terminal.Terminal}).
 * A script invoked with no associated player (e.g. the startup script running
 * with nobody connected) sees every method here return {@code null}/throw,
 * since there's nobody to query or show a dialog to.
 *
 * <p>Credit queries ({@link #getCredits()}) read the server-authoritative
 * {@code PlayerState} directly — no staleness caveat needed now that this
 * runs on the server. All mutating credit operations still live on the Vault
 * API so they stay enforceable independent of this class.
 *
 * <p>The dialog methods ({@link #confirm}, {@link #message}) block the script
 * thread until the target player's client dismisses the dialog, round-tripping
 * over {@link PacketSCPlayerDialogRequest} / {@code PacketCSPlayerDialogResponse}.
 * Scripts run on a dedicated executor ({@link luamade.lua.terminal.Terminal}),
 * so blocking is safe. Thread interruption (e.g. Ctrl+C in the terminal) wakes
 * the waiting script with a {@link LuaError}.
 */
public class Player extends LuaMadeUserdata {

	/** Upper bound on how long a dialog can block a script thread. */
	private static final long DIALOG_TIMEOUT_MS = 5 * 60 * 1000L;

	@LuaMadeCallable
	public String getName() {
		PlayerState player = ScriptInvoker.get();
		return player == null ? null : player.getName();
	}

	@LuaMadeCallable
	public Long getCredits() {
		PlayerState player = ScriptInvoker.get();
		return player == null ? null : player.getCredits();
	}

	@LuaMadeCallable
	public Boolean isValid() {
		return ScriptInvoker.get() != null;
	}

	/**
	 * Opens a native OK/Cancel dialog on the invoking player's client and
	 * blocks the calling script thread until they dismiss it. Returns true for
	 * OK, false for Cancel, Esc, or any other deactivation (window close,
	 * player teleport, etc).
	 */
	@LuaMadeCallable
	public Boolean confirm(String title, String body) {
		return showDialog(title, body);
	}

	/**
	 * Opens a native OK/Cancel dialog purely to display a message. Still blocks
	 * the script thread until dismissed, but the return value is always true and
	 * can be ignored. Distinct from {@link #confirm} only to make calling-site
	 * intent clear.
	 */
	@LuaMadeCallable
	public Boolean message(String title, String body) {
		showDialog(title, body);
		return Boolean.TRUE;
	}

	private Boolean showDialog(String title, String body) {
		PlayerState target = ScriptInvoker.get();
		if(target == null) {
			throw new LuaError("No player associated with this script invocation — cannot show dialog");
		}

		String safeTitle = title == null ? "" : title;
		String safeBody = body == null ? "" : body;

		CompletableFuture<Boolean> result = new CompletableFuture<>();
		int requestId = PlayerDialogRequests.allocate(result);
		try {
			PacketUtil.sendPacket(target, new PacketSCPlayerDialogRequest(requestId, safeTitle, safeBody));
		} catch(Exception ex) {
			PlayerDialogRequests.cancel(requestId);
			throw new LuaError("Failed to send dialog request: " + ex.getMessage());
		}

		try {
			return result.get(DIALOG_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			PlayerDialogRequests.cancel(requestId);
			throw new LuaError("Dialog interrupted");
		} catch(TimeoutException e) {
			PlayerDialogRequests.cancel(requestId);
			throw new LuaError("Dialog timed out after " + DIALOG_TIMEOUT_MS + "ms");
		} catch(Exception e) {
			PlayerDialogRequests.cancel(requestId);
			throw new LuaError("Dialog failed: " + e.getMessage());
		}
	}
}
