package luamade.manager;

import api.utils.textures.StarLoaderTexture;
import luamade.LuaMade;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.resource.ResourceLoader;

public class ResourceManager {

	private static ResourceLoader resourceLoader;

	public static void loadResources(ResourceLoader loader) {
		resourceLoader = loader;
		StarLoaderTexture.runOnGraphicsThread(() -> loadMesh(loader, "Computer"));
	}

	private static Mesh loadMesh(ResourceLoader loader, String path) {
		try {
			loader.getMeshLoader().loadModMesh(LuaMade.getInstance(), path, LuaMade.getInstance().getJarResource("models/" + path + ".zip"), null);
			Mesh mesh = loader.getMeshLoader().getModMesh(LuaMade.getInstance(), path);
			mesh.setFirstDraw(true);
			return mesh;
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to load mesh: " + path, exception);
			return null;
		}
	}
}
