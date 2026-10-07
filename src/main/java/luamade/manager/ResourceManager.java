package luamade.manager;

import api.render.texture.StarLoaderTexture;
import luamade.LuaMade;
import org.schema.game.common.data.element.Element;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.schine.graphicsengine.forms.Mesh;
import org.schema.schine.graphicsengine.shader.Shader;
import org.schema.schine.graphicsengine.texture.TGALoader;
import org.schema.schine.resource.ResourceLoader;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class ResourceManager {

	/** Atlases and icon sheets are a 16x16 grid; index = row * 16 + column. */
	private static final int ATLAS_GRID = 16;

	private static final HashMap<Integer, StarLoaderTexture> textures = new HashMap<>();
	private static final HashMap<String, Mesh> models = new HashMap<>();
	private static final HashMap<Integer, StarLoaderTexture> itemIcons = new HashMap<>();
	private static final HashMap<Integer, StarLoaderTexture> blockIcons = new HashMap<>();
	private static final HashMap<String, Shader> shaderMap = new HashMap<>();

	/** Plain vanilla computer casing (t001), shared by our blocks' undecorated faces instead of a copy in our atlas. */
	private static final int VANILLA_CASING = vanilla(377);

	/**
	 * Block face layout. Faces are listed in vanilla order — 6 sides: front, back, top, bottom, right, left;
	 * 3 sides: top, bottom, sides; 1: all faces — as textures_0 slots, or {@link #vanilla} texture ids. Our slots are
	 * packed back to back with no empty slots, matching the layer layout in textures.kra.
	 */
	public enum Textures {
		DISK_DRIVE(0, 1, 2, VANILLA_CASING, 3, 4),
		DATA_STORE(5, VANILLA_CASING, 6),
		NETWORKED_DATA_STORE(7, VANILLA_CASING, 8),
		PASSWORD_PERMISSION_MODULE(9),
		VAULT(10, 11, 12, VANILLA_CASING, 11, 11);

		private final int[] faces;

		Textures(int... faces) {
			this.faces = faces;
		}

		/**
		 * Points the block's faces at this entry's textures. Returns false and leaves the block untouched when our
		 * tiles aren't loaded (dedicated server, or not painted yet) so callers can keep a vanilla fallback.
		 */
		public boolean apply(ElementInformation info) {
			short[] ids = new short[6];
			for(int side = 0; side < 6; side++) {
				int face = switch(faces.length) {
					case 6 -> faces[side];
					case 3 -> faces[side == Element.TOP ? 0 : side == Element.BOTTOM ? 1 : 2];
					default -> faces[0];
				};
				ids[side] = face < 0 ? (short) (-1 - face) : getTextureID(face);
				if(ids[side] == 0) {
					return false;
				}
			}
			info.setIndividualSides(faces.length);
			info.setTextureId(ids);
			return true;
		}
	}

	/** Encodes a vanilla block texture id for use in {@link Textures} face lists. */
	private static int vanilla(int textureId) {
		return -1 - textureId;
	}

	public enum Models {
		COMPUTER("Computer", new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, -270.0f, 0.0f)),
		// face-mounted panels, authored against the cell's +Z face like vanilla's Small Button
		MODEM("Modem", new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f)),
		REMOTE_ACCESS_POINT("RemoteAccessPoint", new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f)),
		PROJECTOR("Projector", new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f));

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

	/** Item icons in icons/items_0.png (64px tiles), in slot order, packed with no empty slots. */
	public enum ItemIcons {
		HOLO_DISK;

		public boolean apply(ElementInformation info) {
			return applyIcon(itemIcons, ordinal(), info);
		}
	}

	/**
	 * Block icons in icons/blocks_0.png, rendered by scripts/render_icons.py (whose LUAMADE table must keep this
	 * order), packed with no empty slots.
	 */
	public enum BlockIcons {
		COMPUTER, MODEM, DISK_DRIVE, REMOTE_ACCESS_POINT, DATA_STORE, NETWORKED_DATA_STORE, PASSWORD_PERMISSION_MODULE, VAULT, PROJECTOR;

		public boolean apply(ElementInformation info) {
			return applyIcon(blockIcons, ordinal(), info);
		}
	}

	/**
	 * Sets the element's icon. Returns false and leaves it untouched when the sheet isn't loaded (dedicated server)
	 * so callers can keep a vanilla fallback.
	 */
	private static boolean applyIcon(HashMap<Integer, StarLoaderTexture> sheet, int slot, ElementInformation info) {
		StarLoaderTexture t = sheet.get(slot);
		if(t == null) {
			return false;
		}
		info.setBuildIconNum(t.getTextureId());
		return true;
	}

	public static void loadResources(LuaMade instance, ResourceLoader loader) {
		loadAtlas(instance, "textures_0");
		loadIcons(instance, "items_0", itemIcons);
		loadIcons(instance, "blocks_0", blockIcons);
		loadModels(loader);
	}

	/**
	 * Slices a 16x16-tile atlas ({@code textures/<name>.png}) into block textures, same layout as vanilla's
	 * {@code data/textures/block/Default/256/t00X.png} so tiles can be kitbashed across 1:1. An optional normal atlas
	 * ({@code <name>_NRM.png} or {@code <name>_NRM.tga}) is sliced alongside. Tile size is derived from the image
	 * width. Fully transparent tiles are skipped so empty slots don't burn global custom texture ids.
	 */
	private static void loadAtlas(LuaMade instance, String atlasName) {
		try {
			InputStream atlasStream = instance.getSkeleton().getJarResourceStream("textures/" + atlasName + ".png");
			if(atlasStream == null) {
				instance.logWarning("Texture atlas not found: textures/" + atlasName + ".png");
				return;
			}
			BufferedImage atlas = ImageIO.read(atlasStream);
			BufferedImage atlasNRM = null;
			InputStream nrmStream = instance.getSkeleton().getJarResourceStream("textures/" + atlasName + "_NRM.png");
			if(nrmStream != null) {
				atlasNRM = ImageIO.read(nrmStream);
			} else if((nrmStream = instance.getSkeleton().getJarResourceStream("textures/" + atlasName + "_NRM.tga")) != null) {
				ByteBuffer nrmBuffer = TGALoader.loadImage(nrmStream);
				atlasNRM = TGALoader.convertByteBufferToImage(nrmBuffer, TGALoader.getLastWidth(), TGALoader.getLastHeight(), true);
			}
			int tile = atlas.getWidth() / ATLAS_GRID;
			for(int i = 0; i < ATLAS_GRID * ATLAS_GRID; i++) {
				int x = (i % ATLAS_GRID) * tile;
				int y = (i / ATLAS_GRID) * tile;
				BufferedImage texture = atlas.getSubimage(x, y, tile, tile);
				if(isBlank(texture)) {
					continue;
				}
				StarLoaderTexture starLoaderTexture = atlasNRM != null
						? StarLoaderTexture.newBlockTexture(texture, atlasNRM.getSubimage(x, y, tile, tile))
						: StarLoaderTexture.newBlockTexture(texture);
				textures.put(i, starLoaderTexture);
			}
			instance.logDebug("Loaded " + textures.size() + " block textures from atlas " + atlasName);
		} catch(Exception exception) {
			instance.logException("Failed to load atlas " + atlasName, exception);
		}
	}

	private static void loadIcons(LuaMade instance, String sheetName, HashMap<Integer, StarLoaderTexture> icons) {
		try {
			InputStream sheetStream = instance.getSkeleton().getJarResourceStream("icons/" + sheetName + ".png");
			if(sheetStream == null) {
				instance.logWarning("Icon sheet not found: icons/" + sheetName + ".png");
				return;
			}
			BufferedImage sheet = ImageIO.read(sheetStream);
			int tile = sheet.getWidth() / ATLAS_GRID;
			for(int i = 0; i < ATLAS_GRID * ATLAS_GRID; i++) {
				BufferedImage icon = sheet.getSubimage((i % ATLAS_GRID) * tile, (i / ATLAS_GRID) * tile, tile, tile);
				if(isBlank(icon)) {
					continue;
				}
				icons.put(i, StarLoaderTexture.newIconTexture(icon));
			}
		} catch(Exception exception) {
			instance.logException("Failed to load icons " + sheetName, exception);
		}
	}

	private static boolean isBlank(BufferedImage image) {
		for(int y = 0; y < image.getHeight(); y++) {
			for(int x = 0; x < image.getWidth(); x++) {
				if((image.getRGB(x, y) >>> 24) != 0) {
					return false;
				}
			}
		}
		return true;
	}

	private static void loadModels(ResourceLoader loader) {
		for(Models model : Models.values()) {
			try {
				loader.getMeshLoader().loadModMesh(LuaMade.getInstance(), model.getName(), LuaMade.getInstance().getJarResource("models/" + model.getName() + ".zip"), null);
				Mesh mesh = loader.getMeshLoader().getModMesh(LuaMade.getInstance(), model.getName());
				if(mesh == null) {
					LuaMade.getInstance().logException("Mesh loaded but getModMesh returned null for: " + model.getName(), new NullPointerException());
					continue;
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
