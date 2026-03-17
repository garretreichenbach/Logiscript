package luamade.element.block;

import api.config.BlockConfig;
import luamade.LuaMade;
import luamade.element.ElementInterface;
import org.schema.game.common.data.element.ElementInformation;

public abstract class Block implements ElementInterface {

	protected String name;
	protected ElementInformation blockInfo;

	protected Block(String name) {
		this.name = name;
	}

	@Override
	public void initData() {
		blockInfo = BlockConfig.newElement(LuaMade.getInstance(), name, new short[6]);
	}

	@Override
	public short getId() {
		return blockInfo.id;
	}

	@Override
	public ElementInformation getInfo() {
		return blockInfo;
	}
}