package luamade.manager;

import luamade.LuaMade;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.resource.ResourceLoader;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector4f;

public class ResourceManager {

	public static void loadResources(ResourceLoader loader) {
		loadMesh(loader, "Computer");
	}

	private static void loadMesh(ResourceLoader loader, String path) {
		try {
			loader.getMeshLoader().loadModMesh(LuaMade.getInstance(), path, LuaMade.getInstance().getJarResource("models/" + path + ".zip"), null);
			Mesh mesh = loader.getMeshLoader().getModMesh(LuaMade.getInstance(), path);
			if(mesh == null) {
				LuaMade.getInstance().logException("Mesh loaded but getModMesh returned null for: " + path, new NullPointerException());
				return;
			}
			Quat4f q = eulerQuat(0, -270.0f, 0);
			Vector4f rotVec = new Vector4f(q.x, q.y, q.z, q.w);
			mesh.getChilds().get(0).setInitialQuadRot(rotVec);
			LuaMade.getInstance().logDebug("Loaded mesh '" + path + "': children=" + mesh.getChilds().size());
		} catch(Exception exception) {
			LuaMade.getInstance().logException("Failed to load mesh: " + path, exception);
		}
	}

	private static Quat4f eulerQuat(float degX, float degY, float degZ) {
		// ZYX order: R = Rz * Ry * Rx
		Matrix3f mx = new Matrix3f(), my = new Matrix3f(), mz = new Matrix3f();
		mx.rotX((float) Math.toRadians(degX));
		my.rotY((float) Math.toRadians(degY));
		mz.rotZ((float) Math.toRadians(degZ));
		Matrix3f combined = new Matrix3f();
		combined.mul(mz, my);
		combined.mul(mx);
		Quat4f q = new Quat4f();
		q.set(combined);
		return q;
	}
}
