package luamade.network;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache of the most recent {@link PacketSCVaultView} payload per
 * vault UUID. The {@link luamade.gui.VaultDialog} reads from this cache so a
 * deposit/withdraw request can refresh the view without reopening the dialog.
 */
public final class ClientVaultCache {

	public static final class View {
		public final String uuid;
		public final int entityId;
		public final long absIndex;
		public final long balance;
		public final long playerCredits;
		public final String accessLevel;

		public View(String uuid, int entityId, long absIndex, long balance, long playerCredits, String accessLevel) {
			this.uuid = uuid;
			this.entityId = entityId;
			this.absIndex = absIndex;
			this.balance = balance;
			this.playerCredits = playerCredits;
			this.accessLevel = accessLevel;
		}
	}

	private static final ConcurrentHashMap<String, View> cache = new ConcurrentHashMap<>();

	private ClientVaultCache() {
	}

	public static void update(View view) {
		cache.put(view.uuid, view);
	}

	public static View get(String uuid) {
		return cache.get(uuid);
	}
}
