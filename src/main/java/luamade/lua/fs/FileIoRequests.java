package luamade.lua.fs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side registry of pending file read/write requests against a
 * computer's server-side {@link FileSystem}. Mirrors
 * {@link luamade.lua.vault.VaultScriptRequests} — a caller allocates a future,
 * sends the request packet, and blocks on the future; the response packet
 * completes it by request id.
 */
public final class FileIoRequests {

	public static final class ReadResult {
		public final boolean success;
		public final String message;
		/** File contents for a read request; null for a write result (which has no content). */
		public final String content;

		public ReadResult(boolean success, String message, String content) {
			this.success = success;
			this.message = message == null ? "" : message;
			this.content = content;
		}
	}

	private static final AtomicInteger nextId = new AtomicInteger(1);
	private static final ConcurrentHashMap<Integer, CompletableFuture<ReadResult>> pending = new ConcurrentHashMap<>();

	private FileIoRequests() {
	}

	public static int allocate(CompletableFuture<ReadResult> future) {
		int id = nextId.getAndIncrement();
		pending.put(id, future);
		return id;
	}

	/** Called on the client when a response packet arrives. */
	public static void complete(int requestId, ReadResult result) {
		CompletableFuture<ReadResult> future = pending.remove(requestId);
		if(future != null) {
			future.complete(result);
		}
	}

	/** Called if the request is abandoned (e.g. dialog closed before a reply arrived). */
	public static void cancel(int requestId) {
		pending.remove(requestId);
	}
}
