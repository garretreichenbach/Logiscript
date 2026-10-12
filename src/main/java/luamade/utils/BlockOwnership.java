package luamade.utils;

import luamade.LuaMade;
import org.schema.game.common.controller.SegmentController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Records which block owns each persistent block ID (computers, vaults, data stores, network modules). Blueprints save
 * those IDs, so a spawned copy arrives with its original's; a different recorded owner is how a copy is recognised.
 * Owners are {@code <entity UID>@<block index>}, one small file per ID under {@code <world data>/owners}.
 */
public final class BlockOwnership {

	private BlockOwnership() {
	}

	/**
	 * The ID the block at {@code abs} should use: {@code uuid} when this block owns it or nobody does yet (it's claimed
	 * then), or a fresh, claimed ID when another block owns it. Off the server it's always {@code uuid}.
	 */
	public static String claim(SegmentController entity, String kind, String uuid, long abs) {
		if(entity == null || !entity.isOnServer() || uuid == null || uuid.isEmpty()) {
			return uuid;
		}
		String owner = entity.getUniqueIdentifier() + "@" + abs;
		String previous = claimOwner(kind, uuid, owner);
		if(previous == null || previous.equals(owner)) {
			return uuid;
		}
		String copy = UUID.randomUUID().toString();
		claimOwner(kind, copy, owner);
		LuaMade.getInstance().logInfo("The " + kind + " at " + owner + " is a copy of " + uuid + " (owned by " + previous + "); gave it " + copy);
		return copy;
	}

	/** Records {@code owner} unless an owner is already recorded; returns the one recorded before, or null. */
	private static String claimOwner(String kind, String uuid, String owner) {
		String worldDataPath = DataUtils.getWorldDataPath();
		if(worldDataPath == null || worldDataPath.isEmpty()) {
			return null;
		}
		File file = new File(new File(worldDataPath, "owners"), kind + "_" + uuid);
		try {
			if(file.isFile()) {
				return Files.readString(file.toPath(), StandardCharsets.UTF_8).trim();
			}
			file.getParentFile().mkdirs();
			Files.writeString(file.toPath(), owner, StandardCharsets.UTF_8);
		} catch(IOException exception) {
			LuaMade.getInstance().logException("Failed to record owner of " + kind + " " + uuid, exception);
		}
		return null;
	}
}
