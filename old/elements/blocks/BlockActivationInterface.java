package dovtech.logiscript.elements.blocks;

import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

/**
 * BlockActivationInterface
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public interface BlockActivationInterface {

    void onActivation(SegmentPiece segmentPiece, PlayerState player, PlayerInteractionControlManager controlManager);
}
