package dovtech.logiscript;

import api.config.BlockConfig;
import api.listener.Listener;
import api.listener.events.block.SegmentPieceActivateByPlayer;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import dovtech.logiscript.elements.ElementManager;
import dovtech.logiscript.elements.blocks.Block;
import dovtech.logiscript.elements.blocks.BlockActivationInterface;
import dovtech.logiscript.elements.blocks.Factory;
import dovtech.logiscript.elements.blocks.computers.MicroController;
import dovtech.logiscript.elements.blocks.factories.CircuitFabricator;
import dovtech.logiscript.elements.items.components.CPU;
import dovtech.logiscript.managers.ScriptManager;
import dovtech.logiscript.managers.SpriteManager;
import dovtech.logiscript.managers.TextureManager;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class LogiScript extends StarMod {

    //Instance
    private static LogiScript instance;
    public static LogiScript getInstance() {
        return instance;
    }
    public static void main(String[] args) {

    }
    public LogiScript() {
    }

    //Config
    private final String[] defaultConfig = {
            "debug-mode: false"
    };
    public boolean debugMode = false;

    //Data
    private final String[] overwriteClasses = {

    };

    @Override
    public void onEnable() {
        instance = this;
        initConfig();
        initResources();
        registerCommands();
        registerListeners();
    }

    @Override
    public byte[] onClassTransform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] byteCode) {
        for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);

        return super.onClassTransform(loader, className, classBeingRedefined, protectionDomain, byteCode);
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        //Items
        ElementManager.addItem(new CPU.Basic());

        //Factories
        ElementManager.addFactory(new CircuitFabricator());

        //Computers
        ElementManager.addBlock(new MicroController());

        ElementManager.initialize();
    }

    private void initConfig() {
        FileConfiguration config = getConfig("config");
        config.saveDefault(defaultConfig);
        debugMode = config.getConfigurableBoolean("debug-mode", false);
    }

    private void initResources() {
        (new ScriptManager()).initialize();
        (new SpriteManager()).initialize();
        (new TextureManager()).initialize();
    }

    private void registerCommands() {

    }

    private void registerListeners() {
        StarLoader.registerListener(SegmentPieceActivateByPlayer.class, new Listener<SegmentPieceActivateByPlayer>() {
            @Override
            public void onEvent(SegmentPieceActivateByPlayer event) {
                Block block = ElementManager.getBlock(event.getSegmentPiece());
                if(block != null) {
                    if(block instanceof BlockActivationInterface) {
                        ((BlockActivationInterface) block).onActivation(event.getSegmentPiece(), event.getPlayer(), event.getControlManager());
                    }
                } else {
                    Factory factory = ElementManager.getFactory(event.getSegmentPiece());
                    if(factory != null) {
                        if(factory instanceof BlockActivationInterface) {
                            ((BlockActivationInterface) factory).onActivation(event.getSegmentPiece(), event.getPlayer(), event.getControlManager());
                        }
                    }
                }
            }
        }, this);
    }

    private byte[] overwriteClass(String className, byte[] byteCode) {
        byte[] bytes = null;
        try {
            ZipInputStream file = new ZipInputStream(new FileInputStream(this.getSkeleton().getJarFile()));
            while(true) {
                ZipEntry nextEntry = file.getNextEntry();
                if(nextEntry == null) break;
                if(nextEntry.getName().endsWith(className + ".class")) bytes = IOUtils.toByteArray(file);
            }
            file.close();
        } catch(IOException exception) {
            exception.printStackTrace();
        }
        if(bytes != null) return bytes;
        else return byteCode;
    }
}