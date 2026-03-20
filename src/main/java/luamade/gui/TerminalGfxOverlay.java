package luamade.gui;

import luamade.lua.gfx.GfxApi;
import org.lwjgl.opengl.GL11;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.gui.GUIDrawToTextureOverlay;
import org.schema.schine.network.client.ClientState;
import org.schema.schine.network.client.ClientStateInterface;

/**
 * Draws the Lua gfx command buffer onto a texture-backed GUI overlay.
 */
public class TerminalGfxOverlay extends GUIDrawToTextureOverlay {

	private final GfxApi gfxApi;
	private int lastTextureId = -1;
	private boolean canvasEnabled = true;
	private int canvasWidth;
	private int canvasHeight;

	public TerminalGfxOverlay(int width, int height, ClientState state, GfxApi gfxApi) {
		super(Math.max(1, width), Math.max(1, height), state);
		canvasWidth = Math.max(1, width);
		canvasHeight = Math.max(1, height);
		this.gfxApi = gfxApi;
	}

	public void setCanvasBounds(float x, float y, int width, int height) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		setPos(x, y, 0.0F);
		canvasWidth = safeWidth;
		canvasHeight = safeHeight;
		texWidth = safeWidth;
		texHeight = safeHeight;
		if(sprite != null) {
			sprite.setWidth(safeWidth);
			sprite.setHeight(safeHeight);
		}
		if(gfxApi != null) {
			gfxApi.setCanvasSize(safeWidth, safeHeight);
		}
	}

	@Override
	public float getWidth() {
		return canvasWidth;
	}

	@Override
	public float getHeight() {
		return canvasHeight;
	}

	public void setCanvasEnabled(boolean enabled) {
		canvasEnabled = enabled;
		updateVisibility();
	}

	@Override
	public void onInit() {
		super.onInit();
		trackTextureId();
	}

	@Override
	public void draw() {
		updateVisibility();
		if(isInvisible()) {
			return;
		}

		GlUtil.glPushMatrix();
		try {
			transform();
			drawOverlayTexture((ClientStateInterface) getState());
		} finally {
			GlUtil.glDisable(GL11.GL_BLEND);
			GlUtil.glEnable(GL11.GL_TEXTURE_2D);
			GlUtil.glEnable(GL11.GL_LIGHTING);
			GlUtil.glEnable(GL11.GL_DEPTH_TEST);
			GlUtil.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
			GlUtil.glPopMatrix();
		}
	}

	@Override
	public void updateGUI(ClientStateInterface state) {
		updateVisibility();
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

		GlUtil.glDisable(GL11.GL_TEXTURE_2D);
		GlUtil.glDisable(GL11.GL_LIGHTING);
		GlUtil.glDisable(GL11.GL_DEPTH_TEST);
		GlUtil.glEnable(GL11.GL_BLEND);
		GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		GlUtil.glColor4f(0.0f, 0.0f, 0.0f, 1.0f);
		GL11.glBegin(GL11.GL_QUADS);
		GL11.glVertex3f(0.0f, 0.0f, 0.0F);
		GL11.glVertex3f(frame.width, 0.0f, 0.0F);
		GL11.glVertex3f(frame.width, frame.height, 0.0F);
		GL11.glVertex3f(0.0f, frame.height, 0.0F);
		GL11.glEnd();

		for(GfxApi.LayerSnapshot layer : frame.layers) {
			if(!layer.visible || layer.commands.isEmpty()) {
				continue;
			}
			drawLayer(layer);
		}

		GlUtil.glDisable(GL11.GL_BLEND);
		GlUtil.glEnable(GL11.GL_TEXTURE_2D);
		GlUtil.glEnable(GL11.GL_LIGHTING);
		GlUtil.glEnable(GL11.GL_DEPTH_TEST);
		GlUtil.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
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

	private void updateVisibility() {
		boolean hasVisibleCommands = gfxApi != null && gfxApi.hasVisibleCommands();
		setInvisible(!canvasEnabled || !hasVisibleCommands);
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
