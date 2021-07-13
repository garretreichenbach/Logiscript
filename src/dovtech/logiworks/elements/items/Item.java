package dovtech.logiworks.elements.items;

import api.config.BlockConfig;
import dovtech.logiworks.LogiWorks;
import dovtech.logiworks.elements.ElementManager;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

/**
 * Item
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public abstract class Item {

    protected ElementInformation itemInfo;

    public Item(String name, ElementCategory category) {
        itemInfo = BlockConfig.newElement(LogiWorks.getInstance(), name, new short[6]);
        itemInfo.setPlacable(false);
        itemInfo.setPhysical(false);
        BlockConfig.setElementCategory(itemInfo, category);
        ElementManager.addItem(this);
    }

    public final ElementInformation getItemInfo() {
        return itemInfo;
    }

    public final short getId() {
        return itemInfo.getId();
    }

    public abstract void initialize();
}
