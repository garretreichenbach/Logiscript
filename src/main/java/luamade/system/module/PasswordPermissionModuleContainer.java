package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

/**
 * Per-entity module that stores the password hash for each
 * {@link luamade.element.block.PasswordPermissionModule} block on that entity.
 *
 * <p>Passwords are stored as {@code "saltHex:hashHex"} strings (SHA-256).
 * An empty string means no password is set — the module grants no access by itself.
 *
 * <p>Format: {@code VERSION(1) | count(int) | [abs(long) saltHash(String)] * count}
 */
public class PasswordPermissionModuleContainer extends SystemModule {

	private static final byte VERSION = 1;

	/** absIndex → "saltHex:hashHex" (empty = no password configured). */
	private final Long2ObjectOpenHashMap<String> blockPasswords = new Long2ObjectOpenHashMap<>();

	public PasswordPermissionModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, luamade.LuaMade.getInstance(), ElementRegistry.PASSWORD_PERMISSION_MODULE.getId());
	}

	public static PasswordPermissionModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.PASSWORD_PERMISSION_MODULE.getId()) instanceof PasswordPermissionModuleContainer) {
			return (PasswordPermissionModuleContainer) managerContainer.getModMCModule(ElementRegistry.PASSWORD_PERMISSION_MODULE.getId());
		}
		return null;
	}

	/** Returns the stored {@code "saltHex:hashHex"} for this block, or {@code null} if none. */
	public String getPasswordHash(long absIndex) {
		String hash = blockPasswords.get(absIndex);
		return (hash == null || hash.isEmpty()) ? null : hash;
	}

	public void setPasswordHash(long absIndex, String saltHash) {
		if(saltHash == null || saltHash.isEmpty()) {
			blockPasswords.remove(absIndex);
		} else {
			blockPasswords.put(absIndex, saltHash);
		}
		flagUpdatedData();
	}

	public void removeBlock(long absIndex) {
		if(blockPasswords.remove(absIndex) != null) {
			flagUpdatedData();
		}
	}

	public boolean hasPassword(long absIndex) {
		String hash = blockPasswords.get(absIndex);
		return hash != null && !hash.isEmpty();
	}

	// -------------------------------------------------------------------------
	// SystemModule boilerplate
	// -------------------------------------------------------------------------

	@Override
	public void handlePlace(long abs, byte orientation) {
	}

	@Override
	public void handleRemove(long abs) {
		blockPasswords.remove(abs);
	}

	@Override
	public double getPowerConsumedPerSecondResting() {
		return 0;
	}

	@Override
	public double getPowerConsumedPerSecondCharging() {
		return 0;
	}

	@Override
	public String getName() {
		return "Password Permission Module";
	}

	@Override
	public void handle(Timer timer) {
	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
		buffer.writeByte(VERSION);
		buffer.writeInt(blockPasswords.size());
		for(long abs : blockPasswords.keySet().toLongArray()) {
			buffer.writeLong(abs);
			buffer.writeString(safeString(blockPasswords.get(abs)));
		}
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
		blockPasswords.clear();
		byte version = buffer.readByte();
		if(version != VERSION) {
			return;
		}
		int count = buffer.readInt();
		for(int i = 0; i < count; i++) {
			long abs = buffer.readLong();
			String hash = buffer.readString();
			if(hash != null && !hash.isEmpty()) {
				blockPasswords.put(abs, hash);
			}
		}
	}

	private static String safeString(String value) {
		return value == null ? "" : value;
	}
}
