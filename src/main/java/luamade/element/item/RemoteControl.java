package luamade.element.item;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementInterface;
import luamade.element.ElementRegistry;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;

public class RemoteControl implements ElementInterface {

	private ElementInformation itemInfo;

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
		itemInfo = BlockConfig.newElement(LuaMade.getInstance(), "Remote Control", new short[6]);
		itemInfo.placable = false;
		itemInfo.setDescription("Portable controller that can link to LuaMade remote access points and forward player inputs.");
		itemInfo.setInRecipe(true);
		itemInfo.setShoppable(true);
		itemInfo.setPrice(Math.max(1, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price / 2));
		itemInfo.volume = 0.01f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(itemInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(12, (short) 220));
	}

	@Override
	public void initResources() {
		itemInfo.setBuildIconNum(ElementKeyMap.getInfo(6).getBuildIconNum());
		itemInfo.setTextureId(ElementKeyMap.getInfo(6).getTextureIds());
		itemInfo.controlling.add(ElementRegistry.REMOTE_ACCESS_POINT.getId());
	}
}
