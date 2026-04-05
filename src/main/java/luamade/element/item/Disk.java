package luamade.element.item;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementInterface;
import luamade.element.ElementRegistry;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;

public class Disk implements ElementInterface {

	private ElementInformation itemInfo;

	public Disk() {
	}

	@Override
	public short getId() {
		return itemInfo.id;
	}

	@Override
	public ElementInformation getInfo() {
		return itemInfo;
	}

	@Override
	public void initData() {
		itemInfo = BlockConfig.newElement(LuaMade.getInstance(), "Disk", new short[6]);
		itemInfo.placable = false;
		itemInfo.setDescription("Portable storage media for LuaMade disk drives.");
		itemInfo.setDeprecated(true);
		itemInfo.setInRecipe(false);
		itemInfo.setShoppable(false);
		itemInfo.setPrice(Math.max(1, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price / 2));
		itemInfo.volume = 0.01f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(itemInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(10, (short) 220));
	}

	@Override
	public void initResources() {
		itemInfo.setBuildIconNum(ElementKeyMap.getInfo(6).getBuildIconNum());
		itemInfo.setTextureId(ElementKeyMap.getInfo(6).getTextureIds());
		itemInfo.controlling.add(ElementRegistry.DISK_DRIVE.getId());
	}
}
