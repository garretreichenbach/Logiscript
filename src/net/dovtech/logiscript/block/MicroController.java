package net.dovtech.logiscript.block;

import api.element.block.Blocks;
import org.schema.game.common.data.element.ElementInformation;

public class MicroController {

    public static ElementInformation blockInfo;

    public MicroController() {
        blockInfo.setFullName("Micro Controller");
        blockInfo.setDescription("A basic computer designed to run simple tasks from an inserted Program Card.");
        blockInfo.setShoppable(true);
        blockInfo.setPrice(8000);
        blockInfo.setInRecipe(true);
        blockInfo.setDoor(false);
        blockInfo.controlledBy.add(Blocks.SHIP_CORE.getId());
    }
}
