package luamade.gui;

import luamade.lua.gfx.GfxApi;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.newdawn.slick.UnicodeFont;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIDrawToTextureOverlay;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.network.client.ClientState;
import org.schema.schine.network.client.ClientStateInterface;

import java.util.ArrayList;
import java.util.List;

/** Draws Lua gfx2d geometry onto a texture-backed overlay and text via GUITextOverlay instances. */
public class TerminalGfxOverlay extends GUIDrawToTextureOverlay {
	private final GfxApi gfxApi;
	private int lastTextureId = -1;
	private boolean canvasEnabled = true;
	private int canvasWidth;
	private int canvasHeight;
	private boolean textureResizePending;

	public TerminalGfxOverlay(int width, int height, ClientState state, GfxApi gfxApi) {
		super(Math.max(1, width), Math.max(1, height), state);
		canvasWidth = Math.max(1, width);
		canvasHeight = Math.max(1, height);
		this.gfxApi = gfxApi;
	}

	private static List<String> layoutTextLines(String text, int maxWidth, boolean wrap, UnicodeFont font) {
		List<String> lines = new ArrayList<>();
		String[] rawLines = text.split("\\n", -1);
		for(String raw : rawLines) {
			if(!wrap || maxWidth == Integer.MAX_VALUE || font.getWidth(raw) <= maxWidth) {
				lines.add(raw);
				continue;
			}
			lines.addAll(wrapLineByWidth(raw, maxWidth, font));
		}
		return lines;
	}

	private static List<String> wrapLineByWidth(String text, int maxWidth, UnicodeFont font) {
		List<String> wrapped = new ArrayList<>();
		if(text.isEmpty()) {
			wrapped.add("");
			return wrapped;
		}

		StringBuilder currentLine = new StringBuilder();
		for(int i = 0; i < text.length(); i++) {
			char character = text.charAt(i);
			currentLine.append(character);
			if(font.getWidth(currentLine.toString()) > maxWidth) {
				if(currentLine.length() == 1) {
					wrapped.add(currentLine.toString());
					currentLine.setLength(0);
				} else {
					char overflow = currentLine.charAt(currentLine.length() - 1);
					currentLine.setLength(currentLine.length() - 1);
					wrapped.add(currentLine.toString());
					currentLine.setLength(0);
					currentLine.append(overflow);
				}
			}
		}

		if(currentLine.length() > 0) {
			wrapped.add(currentLine.toString());
		}
		return wrapped;
	}

	private static FontLibrary.FontSize resolveFontSize(int textScale) {
		if(textScale <= 1) {
			return FontLibrary.FontSize.SMALL;
		}
		if(textScale <= 3) {
			return FontLibrary.FontSize.MEDIUM;
		}
		return FontLibrary.FontSize.BIG;
	}

