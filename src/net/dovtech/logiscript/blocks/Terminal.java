package net.dovtech.logiscript.blocks;

import api.config.BlockConfig;
import api.element.block.Blocks;
import api.entity.Player;
import net.dovtech.logiscript.gui.TerminalGUI;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIResizableGrabbableWindow;
import org.schema.schine.input.InputState;

public class Terminal {

    private ElementInformation blockInfo;

    public Terminal() {
        blockInfo = BlockConfig.newElement("Terminal", new short[] { 3200 });
        blockInfo.fullName = "Terminal";
        blockInfo.description = "A terminal used to create logic scrips.";
        blockInfo.shoppable = true;
        blockInfo.price = 15000;
        blockInfo.volume = Blocks.BOBBY_AI_MODULE.getInfo().volume;
        blockInfo.mass = Blocks.BOBBY_AI_MODULE.getInfo().mass;
        blockInfo.canActivate = true;
    }

    public ElementInformation getBlockInfo() {
        return blockInfo;
    }

    public void onBlockActivate(Player player, InputState inputState) {
        if(blockInfo.canActivate) {
            TerminalGUI terminalGUI = new TerminalGUI(inputState, 300, 300, "TERMINAL");
        }
    }
}
