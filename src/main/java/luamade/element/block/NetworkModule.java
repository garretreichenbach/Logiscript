package luamade.element.block;

import api.config.BlockConfig;
import api.utils.element.Blocks;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import org.schema.game.client.view.cubes.shapes.BlockStyle;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;

public class NetworkModule extends Block {

	public NetworkModule() {
		super("Network Module");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("Module that provides networking capabilities to connected computers.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 4);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;
	}


	@Override
	public void postInitData() {
		if(ElementRegistry.isRRSInstalled()) {
			BlockConfig.addRecipe(blockInfo, ElementRegistry.RRSElements.BLOCK_ASSEMBLER.getId(), (int) Blocks.DISPLAY_MODULE.getInfo().getFactoryBakeTime(),
					new FactoryResource(1, ElementRegistry.RRSElements.AEGIUM_FIELD_EMITTER.getId()),
					new FactoryResource(3, ElementRegistry.RRSElements.PARSYNE_AMPLIFYING_FOCUS.getId()));
		} else {
			BlockConfig.addRecipe(blockInfo, Blocks.ADVANCED_FACTORY.getId(), (int) Blocks.DISPLAY_MODULE.getInfo().getFactoryBakeTime(),
					new FactoryResource(1, Blocks.WIRELESS_LOGIC_MODULE.getId()),
					new FactoryResource(1, Blocks.SMALL_ACTIVATOR.getId()));
		}
	}

	@Override
	public void initResources() {
		BlockConfig.assignLod(blockInfo, LuaMade.getInstance(), "Computer", null);
		blockInfo.blockStyle = BlockStyle.NORMAL24;
		blockInfo.setBuildIconNum(Blocks.WIRELESS_LOGIC_MODULE.getInfo().getBuildIconNum());
	}
}