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
    private final String texturePath = "dovtech/logiscript/resources/textures/";
    private final String[] textureNames = {

    };
    private final String[] iconNames = {

    };

    @Override
    public void initialize() {
        instance = this;
        resourceMap = new HashMap<>();
        for(String textureName : textureNames) {
            resourceMap.put(textureName, StarLoaderTexture.newBlockTexture(LogiScript.getInstance().getJarBufferedImage(texturePath + "blocks/" + textureName + ".png")));
        }
        for(String iconName : iconNames) {
            resourceMap.put(iconName, StarLoaderTexture.newIconTexture(LogiScript.getInstance().getJarBufferedImage(texturePath + "icons/" + iconName + ".png")));
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