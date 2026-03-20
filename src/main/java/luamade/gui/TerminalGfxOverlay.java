package luamade.gui;

import luamade.lua.gfx.GfxApi;
import org.lwjgl.opengl.GL11;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.gui.GUIDrawToTextureOverlay;
import org.schema.schine.graphicsengine.texture.Texture;
import org.schema.schine.graphicsengine.texture.TextureLoader;
import org.schema.schine.network.client.ClientState;
import org.schema.schine.network.client.ClientStateInterface;

/**
 * Draws the Lua gfx command buffer onto a texture-backed GUI overlay.
 */
public class TerminalGfxOverlay extends GUIDrawToTextureOverlay {

	private final GfxApi gfxApi;
	private int lastTextureId = -1;
	private int requestedWidth;
	private int requestedHeight;

	public TerminalGfxOverlay(int width, int height, ClientState state, GfxApi gfxApi) {
		super(Math.max(1, width), Math.max(1, height), state);
		requestedWidth = Math.max(1, width);
		requestedHeight = Math.max(1, height);
		this.gfxApi = gfxApi;
	}

	public void setCanvasBounds(float x, float y, int width, int height) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		setPos(x, y, 0.0F);
		requestedWidth = safeWidth;
		requestedHeight = safeHeight;
		if(gfxApi != null) {
			gfxApi.setCanvasSize(safeWidth, safeHeight);
		}
	}

	@Override
	public void onInit() {
		super.onInit();
		trackTextureId();
	}

	@Override
	public void draw() {
		super.draw();
		drawOverlayTexture((ClientStateInterface) getState());
	}

	@Override
	public void updateGUI(ClientStateInterface state) {
		ensureTextureSize();
		super.updateGUI(state);
	}

	@Override
	public void cleanUp() {
		releaseTrackedTexture();
		super.cleanUp();
	}

	@Override
	public void drawOverlayTexture(ClientStateInterface state) {
		if(gfxApi == null) {
			return;
		}

		GfxApi.FrameSnapshot frame = gfxApi.snapshot();

		// Only render (and cover the terminal) when at least one visible layer has commands.
		boolean hasCommands = false;
		for(GfxApi.LayerSnapshot layer : frame.layers) {
			if(layer.visible && !layer.commands.isEmpty()) {
				hasCommands = true;
				break;
			}
		}
		if(!hasCommands) {
			return;
		}

		// Clear the FBO to opaque black so the canvas fully covers the terminal text.
		// Scripts can draw shapes with alpha < 1 on top for partial transparency effects,
		// or call gfx.clear() / remove all layers to restore terminal visibility entirely.
		GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

		GlUtil.glDisable(GL11.GL_TEXTURE_2D);
		GlUtil.glDisable(GL11.GL_LIGHTING);
		GlUtil.glDisable(GL11.GL_DEPTH_TEST);
		GlUtil.glEnable(GL11.GL_BLEND);
		GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		for(GfxApi.LayerSnapshot layer : frame.layers) {
			if(!layer.visible || layer.commands.isEmpty()) {
				continue;
			}
			drawLayer(layer);
		}

		GlUtil.glDisable(GL11.GL_BLEND);
		GlUtil.glEnable(GL11.GL_LIGHTING);
		GlUtil.glEnable(GL11.GL_DEPTH_TEST);
	}

	private void drawLayer(GfxApi.LayerSnapshot layer) {
		for(GfxApi.DrawCommand command : layer.commands) {
			GlUtil.glColor4f(command.r, command.g, command.b, command.a);
			switch(command.kind) {
				case POINT:
					GL11.glBegin(GL11.GL_POINTS);
					GL11.glVertex3f(command.x1, command.y1, 0.0F);
					GL11.glEnd();
					break;
				case LINE:
					GL11.glBegin(GL11.GL_LINES);
					GL11.glVertex3f(command.x1, command.y1, 0.0F);
					GL11.glVertex3f(command.x2, command.y2, 0.0F);
					GL11.glEnd();
					break;
				case RECT:
					if(command.filled) {
						GL11.glBegin(GL11.GL_QUADS);
						GL11.glVertex3f(command.x1, command.y1, 0.0F);
						GL11.glVertex3f(command.x2, command.y1, 0.0F);
						GL11.glVertex3f(command.x2, command.y2, 0.0F);
						GL11.glVertex3f(command.x1, command.y2, 0.0F);
						GL11.glEnd();
					} else {
						GL11.glBegin(GL11.GL_LINE_LOOP);
						GL11.glVertex3f(command.x1, command.y1, 0.0F);
						GL11.glVertex3f(command.x2, command.y1, 0.0F);
						GL11.glVertex3f(command.x2, command.y2, 0.0F);
						GL11.glVertex3f(command.x1, command.y2, 0.0F);
						GL11.glEnd();
					}
					break;
			}
		}
	}

	private void ensureTextureSize() {
		if(requestedWidth == texWidth && requestedHeight == texHeight) {
			return;
		}

		int previousTextureId = extractTextureId();
		texWidth = requestedWidth;
		texHeight = requestedHeight;

		Texture texture = TextureLoader.getEmptyTexture(texWidth, texHeight);
		sprite.getMaterial().setTexture(texture);
		sprite.setWidth(texWidth);
		sprite.setHeight(texHeight);
		trackTextureId();

		if(previousTextureId > 0 && previousTextureId != lastTextureId) {
			releaseTexture(previousTextureId);
		}
	}

	private void trackTextureId() {
		lastTextureId = extractTextureId();
	}

	private int extractTextureId() {
		if(sprite == null || sprite.getMaterial() == null || sprite.getMaterial().getTexture() == null) {
			return -1;
		}
		return sprite.getMaterial().getTexture().getTextureId();
	}

	private void releaseTrackedTexture() {
		if(lastTextureId > 0) {
			releaseTexture(lastTextureId);
			lastTextureId = -1;
		}
	}

	private void releaseTexture(int textureId) {
		GL11.glDeleteTextures(textureId);
		Controller.loadedTextures.remove((Integer) textureId);
	}
}
