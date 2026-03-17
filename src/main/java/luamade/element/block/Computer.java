package luamade.element.block;

import api.config.BlockConfig;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;

public class Computer extends Block {

	public Computer() {
		super("Computer");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A fully programmable computer utilizing the LUA language.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
	}

	@Override
	public void postInitData() {
		blockInfo.controlledBy.add((short) 405);
		blockInfo.controlledBy.add((short) 993);
		blockInfo.controlledBy.add((short) 666);
		blockInfo.controlledBy.add((short) 399);

		ElementKeyMap.getInfo(405).controlling.add(getId());
		ElementKeyMap.getInfo(993).controlling.add(getId());
		ElementKeyMap.getInfo(666).controlling.add(getId());
		ElementKeyMap.getInfo(399).controlling.add(getId());

		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(50, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(451).getTextureIds());
	}
}