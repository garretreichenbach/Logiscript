package dovtech.logiscript.managers;

import api.utils.other.HashList;
import dovtech.logiscript.scripts.Script;
import dovtech.logiscript.utils.DataUtils;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ScriptManager {

    private static ScriptManager instance;
    public static ScriptManager getInstance() {
        return instance;
    }

    private HashList<String, Script> resourceMap;
    private final String resourcePath = DataUtils.getWorldDataPath() + "/scripts";

    public void initialize() {
        instance = this;
        resourceMap = new HashList<>();

        File scriptsFolder = new File(resourcePath);
        if(!scriptsFolder.exists()) scriptsFolder.mkdirs();
        else loadScripts(scriptsFolder);
    }

    private void loadScripts(File scriptsFolder) {
        for(File scriptFile : Objects.requireNonNull(scriptsFolder.listFiles())) {
            try {
                Script script = Script.readFile(scriptFile);
                resourceMap.add(script.getUploader(), script);
            } catch(IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public Script getResource(String uploaderName, String scriptName) {
        return null;
    }
}
