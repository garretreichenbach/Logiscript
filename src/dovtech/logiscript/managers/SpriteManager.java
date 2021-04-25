package dovtech.logiscript.managers;

import api.utils.textures.StarLoaderTexture;
import dovtech.logiscript.LogiScript;
import org.schema.schine.graphicsengine.forms.Sprite;
import java.util.HashMap;

/**
 * SpriteManager
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/25/2021
 */
public class SpriteManager implements ResourceManager {

    private static SpriteManager instance;
    public static SpriteManager getInstance() {
        return instance;
    }

    private HashMap<String, Sprite> resourceMap;
    private final String resourcePath = "dovtech/logiscript/resources/sprites/";
    private final String[] resourceNames = {
    };

    @Override
    public void initialize() {
        instance = this;
        resourceMap = new HashMap<>();
        for(String resourceName : resourceNames) {
            Sprite resource = StarLoaderTexture.newSprite(LogiScript.getInstance().getJarBufferedImage(resourcePath + resourceName + ".png"), LogiScript.getInstance(), resourceName);
            resource.setName(resourceName);
            resourceMap.put(resourceName, resource);
        }
    }

    @Override
    public Sprite getResource(String name) {
        return resourceMap.get(name);
    }

    public static Sprite getSprite(String name) {
        return getInstance().getResource(name);
    }
}