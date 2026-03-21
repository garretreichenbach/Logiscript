package luamade.element.block;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementInterface;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.inventory.Inventory;

public class DiskDrive implements ElementInterface {

	private ElementInformation blockInfo;

	public DiskDrive() {
	}

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
		blockInfo = BlockConfig.newElement(LuaMade.getInstance(), "Disk Drive", new short[6]);
		blockInfo.setDescription("Single-slot drive that can read and write LuaMade disk items.");
		blockInfo.setInRecipe(false);
		blockInfo.setShoppable(false);
		blockInfo.setDeprecated(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
		blockInfo.inventoryType = Inventory.INVENTORY_TYPE_OTHER;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(25, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(16).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(16).getTextureIds());
	}
}