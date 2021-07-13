package dovtech.logiworks.elements.blocks;

import api.config.BlockConfig;
import dovtech.logiworks.LogiWorks;
import dovtech.logiworks.elements.ElementManager;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Block
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public abstract class Block {

    protected ElementInformation blockInfo;

    public Block(String name, ElementCategory category) {
        blockInfo = BlockConfig.newElement(LogiWorks.getInstance(), name, new short[6]);
        BlockConfig.setElementCategory(blockInfo, category);
        ElementManager.addBlock(this);
    }

    public final ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public final short getId() {
        return blockInfo.getId();
    }

    public abstract void initialize();
}