package luamade.element.block;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementInterface;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;

public class RemoteAccessPoint implements ElementInterface {

	private ElementInformation blockInfo;

	@Override
	public short getId() {
		return blockInfo.id;
	}

	@Override
	public ElementInformation getInfo() {
		return blockInfo;
	}

	@Override
	public void initData() {
		blockInfo = BlockConfig.newElement(LuaMade.getInstance(), "Remote Access Point", new short[6]);
		blockInfo.setDescription("Lets linked remote control items forward input to a LuaMade computer without opening its UI.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(20, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(451).getTextureIds());
	}
}
