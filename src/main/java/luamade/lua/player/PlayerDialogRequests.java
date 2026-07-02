package luamade.lua.player;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-side registry of pending player dialog requests. Mirrors
 * {@link luamade.lua.vault.VaultScriptRequests}, but lives on the server
 * (unlike Vault's client-side registry) since {@code player.confirm()} /
 * {@code player.message()} now originate from a server-hosted script and
 * must round-trip to whichever client is meant to see the dialog.
 */
public final class PlayerDialogRequests {

	private static final AtomicInteger nextId = new AtomicInteger(1);
	private static final ConcurrentHashMap<Integer, CompletableFuture<Boolean>> pending = new ConcurrentHashMap<>();

	private PlayerDialogRequests() {
	}

	public static int allocate(CompletableFuture<Boolean> future) {
		int id = nextId.getAndIncrement();
		pending.put(id, future);
		return id;
	}

	/** Called on the server when the client's response packet arrives. */
	public static void complete(int requestId, boolean result) {
		CompletableFuture<Boolean> future = pending.remove(requestId);
		if(future != null) {
			future.complete(result);
		}
	}

	/** Called if the script is interrupted or the request is otherwise abandoned. */
	public static void cancel(int requestId) {
		pending.remove(requestId);
	}
}
