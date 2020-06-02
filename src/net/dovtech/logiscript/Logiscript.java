package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.entity.Player;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.block.BlockActivateEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.logiscript.blocks.Terminal;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.input.InputState;

public class Logiscript extends StarMod {
    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    @Override
    public void onGameStart() {
        this.modName = "Logiscript";
        this.modAuthor = "DovTech";
        this.modVersion = "0.2.5";
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);

        registerLisenters();
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

    private void registerLisenters() {

        //Terminal Activate Event
        StarLoader.registerListener(BlockActivateEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                BlockActivateEvent event = (BlockActivateEvent) e;
                if(event.getBlockId() == 3200) { //3200 is the Terminal's block ID
                    Player player = new Player(GameClientState.instance.getPlayer());
                    InputState inputState = GameClientState.instance.getGUIController().getState();
                    Terminal terminal = new Terminal();
                    terminal.onBlockActivate(player, inputState);
                }
            }
        });
    }
}