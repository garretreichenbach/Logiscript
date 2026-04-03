package luamade.listener;

import api.listener.Listener;
import api.listener.events.block.BlockPublicPermissionEvent;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.manager.PasswordAuthManager;
import luamade.utils.SegmentPieceUtils;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.util.ArrayList;

/**
 * Hooks into StarMade's {@link BlockPublicPermissionEvent} to extend the native
 * permission system with our {@link luamade.element.block.PasswordPermissionModule}.
 *
 * <p>The event fires for every adjacency-based permission check: rail docking,
 * personal beam targeting, shipyard operations, etc. If permission has already
 * been granted by a native block (346 or 936), we don't interfere. If permission
 * was denied, we check whether any adjacent PasswordPermissionModule has been
 * authenticated by the accessing faction.
 */
public class BlockPublicPermissionListener {

	public static void register(LuaMade instance) {
		StarLoader.registerListener(BlockPublicPermissionEvent.class, new Listener<BlockPublicPermissionEvent>() {
			@Override
			public void onEvent(BlockPublicPermissionEvent event) {
				// If already allowed by native logic, don't interfere.
				if(event.getPermission()) return;

				int accessingFaction = event.getAccessingFactionId();
				if(accessingFaction == 0) return; // no-faction entities can't auth

				SegmentController sc = event.getSegmentController();
				if(sc == null) return;

				SegmentPiece targetPiece = sc.getSegmentBuffer().getPointUnsave(event.getBlockPos());
				if(targetPiece == null) return;

				short pwdModuleId = ElementRegistry.PASSWORD_PERMISSION_MODULE.getId();
				ArrayList<SegmentPiece> modules = SegmentPieceUtils.getMatchingAdjacent(targetPiece, pwdModuleId);
				for(SegmentPiece module : modules) {
					if(PasswordAuthManager.isAuthed(accessingFaction, sc, module.getAbsoluteIndex())) {
						event.setPermission(true);
						return;
					}
				}
			}
		}, instance);
	}
}
