package luamade.element.block;

import api.listener.events.block.SegmentPieceActivateByPlayer;
import api.listener.events.block.SegmentPieceActivateEvent;

/**
 * <Description>
 *
 * @author TheDerpGamer
 */
public interface ActivationInterface {

  void onPlayerActivation(SegmentPieceActivateByPlayer event);

  void onLogicActivation(SegmentPieceActivateEvent event);
}
