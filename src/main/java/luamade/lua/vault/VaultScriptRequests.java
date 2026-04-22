package luamade.lua.vault;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side registry of pending vault script requests. Scripts block on a
 * {@link Response} future; the response packet completes it by request-id.
 *
 * <p>Every script call that needs a server round-trip ({@code vault.getBalance},
 * {@code vault.requestPayment}, {@code vault.payoutToPlayer}) allocates a
 * fresh id here, sends it with the request packet, and waits on
 * {@link Response}. When the response packet arrives on the client it looks up
 * the id in {@link #pending} and completes the future.
 */
public final class VaultScriptRequests {

	public static final class Response {
		public final boolean success;
		public final String message;
		public final long balance;

		public Response(boolean success, String message, long balance) {
			this.success = success;
			this.message = message == null ? "" : message;
			this.balance = balance;
		}
	}

	private static final AtomicInteger nextId = new AtomicInteger(1);
	private static final ConcurrentHashMap<Integer, java.util.concurrent.CompletableFuture<Response>> pending = new ConcurrentHashMap<>();

	private VaultScriptRequests() {
	}

	public static int allocate(java.util.concurrent.CompletableFuture<Response> future) {
		int id = nextId.getAndIncrement();
		pending.put(id, future);
		return id;
	}

	/** Called on the client when a response packet arrives. */
	public static void complete(int requestId, Response response) {
		java.util.concurrent.CompletableFuture<Response> f = pending.remove(requestId);
		if(f != null) f.complete(response);
	}

	/** Called if the script is interrupted or we're otherwise giving up on this id. */
	public static void cancel(int requestId) {
		pending.remove(requestId);
	}
}
