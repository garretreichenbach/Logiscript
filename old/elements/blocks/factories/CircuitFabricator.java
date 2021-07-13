package dovtech.logiscript.elements.blocks.factories;

import api.config.BlockConfig;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.elements.blocks.BlockActivationInterface;
import dovtech.logiscript.elements.blocks.Factory;
import dovtech.logiscript.managers.TextureManager;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;

/**
 * CircuitFabricator
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public class CircuitFabricator extends Factory implements BlockActivationInterface {

    public CircuitFabricator() {
        super("Circuit Fabricator", ElementKeyMap.getInfo(ElementKeyMap.FACTORY_ADVANCED_ID).getType(),
                "circuit-fabricator-sides", "circuit-fabricator-sides", "circuit-fabricator-caps",
                "circuit-fabricator-caps", "circuit-fabricator-sides", "circuit-fabricator-sides");
    }

    @Override
    public void initialize() {
        blockInfo.setDescription("A complex machine designed to create computer components.");
        blockInfo.setShoppable(true);
        blockInfo.setPrice(1000);
        blockInfo.setBuildIconNum(TextureManager.getTexture("circuit-fabricator-icon").getTextureId());
        blockInfo.setCanActivate(true);
        blockInfo.maxHitPointsFull = ElementKeyMap.getInfo(ElementKeyMap.FACTORY_ADVANCED_ID).getMaxHitPointsFull();
        blockInfo.mass = ElementKeyMap.getInfo(ElementKeyMap.FACTORY_ADVANCED_ID).getMass();

        BlockConfig.addRecipe(blockInfo, ElementManager.FactoryType.ADVANCED_FACTORY.ordinal(), 7,
                new FactoryResource(5, (short) 259),
                new FactoryResource(3, (short) 212),
                new FactoryResource(1, (short) 677),
                new FactoryResource(3, (short) 121)
        );
        BlockConfig.add(blockInfo);
    }

    @Override
    public void onActivation(SegmentPiece segmentPiece, PlayerState player, PlayerInteractionControlManager controlManager) {

    }
}
