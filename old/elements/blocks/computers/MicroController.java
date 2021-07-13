package dovtech.logiscript.elements.blocks.computers;

import api.config.BlockConfig;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.elements.blocks.Block;
import dovtech.logiscript.elements.blocks.BlockActivationInterface;
import dovtech.logiscript.managers.TextureManager;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;

public class MicroController extends Block implements BlockActivationInterface {

    public static ElementInformation blockInfo;

    public MicroController() {
        super("Micro Controller", ElementKeyMap.getInfo(ElementKeyMap.ACTIVAION_BLOCK_ID).getType(),
                "micro-controller-front", "micro-controller-sides", "micro-controller-sides", "micro-controller-sides", "micro-controller-sides", "micro-controller-sides");
    }

    @Override
    public void initialize() {
        blockInfo.setDescription("A simple computer designed to regulate and control subsystems.");
        blockInfo.setShoppable(true);
        blockInfo.setPrice((ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 3) + ElementKeyMap.getInfo(ElementKeyMap.SIGNAL_SENSOR).price);
        blockInfo.setBuildIconNum(TextureManager.getTexture("micro-controller-icon").getTextureId());
        blockInfo.setCanActivate(true);
        blockInfo.maxHitPointsFull = ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getMaxHitPointsFull() * 2;
        blockInfo.mass = ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getMass() * 2;

        BlockConfig.addRecipe(blockInfo, ElementManager.FactoryType.ADVANCED_FACTORY.ordinal(), 5,
                new FactoryResource(5, ElementKeyMap.TEXT_BOX),
                new FactoryResource(1, ElementManager.getItem("Basic CPU").getId())
        );
        BlockConfig.add(blockInfo);
    }

    @Override
    public void onActivation(SegmentPiece segmentPiece, PlayerState player, PlayerInteractionControlManager controlManager) {

    }
}
