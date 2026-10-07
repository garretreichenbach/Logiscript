package luamade.utils;

import api.util.StarRunnable;
import luamade.LuaMade;
import org.luaj.vm2.LuaError;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Runs a task on the server's main game loop and blocks the calling script
 * thread until it finishes. Use for anything that mutates game state the main
 * loop also touches (trade manager queues, galaxy prices, credits).
 */
public final class ServerThread {

	private static final long TIMEOUT_MS = 10_000L;

	private ServerThread() {
	}

	public static <T> T call(Supplier<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		new StarRunnable() {
			@Override
			public void run() {
				try {
					future.complete(task.get());
				} catch(Throwable t) {
					future.completeExceptionally(t);
				}
			}
		}.runLater(LuaMade.getInstance(), 0);
		try {
			return future.get(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LuaError("interrupted");
		} catch(ExecutionException e) {
			throw new LuaError(String.valueOf(e.getCause()));
		} catch(TimeoutException e) {
			throw new LuaError("server did not respond in time");
		}
	}
}
