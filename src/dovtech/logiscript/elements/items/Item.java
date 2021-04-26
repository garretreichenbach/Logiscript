package dovtech.logiscript.elements.items;

import api.config.BlockConfig;
import dovtech.logiscript.LogiScript;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.managers.TextureManager;
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
        String internalName = name.toLowerCase().replace(" ", "-").trim();
        short textureId = (short) TextureManager.getTexture(internalName).getTextureId();
        itemInfo = BlockConfig.newElement(LogiScript.getInstance(), name, textureId);
        itemInfo.setBuildIconNum(textureId);
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
