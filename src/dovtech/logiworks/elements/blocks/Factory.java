package dovtech.logiworks.elements.blocks;

import api.config.BlockConfig;
import dovtech.logiworks.LogiWorks;
import dovtech.logiworks.elements.ElementManager;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Factory
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public abstract class Factory {

    protected ElementInformation blockInfo;

    public Factory(String name, ElementCategory category) {
        blockInfo = BlockConfig.newFactory(LogiWorks.getInstance(), name, new short[6]);
        BlockConfig.setElementCategory(blockInfo, category);
        ElementManager.addFactory(this);
    }

    public final ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public final short getId() {
        return blockInfo.getId();
    }

    public abstract void initialize();
}
