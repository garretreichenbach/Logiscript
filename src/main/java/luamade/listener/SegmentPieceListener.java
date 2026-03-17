package luamade.listener;

import api.listener.fastevents.segmentpiece.*;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Segment;

public class SegmentPieceListener implements SegmentPiecePlayerInteractListener, SegmentPieceAddByMetadataListener, SegmentPieceAddListener, SegmentPieceRemoveListener, SegmentPieceKilledListener {

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {

	}

	@Override
	public void onAdd(short i, byte b, byte b1, byte b2, byte b3, long l, boolean b4) {

	}

	@Override
	public void onAdd(SegmentController segmentController, short i, byte b, byte b1, byte b2, byte b3, Segment segment, boolean b4, long l, boolean b5) {

	}

	@Override
	public void onBlockRemove(short i, int i1, byte b, byte b1, byte b2, byte b3, Segment segment, boolean b4, boolean b5) {

	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {

	}
}
