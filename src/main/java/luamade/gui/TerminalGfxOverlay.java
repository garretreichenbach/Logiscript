package luamade.gui;

import luamade.lua.gfx.Gfx2d;
import org.lwjgl.opengl.GL11;
import org.schema.schine.graphicsengine.forms.font.unicode.UnicodeFont;
import org.schema.schine.graphicsengine.texture.Texture;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.Sprite;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIDrawToTextureOverlay;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.vbogui.GuiBatchRenderer;
import org.schema.schine.network.client.ClientState;
import org.schema.schine.network.client.ClientStateInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws gfx2d geometry streamed from the server onto a texture-backed overlay
 * and text via GUITextOverlay instances. The gfx2d buffer itself lives
 * server-side (scripts execute there); this renders whatever
 * {@link ComputerSessionView} last received via {@code PacketSCGfxSnapshot}.
 *
 * <p>Geometry goes through the engine's {@link GuiBatchRenderer}, never raw GL: on the Vulkan backend (no GL
 * context) or a core-profile context any {@code GL11} draw call aborts the JVM.
 */
public class TerminalGfxOverlay extends GUIDrawToTextureOverlay {
	private final ComputerSessionView sessionView;
	private Texture lastTexture;
	private boolean canvasEnabled = true;
	private int canvasWidth;
	private int canvasHeight;
	private boolean textureResizePending;

