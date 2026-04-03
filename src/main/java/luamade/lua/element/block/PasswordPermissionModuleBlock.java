package luamade.lua.element.block;

import luamade.element.ElementRegistry;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.manager.PasswordAuthManager;
import luamade.system.module.ComputerModule;
import luamade.system.module.PasswordPermissionModuleContainer;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Lua-facing wrapper for a {@link luamade.element.block.PasswordPermissionModule} block.
 *
 * <h2>Configuration</h2>
 * Set the password from a computer on the <em>same entity</em> as the module:
 * <pre>
 * local ppm = block.wrapAs(myPiece, "passwordmodule")
 * ppm:setPassword("secret")
 * </pre>
 *
 * <h2>Authentication</h2>
 * Authenticate from any computer whose faction should gain access:
 * <pre>
 * local ppm = block.wrapAs(nearbyPiece, "passwordmodule")
 * if ppm:auth("secret") then
 *     print("Access granted for 5 minutes")
 * end
 * </pre>
 * A successful {@code auth()} registers the calling computer's faction in
 * {@link PasswordAuthManager}. StarMade's native permission checks (docking, rail,
 * beam, console activation) will honour this auth for the TTL duration.
 */
@LuaMadeClass("PasswordPermissionModule")
public class PasswordPermissionModuleBlock extends Block {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final ComputerModule module;

	public PasswordPermissionModuleBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
		this.module = module;
	}

	// -------------------------------------------------------------------------
	// Auth
	// -------------------------------------------------------------------------

	/**
	 * Verifies {@code password} against this module's stored password.
	 * On success, the calling computer's faction is authenticated for
	 * {@link PasswordAuthManager#DEFAULT_TTL_MS} ms.
	 *
	 * @return {@code true} if the password matched.
	 */
	@LuaMadeCallable
	public Boolean auth(String password) {
		if(password == null) return false;
		SegmentPiece piece = getSegmentPiece();
		PasswordPermissionModuleContainer container = requireContainer(piece);
		String saltHash = container.getPasswordHash(piece.getAbsoluteIndex());
		if(saltHash == null) {
			// No password configured — auth always succeeds, but register the faction
			// so that the event listener and mixin can still observe it.
			PasswordAuthManager.authenticate(computerFaction(), piece.getSegmentController(), piece.getAbsoluteIndex());
			return true;
		}
		if(!verifyPassword(password, saltHash)) return false;
		PasswordAuthManager.authenticate(computerFaction(), piece.getSegmentController(), piece.getAbsoluteIndex());
		return true;
	}

	/**
	 * Revokes the current computer's faction auth for this module.
	 */
	@LuaMadeCallable
	public void deauth() {
		SegmentPiece piece = getSegmentPiece();
		PasswordAuthManager.deauthenticate(computerFaction(), piece.getSegmentController(), piece.getAbsoluteIndex());
	}

	/**
	 * Returns {@code true} if the calling computer's faction is currently authenticated.
	 */
	@LuaMadeCallable
	public Boolean isAuthed() {
		SegmentPiece piece = getSegmentPiece();
		return PasswordAuthManager.isAuthed(computerFaction(), piece.getSegmentController(), piece.getAbsoluteIndex());
	}

	// -------------------------------------------------------------------------
	// Password management (same-entity computers only)
	// -------------------------------------------------------------------------

	/**
	 * Sets the password for this module. Only allowed from a computer on the
	 * <em>same entity</em> as the module.
	 *
	 * @param password the new password, or {@code nil}/empty to clear it.
	 */
	@LuaMadeCallable
	public void setPassword(String password) {
		requireSameEntity();
		SegmentPiece piece = getSegmentPiece();
		PasswordPermissionModuleContainer container = requireContainer(piece);
		container.setPasswordHash(piece.getAbsoluteIndex(), buildSaltHash(password));
	}

	/**
	 * Removes the password from this module. The module will then grant access
	 * to any faction that calls {@code auth()} regardless of what they pass.
	 * Requires same-entity computer.
	 */
	@LuaMadeCallable
	public void clearPassword() {
		requireSameEntity();
		SegmentPiece piece = getSegmentPiece();
		PasswordPermissionModuleContainer container = requireContainer(piece);
		container.setPasswordHash(piece.getAbsoluteIndex(), null);
	}

	/**
	 * Returns {@code true} when a password is configured on this module.
	 */
	@LuaMadeCallable
	public Boolean isProtected() {
		SegmentPiece piece = getSegmentPiece();
		PasswordPermissionModuleContainer container = requireContainer(piece);
		return container.hasPassword(piece.getAbsoluteIndex());
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private int computerFaction() {
		return module.getSegmentPiece().getSegmentController().getFactionId();
	}

	private void requireSameEntity() {
		if(getSegmentPiece().getSegmentController() != module.getSegmentPiece().getSegmentController()) {
			throw new LuaError("Password configuration requires a computer on the same entity as the Permission Module");
		}
	}

	private static PasswordPermissionModuleContainer requireContainer(SegmentPiece piece) {
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("Password Permission Module is not available on this structure type");
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		PasswordPermissionModuleContainer container = PasswordPermissionModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			throw new LuaError("Password Permission Module container is not initialized");
		}
		return container;
	}

	// ── Password helpers ──────────────────────────────────────────────────────

	private static String buildSaltHash(String password) {
		if(password == null || password.isEmpty()) return "";
		byte[] salt = new byte[16];
		SECURE_RANDOM.nextBytes(salt);
		byte[] hash = hashPassword(password, salt);
		return toHex(salt) + ":" + toHex(hash);
	}

	static boolean verifyPassword(String password, String saltHash) {
		if(saltHash == null || saltHash.isEmpty()) return true;
		int colon = saltHash.indexOf(':');
		if(colon < 0) return false;
		byte[] salt = fromHex(saltHash.substring(0, colon));
		String expected = saltHash.substring(colon + 1);
		byte[] hash = hashPassword(password, salt);
		return MessageDigest.isEqual(fromHex(expected), hash);
	}

	private static byte[] hashPassword(String password, byte[] salt) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			return md.digest(password.getBytes(StandardCharsets.UTF_8));
		} catch(NoSuchAlgorithmException e) {
			throw new LuaError("SHA-256 is not available on this JVM: " + e.getMessage());
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for(byte b : bytes) sb.append(String.format("%02x", b & 0xff));
		return sb.toString();
	}

	private static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] data = new byte[len / 2];
		for(int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
		}
		return data;
	}
}
