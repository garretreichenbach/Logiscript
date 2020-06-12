package net.dovtech.logiscript.blocks;

import api.config.BlockConfig;
import api.element.block.Blocks;
import org.schema.game.common.data.element.ElementInformation;

public class Terminal {

    public static ElementInformation blockInfo;

    public Terminal() {
        blockInfo = BlockConfig.newElement("Terminal");
        blockInfo.fullName = "Terminal";
        blockInfo.description = "A terminal used to create logic scrips.";
        blockInfo.shoppable = true;
        blockInfo.price = 15000;
        blockInfo.volume = Blocks.BOBBY_AI_MODULE.getInfo().volume;
        blockInfo.mass = Blocks.BOBBY_AI_MODULE.getInfo().mass;
        blockInfo.canActivate = true;
        blockInfo.systemBlock = true;
    }
}
