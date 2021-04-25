package dovtech.logiscript.managers;

import api.utils.textures.StarLoaderTexture;
import dovtech.logiscript.LogiScript;
import java.util.HashMap;

/**
 * TextureManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public class TextureManager implements ResourceManager {

    private static TextureManager instance;
    public static TextureManager getInstance() {
        return instance;
    }

    private HashMap<String, StarLoaderTexture> resourceMap;
    private final String resourcePath = "dovtech/logiscript/resources/textures/";
    private final String[] resourceNames = {

    };

    @Override
    public void initialize() {
        instance = this;
        resourceMap = new HashMap<>();
        for(String resourceName : resourceNames) {
            resourceMap.put(resourceName, StarLoaderTexture.newBlockTexture(LogiScript.getInstance().getJarBufferedImage(resourcePath + resourceName + ".png")));
        }
    }

    @Override
    public StarLoaderTexture getResource(String name) {
        return resourceMap.get(name);
    }

    public static StarLoaderTexture getTexture(String name) {
        return getInstance().getResource(name);
    }
}