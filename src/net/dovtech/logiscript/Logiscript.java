package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.block.BlockModifyEvent;
import api.listener.events.block.ClientActivateSegmentPieceEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.logiscript.blocks.Terminal;
import net.dovtech.logiscript.gui.TerminalGUI;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.resource.tag.Tag;

import java.io.File;
import java.util.Arrays;

public class Logiscript extends StarMod {
    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }
    private File lscriptsFolder = new File("scripts");

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    @Override
    public void onGameStart() {
        this.modName = "Logiscript";
        this.modAuthor = "DovTech";
        this.modVersion = "0.2.6";
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";
        if(!lscriptsFolder.exists()) lscriptsFolder.mkdir();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);

        //Terminal Activate Event
        StarLoader.registerListener(ClientActivateSegmentPieceEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                final ClientActivateSegmentPieceEvent event = (ClientActivateSegmentPieceEvent) e;
                if(event.getPiece().getInfo().getName().equals("Terminal")) {
                    new TerminalGUI(event.getPiece(), event.getPicm().getState(), 300, 300, "TERMINAL");
                }
            }
        });
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        Terminal terminal = new Terminal();
        ElementInformation terminalInfo = terminal.getBlockInfo();
        FactoryResource[] terminalRecipe = {
            new FactoryResource(1, Blocks.DISPLAY_MODULE.getId()),
            new FactoryResource(5, Blocks.ACTIVATION_MODULE.getId()),
            new FactoryResource(2, Blocks.SENSOR.getId()),
            new FactoryResource(1, Blocks.BOBBY_AI_MODULE.getId()),
        };
        BlockConfig.addRecipe(terminalInfo, FactoryType.ADVANCED, 10, terminalRecipe);
        config.add(terminalInfo);
    }

    public static Logiscript getInstance() {
        return inst;
    }
}