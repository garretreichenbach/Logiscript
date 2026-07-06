package luamade.manager;

import api.utils.textures.StarLoaderTexture;
import luamade.LuaMade;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.graphicsengine.shader.Shader;
import org.schema.schine.graphicsengine.texture.TGALoader;
import org.schema.schine.resource.ResourceLoader;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ResourceManager {

	private static final int TEXTURE_ATLAS_SIZE = 4096;
	private static final int TEXTURES_PER_ATLAS = 16 * 16; // 256 textures per atlas
	private static final int ICON_ATLAS_SIZE = 1024;
	private static final int ICONS_PER_ATLAS = 16 * 16; // 256 sprites per atlas

	private static final HashMap<Integer, StarLoaderTexture> textures = new HashMap<>();
	private static final HashMap<String, Mesh> models = new HashMap<>();
	private static final HashMap<Integer, StarLoaderTexture> icons = new HashMap<>();
	private static final HashMap<String, Shader> shaderMap = new HashMap<>();

	public enum Textures {
		DISK_DRIVE_FRONT(0);

		private final int index;

		Textures(int index) {
			this.index = index;
		}

		public StarLoaderTexture getTexture() {
			return textures.get(index);
		}

		public short getTextureID() {
			StarLoaderTexture t = getTexture();
			if(t == null) {
				return 0;
			}
			return (short) t.getTextureId();
		}
	}

	public enum Models {
		COMPUTER("Computer", new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, -270.0f, 0.0f));

		private final String name;
		private final Vector3f offset;
		private final Vector3f rotation;

		Models(String name, Vector3f offset, Vector3f rotation) {
			this.name = name;
			this.offset = offset;
			this.rotation = rotation;
		}

		public String getName() {
			return name;
		}

		public Vector3f getOffset() {
			return offset;
		}

		public Vector3f getRotation() {
			return rotation;
		}
	}

	public enum Icons {
		COMPUTER_ICON,
		DISK_DRIVE_ICON;

		public StarLoaderTexture getIcon() {
			return icons.get(ordinal());
		}

		public short getIconID() {
			StarLoaderTexture t = getIcon();
			if(t == null) {
				return 0;
			}
			return (short) t.getTextureId();
		}
	}

	public static void loadResources(LuaMade instance, ResourceLoader loader) {
		loadAtlas(instance, "textures_0");
		loadIcons(instance, "icons_0");
		loadModels(loader);
	}

	private static void loadAtlas(LuaMade instance, String atlasName) {
		try {
			BufferedImage atlas0 = instance.getJarBufferedImage("textures/" + atlasName + ".png");
			InputStream atlas0NRMStream = instance.getJarResource("textures/" + atlasName + "_NRM.tga");
			ByteBuffer atlas0NRMBuffer = TGALoader.loadImage(atlas0NRMStream);
			BufferedImage atlas0NRM = TGALoader.convertByteBufferToImage(atlas0NRMBuffer, TGALoader.getLastWidth(), TGALoader.getLastHeight(), true);
			for(int i = 0; i < TEXTURES_PER_ATLAS; i++) {
				int x = (i % 16) * (TEXTURE_ATLAS_SIZE / 16);
				int y = (i / 16) * (TEXTURE_ATLAS_SIZE / 16);
				BufferedImage texture = atlas0.getSubimage(x, y, TEXTURE_ATLAS_SIZE / 16, TEXTURE_ATLAS_SIZE / 16);
				BufferedImage textureNRM = atlas0NRM.getSubimage(x, y, TEXTURE_ATLAS_SIZE / 16, TEXTURE_ATLAS_SIZE / 16);
				StarLoaderTexture starLoaderTexture = StarLoaderTexture.newBlockTexture(texture, textureNRM);
				textures.put(i, starLoaderTexture);
			}
		} catch(Exception exception) {
			instance.logException("Failed to load atlas " + atlasName, exception);
		}
	}

	private static void loadIcons(LuaMade instance, String sheetName) {
		try {
			BufferedImage icons0 = instance.getJarBufferedImage("icons/" + sheetName + ".png");
			for(int i = 0; i < ICONS_PER_ATLAS; i++) {
				int x = (i % 16) * (ICON_ATLAS_SIZE / 16);
				int y = (i / 16) * (ICON_ATLAS_SIZE / 16);
				BufferedImage icon = icons0.getSubimage(x, y, ICON_ATLAS_SIZE / 16, ICON_ATLAS_SIZE / 16);
				StarLoaderTexture starLoaderTexture = StarLoaderTexture.newIconTexture(icon);
				icons.put(i, starLoaderTexture);
			}
		} catch(Exception exception) {
			instance.logException("Failed to load icons " + sheetName, exception);
		}
	}

	private static void loadModels(ResourceLoader loader) {
		for(Models model : Models.values()) {
			try {
				loader.getMeshLoader().loadModMesh(LuaMade.getInstance(), model.getName(), LuaMade.getInstance().getJarResource("models/" + model.getName() + ".zip"), null);
				Mesh mesh = loader.getMeshLoader().getModMesh(LuaMade.getInstance(), model.getName());
				if(mesh == null) {
					LuaMade.getInstance().logException("Mesh loaded but getModMesh returned null for: " + model.getName(), new NullPointerException());
					return;
				}
				Quat4f q = eulerQuat(model.getRotation().x, model.getRotation().y, model.getRotation().z);
				Vector4f rotVec = new Vector4f(q.x, q.y, q.z, q.w);
				mesh.getChilds().getFirst().setInitialQuadRot(rotVec);
				mesh.getChilds().getFirst().setInitionPos(model.getOffset());
				models.put(mesh.getName(), mesh);
			} catch(Exception exception) {
				LuaMade.getInstance().logException("Failed to load mesh: " + model.getName(), exception);
			}
		}
	}

	private static void loadShader(LuaMade instance, String shaderName) {
		try {
			Shader shader = Shader.newModShader(instance.getSkeleton(), shaderName, instance.getClass().getResourceAsStream("/shaders/" + shaderName + ".vert"), instance.getClass().getResourceAsStream("/shaders/" + shaderName + ".frag"));
			shaderMap.put(shaderName, shader);
		} catch(Exception exception) {
			instance.logException("Failed to load shader: " + shaderName, exception);
		}
	}

	public static Shader getShader(String shaderName) {
		return shaderMap.get(shaderName);
	}

	public static short getTextureID(int index) {
		StarLoaderTexture t = textures.get(index);
		if(t == null) {
			return 0;
		}
		return (short) t.getTextureId();
	}

	private static Quat4f eulerQuat(float degX, float degY, float degZ) {
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
