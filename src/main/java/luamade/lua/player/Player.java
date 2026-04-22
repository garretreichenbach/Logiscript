package luamade.lua.player;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.luaj.vm2.LuaError;
import org.schema.game.client.controller.PlayerGameOkCancelInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.PlayerState;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lua-facing userdata for the local player. Scripts reach it via the {@code player}
 * global.
 *
 * <p>Read-only credit queries are available via {@link #getCredits()}; all mutating
 * credit operations live on the Vault API so they stay enforceable by the server.
 * Scripts should treat credit balances they observe through this class as "may be
 * stale by up to one network tick" — the authoritative value is always server-side.
 *
 * <p>The dialog methods ({@link #confirm}, {@link #message}) block the script thread
 * until the user dismisses the dialog. Scripts run on a dedicated executor
 * ({@link luamade.lua.terminal.Terminal}), so blocking is safe and avoids the
 * ergonomic cost of callback-style Lua APIs. Thread interruption (e.g. Ctrl+C in
 * the terminal) wakes the waiting script with a {@link LuaError}.
 */
public class Player extends LuaMadeUserdata {

	/** Upper bound on how long a dialog can block a script thread. */
	private static final long DIALOG_TIMEOUT_MS = 5 * 60 * 1000L;

	private static final AtomicInteger windowCounter = new AtomicInteger(0);

	@LuaMadeCallable
	public String getName() {
		PlayerState player = localPlayer();
		return player == null ? null : player.getName();
	}

	@LuaMadeCallable
	public Long getCredits() {
		PlayerState player = localPlayer();
		return player == null ? null : player.getCredits();
	}

	@LuaMadeCallable
	public Boolean isValid() {
		return localPlayer() != null;
	}

	/**
	 * Opens a native OK/Cancel dialog and blocks the calling script thread until
	 * the user dismisses it. Returns true for OK, false for Cancel, Esc, or any
	 * other deactivation (window close, player teleport, etc).
	 */
	@LuaMadeCallable
	public Boolean confirm(String title, String body) {
		return showDialog(title, body, "LOGI_CONFIRM");
	}

	/**
	 * Opens a native OK/Cancel dialog purely to display a message. Still blocks
	 * the script thread until dismissed, but the return value is always true and
	 * can be ignored. Distinct from {@link #confirm} only to make calling-site
	 * intent clear.
	 */
	@LuaMadeCallable
	public Boolean message(String title, String body) {
		showDialog(title, body, "LOGI_MESSAGE");
		return Boolean.TRUE;
	}

	private Boolean showDialog(String title, String body, String windowIdPrefix) {
		GameClientState client = GameClientState.instance;
		if(client == null) {
			throw new LuaError("No active client — cannot show dialog");
		}
		String windowId = windowIdPrefix + "_" + windowCounter.incrementAndGet();
		String safeTitle = title == null ? "" : title;
		String safeBody = body == null ? "" : body;

		CompletableFuture<Boolean> result = new CompletableFuture<>();
		DialogHolder holder = new DialogHolder();

		PlayerGameOkCancelInput dialog = new PlayerGameOkCancelInput(windowId, client, safeTitle, safeBody) {
			@Override
			public void pressedOK() {
				result.complete(Boolean.TRUE);
				deactivate();
			}

			@Override
			public void onDeactivate() {
				// Covers Cancel, Esc, window close, and any case where pressedOK already
				// completed the future — the second complete() is a no-op.
				result.complete(Boolean.FALSE);
			}
		};
		holder.dialog = dialog;
		dialog.activate();

		try {
			return result.get(DIALOG_TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			// Script canceled mid-dialog — close it so the player isn't left staring
			// at a zombie window.
			Thread.currentThread().interrupt();
			deactivateSafely(holder.dialog);
			throw new LuaError("Dialog interrupted");
		} catch(TimeoutException e) {
			deactivateSafely(holder.dialog);
			throw new LuaError("Dialog timed out after " + DIALOG_TIMEOUT_MS + "ms");
		} catch(Exception e) {
			deactivateSafely(holder.dialog);
			throw new LuaError("Dialog failed: " + e.getMessage());
		}
	}

	private static void deactivateSafely(PlayerGameOkCancelInput dialog) {
		if(dialog == null) return;
		try {
			dialog.deactivate();
		} catch(Exception ignored) {
		}
	}

	private static PlayerState localPlayer() {
		GameClientState client = GameClientState.instance;
		if(client == null) return null;
		try {
			return client.getPlayer();
		} catch(Exception e) {
			return null;
		}
	}

	/** Mutable box so the anonymous dialog subclass and the waiting thread share a ref. */
	private static final class DialogHolder {
		PlayerGameOkCancelInput dialog;
	}
}
