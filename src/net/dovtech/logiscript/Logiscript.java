package net.dovtech.logiscript;

import api.DebugFile;
import api.common.GameCommon;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import net.dovtech.logiscript.blocks.Terminal;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import java.io.File;

public class Logiscript extends StarMod {

    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }

    //Scripts
    private File scriptsFolder;

    //Configuration
    private String[] defaultConfig = {
            "debug-mode: false"
    };

    public boolean debugMode = false;

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    public enum GameMode {CLIENT, SERVER, SINGLEPLAYER}

    @Override
    public void onGameStart() {
        setModName("Logiscript");
        setModAuthor("Dovtech");
        setModVersion("0.3.1");
        setModSMVersion("0.202.108");
        setModDescription("Adds lua-based logic scripting to StarMade.");
    }

    @Override
    public void onEnable() {
        super.onEnable();

        if (getGameMode().equals(GameMode.SERVER) || getGameMode().equals(GameMode.SINGLEPLAYER)) {
            initConfig();
        }

        registerPackets();
        registerCommands();
        registerListeners();
        loadScripts();
        DebugFile.log("Enabled", this);
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        Terminal terminal = new Terminal();
        ElementInformation terminalInfo = Terminal.blockInfo;
        FactoryResource[] terminalRecipe = {
            new FactoryResource(4, Blocks.DISPLAY_MODULE.getId()),
            new FactoryResource(5, Blocks.ACTIVATION_MODULE.getId()),
            new FactoryResource(5, Blocks.NOT_SIGNAL.getId()),
            new FactoryResource(3, Blocks.SENSOR.getId()),
            new FactoryResource(1, Blocks.BOBBY_AI_MODULE.getId()),
        };
        BlockConfig.addRecipe(terminalInfo, (int) FactoryType.ADVANCED.getId(), 10, terminalRecipe);
        config.add(terminalInfo);
    }

    public static Logiscript getInstance() {
        return inst;
    }

    public GameMode getGameMode() {
        if (GameCommon.isDedicatedServer()) {
            return GameMode.SERVER;
        } else if (GameCommon.isOnSinglePlayer()) {
            return GameMode.SINGLEPLAYER;
        } else if (GameCommon.isClientConnectedToServer()) {
            return GameMode.CLIENT;
        } else {
            return GameMode.CLIENT;
        }
    }

    private void initConfig() {
        FileConfiguration config = getConfig("config");
        config.saveDefault(defaultConfig);

        this.debugMode = Boolean.parseBoolean(config.getString("debug-mode"));

        DebugFile.log("Loaded Config", this);
    }

    private void registerPackets() {

        DebugFile.log("Registered Packets", this);
    }

    private void registerCommands() {

        DebugFile.log("Registered Commands", this);
    }

    private void registerListeners() {

        DebugFile.log("Registered Listeners", this);
    }

    private void loadScripts() {
        scriptsFolder = new File("moddata/Logiscript/scripts/");
        if(!scriptsFolder.exists()) scriptsFolder.mkdir();


        DebugFile.log("Loaded Scripts", this);
    }
}