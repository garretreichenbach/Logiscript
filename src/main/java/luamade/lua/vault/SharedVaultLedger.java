package luamade.lua.vault;

import luamade.LuaMade;
import luamade.utils.DataUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-vault balance ledger persisted as JSON on disk.
 *
 * <p>Each Vault block is identified by a persistent UUID assigned by
 * {@link luamade.system.module.VaultModuleContainer}. That UUID keys the file at
 * {@code <worldData>/vaults/<uuid>.json}.
 *
 * <p>All mutations go through {@link #deposit}, {@link #withdraw}, and
 * {@link #transfer}. These are synchronized per-vault so concurrent ops never
 * race. Balances are non-negative longs; overflow and underflow throw.
 */
public final class SharedVaultLedger {

	/** vault UUID → current balance in credits (always >= 0). */
	private static final ConcurrentHashMap<String, Long> balances = new ConcurrentHashMap<>();

	/** Guards per-vault mutations + JSON writes. */
	private static final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

	private SharedVaultLedger() {
	}

	// -------------------------------------------------------------------------
	// Public API
	// -------------------------------------------------------------------------

	public static long getBalance(String vaultUuid) {
		Long b = balances.get(vaultUuid);
		if(b == null) {
			synchronized(lockFor(vaultUuid)) {
				b = balances.get(vaultUuid);
				if(b == null) {
					b = load(vaultUuid);
					balances.put(vaultUuid, b);
				}
			}
		}
		return b;
	}

	/** Adds {@code amount} to the vault. Returns the new balance. */
	public static long deposit(String vaultUuid, long amount) {
		if(amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
		synchronized(lockFor(vaultUuid)) {
			long current = getBalance(vaultUuid);
			if(amount > Long.MAX_VALUE - current) {
				throw new IllegalArgumentException("Deposit would overflow vault balance");
			}
			long next = current + amount;
			balances.put(vaultUuid, next);
			persist(vaultUuid, next);
			return next;
		}
	}

	/**
	 * Removes {@code amount} from the vault. Returns the new balance.
	 * Throws {@link IllegalArgumentException} if the vault has insufficient funds.
	 */
	public static long withdraw(String vaultUuid, long amount) {
		if(amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive");
		synchronized(lockFor(vaultUuid)) {
			long current = getBalance(vaultUuid);
			if(current < amount) {
				throw new IllegalArgumentException("Insufficient vault balance");
			}
			long next = current - amount;
			balances.put(vaultUuid, next);
			persist(vaultUuid, next);
			return next;
		}
	}

	/**
	 * Moves {@code amount} from {@code srcUuid} to {@code dstUuid}. Atomic w.r.t.
	 * both vaults: if either step would fail, neither is applied.
	 *
	 * <p>Lock order is lexicographic on UUIDs so any two transfers between the
	 * same pair of vaults acquire locks in the same order and cannot deadlock.
	 */
	public static void transfer(String srcUuid, String dstUuid, long amount) {
		if(amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive");
		if(srcUuid.equals(dstUuid)) return;
		Object firstLock;
		Object secondLock;
		if(srcUuid.compareTo(dstUuid) < 0) {
			firstLock = lockFor(srcUuid);
			secondLock = lockFor(dstUuid);
		} else {
			firstLock = lockFor(dstUuid);
			secondLock = lockFor(srcUuid);
		}
		synchronized(firstLock) {
			synchronized(secondLock) {
				long srcBal = getBalance(srcUuid);
				long dstBal = getBalance(dstUuid);
				if(srcBal < amount) {
					throw new IllegalArgumentException("Insufficient vault balance");
				}
				if(amount > Long.MAX_VALUE - dstBal) {
					throw new IllegalArgumentException("Transfer would overflow destination balance");
				}
				long newSrc = srcBal - amount;
				long newDst = dstBal + amount;
				balances.put(srcUuid, newSrc);
				balances.put(dstUuid, newDst);
				persist(srcUuid, newSrc);
				persist(dstUuid, newDst);
			}
		}
	}

	/** Writes all loaded vaults to disk. Called on server shutdown. */
	public static void saveAll() {
		for(Map.Entry<String, Long> e : balances.entrySet()) {
			persist(e.getKey(), e.getValue());
		}
	}

	/** Removes a vault's file + in-memory entry. Called when a Vault block is destroyed. */
	public static void deleteVault(String vaultUuid) {
		synchronized(lockFor(vaultUuid)) {
			balances.remove(vaultUuid);
			File file = vaultFile(vaultUuid);
			if(file != null && file.exists()) {
				file.delete();
			}
		}
		locks.remove(vaultUuid);
	}

	// -------------------------------------------------------------------------
	// Persistence
	// -------------------------------------------------------------------------

	private static long load(String vaultUuid) {
		File file = vaultFile(vaultUuid);
		if(file == null || !file.exists()) return 0L;
		try {
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			JSONObject obj = new JSONObject(json);
			return obj.optLong("balance", 0L);
		} catch(Exception ex) {
			LuaMade.getInstance().logWarning("Failed to load vault " + vaultUuid + ": " + ex.getMessage());
			return 0L;
		}
	}

	private static void persist(String vaultUuid, long balance) {
		File file = vaultFile(vaultUuid);
		if(file == null) return;
		try {
			File parent = file.getParentFile();
			if(parent != null && !parent.exists()) parent.mkdirs();
			JSONObject obj = new JSONObject();
			obj.put("balance", balance);
			Files.write(file.toPath(), obj.toString(2).getBytes(StandardCharsets.UTF_8));
		} catch(IOException ex) {
			LuaMade.getInstance().logWarning("Failed to persist vault " + vaultUuid + ": " + ex.getMessage());
		}
	}

	private static File vaultFile(String vaultUuid) {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath == null || worldDataPath.trim().isEmpty()) return null;
		File dir = new File(worldDataPath, "vaults");
		return new File(dir, vaultUuid + ".json");
	}

	private static Object lockFor(String vaultUuid) {
		return locks.computeIfAbsent(vaultUuid, id -> new Object());
	}
}
