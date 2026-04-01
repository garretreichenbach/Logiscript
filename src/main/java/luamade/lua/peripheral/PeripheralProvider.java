package luamade.lua.peripheral;

import luamade.lua.element.block.Block;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;

/**
 * Implemented by anything that can wrap a SegmentPiece into a Block subclass.
 * Register instances with {@link PeripheralRegistry#register(PeripheralProvider)}.
 * <p></p>
 * Built-in providers are registered automatically on startup. Other mods may
 * call {@code register()} at any point before their scripts run.
 */
public interface PeripheralProvider {
	/**
	 * The lower-case string aliases that map to this provider when the script
	 * calls {@code wrap(block, "typename")} or {@code wrapAs(piece, "typename")}.
	 * Names must be unique across all registered providers.
	 */
	String[] getTypeNames();

	/**
	 * Returns true if this provider can auto-wrap the given piece.
	 * Providers are tried in registration order; the first match wins.
	 */
	boolean canWrap(SegmentPiece piece);

	/**
	 * Constructs and returns the appropriate Block subclass for the given piece.
	 * Called only when {@link #canWrap} returned true, or when the user
	 * explicitly requested this provider by type name.
	 */
	Block wrap(SegmentPiece piece, ComputerModule module);
}
