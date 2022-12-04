package luamade.element.item;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.manager.ResourceManager;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/26/2021
 */
public abstract class Item {

	protected ElementInformation itemInfo;

	public Item(String name, ElementCategory category) {
		String internalName = name.toLowerCase().replace(" ", "-").trim();
		short textureId = (short) ResourceManager.getTexture(internalName).getTextureId();
		itemInfo = BlockConfig.newElement(LuaMade.getInstance(), name, textureId);
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
