package luamade.element.block;

import api.config.BlockConfig;
import luamade.LuaMade;
import org.schema.game.common.data.element.ElementCategory;
import org.schema.game.common.data.element.ElementInformation;

public abstract class Block {

	protected ElementInformation blockInfo;

	public Block(String name, ElementCategory category) {
		blockInfo = BlockConfig.newElement(LuaMade.getInstance(), name, new short[6]);
		BlockConfig.setElementCategory(blockInfo, category);
	}

	public final ElementInformation getBlockInfo() {
		return blockInfo;
	}

	public final short getId() {
		return blockInfo.getId();
	}

	public abstract void initialize();
}
