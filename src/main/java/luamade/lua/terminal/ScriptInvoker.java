package luamade.lua.terminal;

import org.schema.game.common.data.player.PlayerState;

/**
 * Thread-bound "who caused the currently-running script to execute" context.
 *
 * <p>Scripts now run server-side, so APIs that used to assume "the local
 * player" (e.g. {@link luamade.lua.player.Player}'s dialog calls) need to know
 * which connected player to target. {@link Terminal} sets this once per
 * top-level script task (see {@code submitScriptTask}) from whichever
 * {@code PlayerState} sent the input that started it; nested calls (e.g. a
 * script invoking {@code term.runCommand(...)}) run on the same thread and
 * inherit the same value automatically.
 *
 * <p>May be {@code null} — e.g. the startup script boots with no interacting
 * player. Callers must handle that case gracefully rather than assuming a
 * player is always available.
 */
public final class ScriptInvoker {

	private static final ThreadLocal<PlayerState> CURRENT = new ThreadLocal<>();

	private ScriptInvoker() {
	}

	public static void set(PlayerState player) {
		CURRENT.set(player);
	}

	public static void clear() {
		CURRENT.remove();
	}

	public static PlayerState get() {
		return CURRENT.get();
	}
}
