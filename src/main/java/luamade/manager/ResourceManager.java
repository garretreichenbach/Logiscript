package luamade.manager;

import api.utils.textures.StarLoaderTexture;
import luamade.LuaMade;
import org.schema.schine.graphicsengine.core.ResourceException;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.resource.ResourceLoader;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.HashMap;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @since 06/15/2021
 */
public class ResourceManager {

	private static final String[] textureNames = {
	};

	private static final String[] spriteNames = {
	};

	private static final String[] modelNames = {

	};

	private static final HashMap<String, StarLoaderTexture> textureMap = new HashMap<>();
	private static final HashMap<String, Sprite> spriteMap = new HashMap<>();
	private static final HashMap<String, Mesh> meshMap = new HashMap<>();

	public static void loadResources(final LuaMade instance, final ResourceLoader loader) {
		StarLoaderTexture.runOnGraphicsThread(
				new Runnable() {
					@Override
					public void run() {
						// Load Textures
						for (String textureName : textureNames) {
							try {
								if (textureName.endsWith("icon")) textureMap.put(textureName, StarLoaderTexture.newIconTexture(ImageIO.read(instance.getJarResource("textures/" + textureName + ".png"))));
								else textureMap.put(textureName, StarLoaderTexture.newBlockTexture(ImageIO.read(instance.getJarResource("textures/" + textureName + ".png"))));
							} catch (Exception exception) {
								exception.printStackTrace();
							}
						}

						// Load Sprites
						for (String spriteName : spriteNames) {
							try {
								Sprite sprite = StarLoaderTexture.newSprite(ImageIO.read(instance.getJarResource("sprites/" + spriteName + ".png")), instance, spriteName);
								sprite.setPositionCenter(false);
								sprite.setName(spriteName);
								spriteMap.put(spriteName, sprite);
							} catch (Exception exception) {
								exception.printStackTrace();
							}
						}

						// Load models
						for (String modelName : modelNames) {
							try {
								loader.getMeshLoader().loadModMesh(instance, modelName, instance.getJarResource("models/" + modelName + ".zip"), null);
								Mesh mesh = loader.getMeshLoader().getModMesh(instance, modelName);
								mesh.setFirstDraw(true);
								meshMap.put(modelName, mesh);
							} catch (ResourceException | IOException exception) {
								exception.printStackTrace();
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

	public static Mesh getMesh(String name) {
		if (meshMap.containsKey(name)) return (Mesh) meshMap.get(name).getChilds().get(0);
		else return null;
	}
}
