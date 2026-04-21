package luamade.manager;

import luamade.LuaMade;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.resource.ResourceLoader;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;

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
			// Rotate the parent MeshGroup's transform to correct orientation.
			// The renderer applies mesh.getParent().getTransform() during drawing (SegmentDrawer line 649).
			Quat4f correctionRot = new Quat4f();
			correctionRot.set(new AxisAngle4f(1, 0, 0, -(float)(Math.PI / 2)));
			Matrix3f rotMatrix = new Matrix3f();
			rotMatrix.set(correctionRot);
			mesh.getTransform().basis.set(rotMatrix);
			mesh.setFirstDraw(true);
			return mesh;
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to load mesh: " + path, exception);
			return null;
		}
	}
}