	public void setCanvasBounds(float x, float y, int width, int height) {
		int safeWidth = Math.max(1, width);
		int safeHeight = Math.max(1, height);
		boolean sizeChanged = canvasWidth != safeWidth || canvasHeight != safeHeight;
		setPos(x, y, 0.0F);
		canvasWidth = safeWidth;
		canvasHeight = safeHeight;
		texWidth = safeWidth;
		texHeight = safeHeight;
		if(sizeChanged) {
			textureResizePending = true;
		}
		if(sprite != null) {
			sprite.setWidth(safeWidth);
			sprite.setHeight(safeHeight);
		}
		if(gfxApi != null) {
			gfxApi.setViewportSize(safeWidth, safeHeight);
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

	public boolean isOverlayActive() {
		return canvasEnabled && gfxApi != null && gfxApi.hasVisibleCommands();
	}

	@Override
	public void onInit() {
		if(sprite == null) {
			sprite = new Sprite(Math.max(1, canvasWidth), Math.max(1, canvasHeight));
		}
		releaseTrackedTexture();
		super.onInit();
		trackTextureId();
		textureResizePending = false;
	}

	@Override
	public void draw() {
		updateVisibility();
		if(isInvisible()) {
			return;
		}
		// Protect against OpenGL context loss (alt-tab, window focus loss, etc.)
		try {
			if(!canDeleteTextureNow()) {
				return;
			}
			ensureRenderResources();
			super.draw();
			drawOverlayTexture((ClientStateInterface) getState());
		} catch(Exception e) {
			// Log exception but don't crash - OpenGL context may be recovering
			try {
				releaseTrackedTexture();
			} catch(Exception ignored) {
			}
		}
	}

	@Override
	public void updateGUI(ClientStateInterface state) {
		updateVisibility();
		if(isInvisible()) {
			return;
		}
		// Protect against OpenGL context loss (alt-tab, window focus loss, etc.)
		try {
			if(!canDeleteTextureNow()) {
				return;
			}
			ensureRenderResources();
			super.updateGUI(state);
		} catch(Exception e) {
			// Log exception but don't crash - OpenGL context may be recovering
			try {
				releaseTrackedTexture();
			} catch(Exception ignored) {
			}
		}
	}

	@Override
	public void cleanUp() {
		releaseTrackedTexture();
		super.cleanUp();
	}

	private void drawCircle(GfxApi.DrawCommand command, float scaleX, float scaleY) {
		int segments = Math.max(8, command.segments);
		float cx = command.x1 * scaleX;
		float cy = command.y1 * scaleY;
		float radiusX = command.x2 * scaleX;
		float radiusY = command.x2 * scaleY;
		if(radiusX <= 0.0f || radiusY <= 0.0f) {
			return;
		}

		if(command.filled) {
			GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			GL11.glVertex3f(cx, cy, 0.0F);
			for(int i = 0; i <= segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				GL11.glVertex3f(cx + (float) Math.cos(angle) * radiusX, cy + (float) Math.sin(angle) * radiusY, 0.0F);
			}
			GL11.glEnd();
		} else {
			applyLineWidth(command.lineWidth * 0.5f * (scaleX + scaleY));
			GL11.glBegin(GL11.GL_LINE_LOOP);
			for(int i = 0; i < segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				GL11.glVertex3f(cx + (float) Math.cos(angle) * radiusX, cy + (float) Math.sin(angle) * radiusY, 0.0F);
			}
			GL11.glEnd();
			resetLineWidth();
		}
	}

	private void drawPolygon(GfxApi.DrawCommand command, float scaleX, float scaleY) {
		if(command.points == null || command.points.length < 6 || (command.points.length % 2) != 0) {
			return;
		}

		if(!command.filled) {
			applyLineWidth(command.lineWidth * 0.5f * (scaleX + scaleY));
		}

		GL11.glBegin(command.filled ? GL11.GL_POLYGON : GL11.GL_LINE_LOOP);
		for(int i = 0; i < command.points.length; i += 2) {
			GL11.glVertex3f(command.points[i] * scaleX, command.points[i + 1] * scaleY, 0.0F);
		}
		GL11.glEnd();

		if(!command.filled) {
			resetLineWidth();
		}
	}

	@Override
	public void drawOverlayTexture(ClientStateInterface state) {
		if(gfxApi == null) {
			return;
		}

		try {
			GfxApi.FrameSnapshot frame = gfxApi.snapshot();
			List<TextOverlaySpec> textOverlays = new ArrayList<>();

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
			GL11.glVertex3f(frame.viewportWidth, 0.0f, 0.0F);
			GL11.glVertex3f(frame.viewportWidth, frame.viewportHeight, 0.0F);
			GL11.glVertex3f(0.0f, frame.viewportHeight, 0.0F);
			GL11.glEnd();

			for(GfxApi.LayerSnapshot layer : frame.layers) {
				if(!layer.visible || layer.commands.isEmpty()) {
					continue;
				}
				drawLayer(layer, textOverlays, frame.scaleX, frame.scaleY);
			}

			GlUtil.glDisable(GL11.GL_BLEND);
			GlUtil.glEnable(GL11.GL_TEXTURE_2D);
			GlUtil.glEnable(GL11.GL_LIGHTING);
			GlUtil.glEnable(GL11.GL_DEPTH_TEST);
			GlUtil.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

			drawTextOverlays(textOverlays);
		} catch(Exception e) {
			// OpenGL context may be lost during alt-tab or window focus loss
			// Gracefully handle by releasing resources and returning
			try {
				releaseTrackedTexture();
			} catch(Exception ignored) {
			}
		}
	}

	private void drawBitmap(GfxApi.DrawCommand command, float scaleX, float scaleY) {
		if(command.bitmapPixels == null || command.bitmapWidth <= 0 || command.bitmapHeight <= 0) {
			return;
		}

		int expectedPixels = command.bitmapWidth * command.bitmapHeight;
		if(command.bitmapPixels.length < expectedPixels) {
			return;
		}

		GL11.glBegin(GL11.GL_QUADS);
		for(int y = 0; y < command.bitmapHeight; y++) {
			for(int x = 0; x < command.bitmapWidth; x++) {
				int packed = command.bitmapPixels[(y * command.bitmapWidth) + x];
				float red = ((packed >> 24) & 0xFF) / 255.0f;
				float green = ((packed >> 16) & 0xFF) / 255.0f;
				float blue = ((packed >> 8) & 0xFF) / 255.0f;
				float alpha = (packed & 0xFF) / 255.0f;
				if(alpha <= 0.0f) {
					continue;
				}

				GlUtil.glColor4f(red, green, blue, alpha);
				float px = command.x1 + x;
				float py = command.y1 + y;
				addQuad(px * scaleX, py * scaleY, (px + 1.0f) * scaleX, (py + 1.0f) * scaleY);
			}
		}
		GL11.glEnd();
	}

	private void drawLayer(GfxApi.LayerSnapshot layer, List<TextOverlaySpec> textOverlays, float scaleX, float scaleY) {
		try {
			for(GfxApi.DrawCommand command : layer.commands) {
				GlUtil.glColor4f(command.r, command.g, command.b, command.a);
				switch(command.kind) {
					case POINT:
						GL11.glBegin(GL11.GL_POINTS);
						GL11.glVertex3f(command.x1 * scaleX, command.y1 * scaleY, 0.0F);
						GL11.glEnd();
						break;
					case LINE:
						applyLineWidth(command.lineWidth * 0.5f * (scaleX + scaleY));
						GL11.glBegin(GL11.GL_LINES);
						GL11.glVertex3f(command.x1 * scaleX, command.y1 * scaleY, 0.0F);
						GL11.glVertex3f(command.x2 * scaleX, command.y2 * scaleY, 0.0F);
						GL11.glEnd();
						resetLineWidth();
						break;
					case RECT:
						if(command.filled) {
							GL11.glBegin(GL11.GL_QUADS);
							GL11.glVertex3f(command.x1 * scaleX, command.y1 * scaleY, 0.0F);
							GL11.glVertex3f(command.x2 * scaleX, command.y1 * scaleY, 0.0F);
							GL11.glVertex3f(command.x2 * scaleX, command.y2 * scaleY, 0.0F);
							GL11.glVertex3f(command.x1 * scaleX, command.y2 * scaleY, 0.0F);
							GL11.glEnd();
						} else {
							GL11.glBegin(GL11.GL_LINE_LOOP);
							GL11.glVertex3f(command.x1 * scaleX, command.y1 * scaleY, 0.0F);
							GL11.glVertex3f(command.x2 * scaleX, command.y1 * scaleY, 0.0F);
							GL11.glVertex3f(command.x2 * scaleX, command.y2 * scaleY, 0.0F);
							GL11.glVertex3f(command.x1 * scaleX, command.y2 * scaleY, 0.0F);
							GL11.glEnd();
						}
						break;
					case CIRCLE:
						drawCircle(command, scaleX, scaleY);
						break;
					case POLYGON:
						drawPolygon(command, scaleX, scaleY);
						break;
					case TEXT:
						collectTextOverlaySpecs(command, textOverlays, scaleX, scaleY);
						break;
					case BITMAP:
						drawBitmap(command, scaleX, scaleY);
						break;
				}
			}
		} catch(Exception e) {
			// OpenGL context may be lost during alt-tab, catch and continue
		}
	}

	private void collectTextOverlaySpecs(GfxApi.DrawCommand command, List<TextOverlaySpec> textOverlays, float scaleX, float scaleY) {
		if(command.text == null || command.text.isEmpty()) {
			return;
		}

		float fontScale = Math.max(0.5f, 0.5f * (scaleX + scaleY));
		int normalizedTextScale = Math.max(1, Math.round(command.textScale * fontScale));
		FontLibrary.FontSize fontSize = resolveFontSize(normalizedTextScale);
		UnicodeFont font = fontSize.getFont();
		int lineHeight = Math.max(1, font.getLineHeight());
		int maxWidth = command.textMaxWidth > 0 ? Math.max(1, Math.round(command.textMaxWidth * scaleX)) : Integer.MAX_VALUE;
		List<String> lines = layoutTextLines(command.text, maxWidth, command.textWrap, font);
		if(lines.isEmpty()) {
			return;
		}

		float textX = command.x1 * scaleX;
		float textY = command.y1 * scaleY;
		float clipY2 = command.textMaxHeight > 0 ? textY + (command.textMaxHeight * scaleY) : Float.POSITIVE_INFINITY;
		for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			float lineY = textY + (lineIndex * lineHeight);
			if(lineY >= clipY2) {
				break;
			}

			String line = lines.get(lineIndex);
			int textWidth = Math.max(1, font.getWidth(line));
			float lineX = textX;
			if(command.textMaxWidth > 0) {
				if("center".equals(command.textAlign)) {
					lineX = textX + ((maxWidth - textWidth) * 0.5f);
				} else if("right".equals(command.textAlign)) {
					lineX = textX + (maxWidth - textWidth);
				}
			}

			textOverlays.add(new TextOverlaySpec(line, lineX, lineY, command.r, command.g, command.b, command.a, Math.max(textWidth + 8, command.textMaxWidth > 0 ? maxWidth : textWidth + 8), lineHeight + 4, fontSize));
		}
	}

	private void drawTextOverlays(List<TextOverlaySpec> textOverlays) {
		if(textOverlays.isEmpty()) {
			return;
		}

		float overlayX = getPos().x;
		float overlayY = getPos().y;
		for(TextOverlaySpec spec : textOverlays) {
			GUITextOverlay textOverlay = new GUITextOverlay(Math.max(12, spec.width), Math.max(10, spec.height), spec.fontSize, getState());
			textOverlay.setTextSimple(spec.text);
			textOverlay.onInit();
			textOverlay.updateTextSize();
			textOverlay.setColor(spec.r, spec.g, spec.b, spec.a);
			textOverlay.setPos(overlayX + spec.x, overlayY + spec.y, 0.0F);
			textOverlay.draw();
			textOverlay.cleanUp();
		}
	}

	private void applyLineWidth(float width) {
		GL11.glLineWidth(Math.max(1.0f, width));
	}

	private void resetLineWidth() {
		GL11.glLineWidth(1.0f);
	}

	private void addQuad(float x1, float y1, float x2, float y2) {
		GL11.glVertex3f(x1, y1, 0.0F);
		GL11.glVertex3f(x2, y1, 0.0F);
		GL11.glVertex3f(x2, y2, 0.0F);
		GL11.glVertex3f(x1, y2, 0.0F);
	}

	private void updateVisibility() {
		boolean overlayActive = isOverlayActive();
		setInvisible(!overlayActive);
	}

	private void ensureRenderResources() {
		if(textureResizePending || sprite == null) {
			onInit();
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
			try {
				if(canDeleteTextureNow()) {
					releaseTexture(lastTextureId);
				}
			} catch(Exception ignored) {
				// OpenGL context may be lost, texture release can fail safely
			}
			lastTextureId = -1;
		}
	}

	private void releaseTexture(int textureId) {
		try {
			GL11.glDeleteTextures(textureId);
			if(Controller.loadedTextures != null) {
				Controller.loadedTextures.remove((Integer) textureId);
			}
		} catch(Exception ignored) {
			// OpenGL context may be lost, texture release can fail safely
		}
	}

	private boolean canDeleteTextureNow() {
		try {
			return Display.isCreated() && Display.isCurrent();
		} catch(Throwable ignored) {
			return false;
		}
	}

	private static final class TextOverlaySpec {
		private final String text;
		private final float x;
		private final float y;
		private final float r;
		private final float g;
		private final float b;
		private final float a;
		private final int width;
		private final int height;
		private final FontLibrary.FontSize fontSize;

		private TextOverlaySpec(String text, float x, float y, float r, float g, float b, float a, int width, int height, FontLibrary.FontSize fontSize) {
			this.text = text;
			this.x = x;
			this.y = y;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.width = width;
			this.height = height;
			this.fontSize = fontSize;
		}
	}
}