	public TerminalGfxOverlay(int width, int height, ClientState state, ComputerSessionView sessionView) {
		super(Math.max(1, width), Math.max(1, height), state);
		canvasWidth = Math.max(1, width);
		canvasHeight = Math.max(1, height);
		this.sessionView = sessionView;
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
		if(sizeChanged && sessionView != null) {
			sessionView.sendViewportResize(safeWidth, safeHeight);
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
		if(!canvasEnabled || sessionView == null) {
			return false;
		}
		Gfx2d.FrameSnapshot snapshot = sessionView.getGfxSnapshot();
		if(snapshot == null) {
			return false;
		}
		for(Gfx2d.LayerSnapshot layer : snapshot.layers) {
			if(layer.visible && !layer.commands.isEmpty()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onInit() {
		if(sprite == null) {
			sprite = new Sprite(Math.max(1, canvasWidth), Math.max(1, canvasHeight));
		}
		releaseTrackedTexture();
		super.onInit();
		trackTexture();
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
		// The base render-to-texture pass calls raw GL11.glCopyTexImage2D; draw() already renders
		// directly, so skip the pass where there is no GL context.
		if(isInvisible() || GlUtil.isVulkan()) {
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

	private void drawCircle(Gfx2d.DrawCommand command, float scaleX, float scaleY) {
		int segments = Math.max(8, command.segments);
		float cx = command.x1 * scaleX;
		float cy = command.y1 * scaleY;
		float radiusX = command.x2 * scaleX;
		float radiusY = command.x2 * scaleY;
		if(radiusX <= 0.0f || radiusY <= 0.0f) {
			return;
		}

		float[] xs = new float[segments];
		float[] ys = new float[segments];
		for(int i = 0; i < segments; i++) {
			double angle = (Math.PI * 2.0 * i) / segments;
			xs[i] = cx + (float) Math.cos(angle) * radiusX;
			ys[i] = cy + (float) Math.sin(angle) * radiusY;
		}
		drawShape(xs, ys, command.filled, lineWidth(command, scaleX, scaleY));
	}

	private void drawPolygon(Gfx2d.DrawCommand command, float scaleX, float scaleY) {
		if(command.points == null || command.points.length < 6 || (command.points.length % 2) != 0) {
			return;
		}

		int n = command.points.length / 2;
		float[] xs = new float[n];
		float[] ys = new float[n];
		for(int i = 0; i < n; i++) {
			xs[i] = command.points[i * 2] * scaleX;
			ys[i] = command.points[i * 2 + 1] * scaleY;
		}
		drawShape(xs, ys, command.filled, lineWidth(command, scaleX, scaleY));
	}

	/** Filled: fan from the first vertex (the same convex contract GL_POLYGON had). Outline: closed loop. */
	private void drawShape(float[] xs, float[] ys, boolean filled, float width) {
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		if(filled) {
			batch.polygon(xs, ys, xs.length);
			return;
		}
		for(int i = 0; i < xs.length; i++) {
			int j = (i + 1) % xs.length;
			batch.line(xs[i], ys[i], xs[j], ys[j], width);
		}
	}

	@Override
	public void drawOverlayTexture(ClientStateInterface state) {
		if(sessionView == null) {
			return;
		}
		Gfx2d.FrameSnapshot frame = sessionView.getGfxSnapshot();
		if(frame == null) {
			return;
		}

		try {
			List<TextOverlaySpec> textOverlays = new ArrayList<>();

			// Only render (and cover the terminal) when at least one visible layer has commands.
			boolean hasCommands = false;
			for(Gfx2d.LayerSnapshot layer : frame.layers) {
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

			GuiBatchRenderer batch = GuiBatchRenderer.get();
			batch.begin();
			try {
				batch.setColor(0.0f, 0.0f, 0.0f, 1.0f);
				batch.quadColored(0.0f, 0.0f, frame.viewportWidth, frame.viewportHeight);

				for(Gfx2d.LayerSnapshot layer : frame.layers) {
					if(!layer.visible || layer.commands.isEmpty()) {
						continue;
					}
					drawLayer(layer, textOverlays, frame.scaleX, frame.scaleY);
				}
			} finally {
				batch.end();
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

	private void drawBitmap(Gfx2d.DrawCommand command, float scaleX, float scaleY) {
		if(command.bitmapPixels == null || command.bitmapWidth <= 0 || command.bitmapHeight <= 0) {
			return;
		}

		int expectedPixels = command.bitmapWidth * command.bitmapHeight;
		if(command.bitmapPixels.length < expectedPixels) {
			return;
		}

		GuiBatchRenderer batch = GuiBatchRenderer.get();
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

				batch.setColor(red, green, blue, alpha);
				float px = command.x1 + x;
				float py = command.y1 + y;
				batch.quadColored(px * scaleX, py * scaleY, scaleX, scaleY);
			}
		}
	}

	private void drawLayer(Gfx2d.LayerSnapshot layer, List<TextOverlaySpec> textOverlays, float scaleX, float scaleY) {
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		try {
			for(Gfx2d.DrawCommand command : layer.commands) {
				batch.setColor(command.r, command.g, command.b, command.a);
				switch(command.kind) {
					case POINT:
						// 1px, as GL_POINTS drew at the default point size.
						batch.quadColored(command.x1 * scaleX - 0.5f, command.y1 * scaleY - 0.5f, 1.0f, 1.0f);
						break;
					case LINE:
						batch.line(command.x1 * scaleX, command.y1 * scaleY, command.x2 * scaleX, command.y2 * scaleY, lineWidth(command, scaleX, scaleY));
						break;
					case RECT: {
						float x1 = command.x1 * scaleX, y1 = command.y1 * scaleY;
						float x2 = command.x2 * scaleX, y2 = command.y2 * scaleY;
						if(command.filled) {
							batch.polygon(new float[] {x1, x2, x2, x1}, new float[] {y1, y1, y2, y2}, 4);
						} else {
							// Outline stayed at the default 1px line width before; keep it.
							drawShape(new float[] {x1, x2, x2, x1}, new float[] {y1, y1, y2, y2}, false, 1.0f);
						}
						break;
					}
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

	private void collectTextOverlaySpecs(Gfx2d.DrawCommand command, List<TextOverlaySpec> textOverlays, float scaleX, float scaleY) {
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

	private static float lineWidth(Gfx2d.DrawCommand command, float scaleX, float scaleY) {
		return Math.max(1.0f, command.lineWidth * 0.5f * (scaleX + scaleY));
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

	private void trackTexture() {
		lastTexture = (sprite == null || sprite.getMaterial() == null) ? null : sprite.getMaterial().getTexture();
	}

	private void releaseTrackedTexture() {
		if(lastTexture != null) {
			try {
				if(canDeleteTextureNow()) {
					lastTexture.cleanUp();
				}
			} catch(Exception ignored) {
				// OpenGL context may be lost, texture release can fail safely
			}
			lastTexture = null;
		}
	}

	private boolean canDeleteTextureNow() {
		if(GlUtil.isVulkan()) {
			return true; // no GL context to lose; GLFW reports none current
		}
		try {
			return org.lwjgl.glfw.GLFW.glfwGetCurrentContext() != 0L;
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
