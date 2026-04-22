package luamade.lua.element.block;

import luamade.lua.ftp.FtpApi;
import luamade.lua.networking.NetworkInterface;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeClass;
import luamade.system.module.ComputerModule;
import luamade.system.module.NetworkModuleContainer;
import org.luaj.vm2.LuaError;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;

/**
 * Lua-facing peripheral wrapper for a Network Module block.
 *
 * <p>Provides access to the networking API ({@link NetworkInterface}) and
 * the FTP API ({@link FtpApi}) through the peripheral system:
 * <pre>
 * local nm  = peripheral.wrapRelative("front", "networkmodule")
 * local net = nm.getNet()
 * local ftp = nm.getFtp()
 * </pre>
 */
@LuaMadeClass("NetworkModule")
public class NetworkModuleBlock extends Block {

	private final ComputerModule module;

	public NetworkModuleBlock(SegmentPiece piece, ComputerModule module) {
		super(piece, module);
		this.module = module;
	}

	/**
	 * Returns the {@link NetworkInterface} for this Network Module block.
	 * The interface is created lazily and reused across calls.
	 */
	@LuaMadeCallable
	public NetworkInterface getNet() {
		SegmentPiece piece = getSegmentPiece();
		NetworkModuleContainer container = requireContainer(piece);
		String uuid = container.getOrAssignUuid(piece.getAbsoluteIndex());
		return NetworkInterface.getOrCreate(piece, uuid);
	}

	/**
	 * Returns an {@link FtpApi} bound to this Network Module's hostname and
	 * the calling computer's filesystem.
	 */
	@LuaMadeCallable
	public FtpApi getFtp() {
		return new FtpApi(getNet(), module.getFileSystem());
	}

	private NetworkModuleContainer requireContainer(SegmentPiece piece) {
		if(!(piece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			throw new LuaError("Network module is not available on this structure type");
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) piece.getSegmentController();
		NetworkModuleContainer container = NetworkModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) {
			throw new LuaError("Network module container is not initialized");
		}
		return container;
	}
}
