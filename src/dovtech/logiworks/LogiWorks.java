package dovtech.logiworks;

import api.config.BlockConfig;
import api.mod.StarMod;
import dovtech.logiworks.elements.ElementManager;
import dovtech.logiworks.utils.ConfigManager;
import dovtech.logiworks.utils.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.schema.schine.resource.ResourceLoader;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * LogiWorks mod main class.
 *
 * @author TheDerpGamer, Captain Skwidz
 * @since 07/13/2021
 */
public class LogiWorks extends StarMod {

    //Instance
    private static LogiWorks instance;
    public static LogiWorks getInstance() {
        return instance;
    }
    public static void main(String[] args) { }
    public LogiWorks() { }

    //Data
    private final String[] overwriteClasses = {

    };

    @Override
    public void onEnable() {
        instance = this;
        ConfigManager.initialize(this);
        registerCommands();
        registerListeners();
    }

    @Override
    public byte[] onClassTransform(String className, byte[] byteCode) {
        for(String name : overwriteClasses) if(className.endsWith(name)) return overwriteClass(className, byteCode);
        return super.onClassTransform(className, byteCode);
    }

    @Override
    public void onResourceLoad(ResourceLoader loader) {
        ResourceManager.loadResources(this);
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {

        ElementManager.initialize();
    }

    private void registerCommands() {

    }

    private void registerListeners() {

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