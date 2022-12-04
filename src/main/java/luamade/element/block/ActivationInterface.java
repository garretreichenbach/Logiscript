package luamade.element.block;

import api.listener.events.block.SegmentPieceActivateByPlayer;
import org.schema.game.common.data.SegmentPiece;

/**
 * <Description>
 *
 * @author TheDerpGamer
 */
public interface ActivationInterface {

  void onPlayerActivation(SegmentPieceActivateByPlayer event);

  void onLogicActivation(SegmentPiece target, boolean active);
}
