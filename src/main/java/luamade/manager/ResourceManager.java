package luamade.manager;

import luamade.LuaMade;
import org.schema.schine.graphicsengine.forms.AbstractSceneNode;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.resource.ResourceLoader;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector4f;

public class ResourceManager {

	private static ResourceLoader resourceLoader;

	public static void loadResources(ResourceLoader loader) {
		resourceLoader = loader;
		// Load mesh directly - onResourceLoad is already called on the graphics thread
		// by SLModResourceLoader, so deferring via runOnGraphicsThread would delay loading
		// to the next frame and cause the mesh to be missing when blocks first render.
		loadMesh(loader, "Computer");
	}

	private static Mesh loadMesh(ResourceLoader loader, String path) {
		try {
			loader.getMeshLoader().loadModMesh(LuaMade.getInstance(), path, LuaMade.getInstance().getJarResource("models/" + path + ".zip"), null);
			Mesh mesh = loader.getMeshLoader().getModMesh(LuaMade.getInstance(), path);
			if(mesh == null) {
				LuaMade.getInstance().logException("Mesh loaded but getModMesh returned null for: " + path, new NullPointerException());
				return null;
			}
			LuaMade.getInstance().logDebug("Loaded mesh '" + path + "': children=" + mesh.getChilds().size());
			// Set rotation on child meshes' initialQuadRot (used by SegmentDrawer for placed blocks).
			// The parent transform is left as identity since the preview path already applies initialQuadRot.
			Quat4f correctionRot = new Quat4f();
			correctionRot.set(new AxisAngle4f(1, 0, 0, (float) Math.PI));
			Vector4f rotVec = new Vector4f(correctionRot.x, correctionRot.y, correctionRot.z, correctionRot.w);
			for(AbstractSceneNode child : mesh.getChilds()) {
				if(child instanceof Mesh) {
					child.setInitialQuadRot(rotVec);
				}
			}
			mesh.setFirstDraw(true);
			return mesh;
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to load mesh: " + path, exception);
			return null;
		}
	}
}
