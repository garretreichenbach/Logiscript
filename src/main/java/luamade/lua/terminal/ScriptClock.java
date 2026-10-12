package luamade.lua.terminal;

/**
 * A script's running time for the {@code script_max_wall_clock_ms} limit. Time spent blocked waiting (sleep, input,
 * dialogs, read prompts) doesn't count: the limit is there to stop runaway CPU use, and a waiting script uses none,
 * so a controller-mode loop that idles in {@code input.waitFor} can run as long as it's wanted.
 *
 * <p>Blocking calls wrap the wait in {@code try(ScriptClock.Blocked ignored = ScriptClock.blocked()) { ... }}; it's a
 * no-op off script threads, and nested waits count once.
 */
public final class ScriptClock {

	private static final ThreadLocal<ScriptClock> CURRENT = new ThreadLocal<>();
	private static final Blocked NOT_A_SCRIPT = () -> {
	};

	private final long startedAt = System.currentTimeMillis();
	private long blockedMs;
	private long blockedSince;
	private int depth;

	/** Ends a blocked stretch; {@link AutoCloseable#close()} without the checked exception. */
	public interface Blocked extends AutoCloseable {
		@Override
		void close();
	}

	static void bind(ScriptClock clock) {
		CURRENT.set(clock);
	}

	static void unbind() {
		CURRENT.remove();
	}

	public static Blocked blocked() {
		ScriptClock clock = CURRENT.get();
		if(clock == null) {
			return NOT_A_SCRIPT;
		}
		clock.begin();
		return clock::end;
	}

	private synchronized void begin() {
		if(depth++ == 0) {
			blockedSince = System.currentTimeMillis();
		}
	}

	private synchronized void end() {
		if(--depth == 0) {
			blockedMs += System.currentTimeMillis() - blockedSince;
		}
	}

	/** Milliseconds the script has spent running, not waiting. */
	synchronized long activeMs(long now) {
		long blocked = blockedMs + (depth > 0 ? now - blockedSince : 0);
		return now - startedAt - blocked;
	}
}
