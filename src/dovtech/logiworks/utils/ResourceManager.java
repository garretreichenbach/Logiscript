package dovtech.logiworks.utils;

import api.utils.textures.StarLoaderTexture;
import dovtech.logiworks.LogiWorks;
import org.schema.schine.graphicsengine.forms.Sprite;

import java.util.HashMap;

/**
 * LogiWorks mod resource manager.
 *
 * @author TheDerpGamer
 * @since 06/15/2021
 */
public class ResourceManager {

    private static final String[] textureNames = {

    };

    private static final String[] spriteNames = {

    };

    private static HashMap<String, StarLoaderTexture> textureMap = new HashMap<>();
    private static HashMap<String, Sprite> spriteMap = new HashMap<>();

    public static void loadResources(final LogiWorks instance) {

        StarLoaderTexture.runOnGraphicsThread(new Runnable() {
            @Override
            public void run() {
                //Load Textures
                for(String textureName : textureNames) {
                    try {
                        if(textureName.endsWith("icon")) {
                            textureMap.put(textureName, StarLoaderTexture.newIconTexture(instance.getJarBufferedImage("dovtech/logiworks/resources/textures/" + textureName + ".png")));
                        } else {
                            textureMap.put(textureName, StarLoaderTexture.newBlockTexture(instance.getJarBufferedImage("dovtech/logiworks/resources/textures/" + textureName + ".png")));
                        }
                    } catch(Exception exception) {
                        LogManager.logException("Failed to load texture \"" + textureName + "\"", exception);
                    }
                }

                //Load Sprites
                for(String spriteName : spriteNames) {
                    try {
                        Sprite sprite = StarLoaderTexture.newSprite(instance.getJarBufferedImage("dovtech/logiworks/resources/sprites/" + spriteName + ".png"), instance, spriteName);
                        sprite.setPositionCenter(true);
                        sprite.setName(spriteName);
                        spriteMap.put(spriteName, sprite);
                    } catch(Exception exception) {
                        LogManager.logException("Failed to load sprite \"" + spriteName + "\"", exception);
                    }
                }
            }
        });
    }

    public static StarLoaderTexture getTexture(String name) {
        return textureMap.get(name);
    }

    public static Sprite getSprite(String name) {
        return spriteMap.get(name);
    }
}