package luamade.gui;

import luamade.lua.gfx.GfxApi;
import org.lwjgl.opengl.GL11;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.gui.GUIDrawToTextureOverlay;
import org.schema.schine.network.client.ClientState;
import org.schema.schine.network.client.ClientStateInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws the Lua gfx command buffer onto a texture-backed GUI overlay.
 */
public class TerminalGfxOverlay extends GUIDrawToTextureOverlay {
	private static final int FONT_GLYPH_WIDTH = 5;
	private static final int FONT_GLYPH_HEIGHT = 7;
	private static final int FONT_GLYPH_SPACING = 1;
	private static final Map<Character, String[]> TINY_FONT = buildTinyFont();

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
		super.draw();
		drawOverlayTexture((ClientStateInterface) getState());
	}

	@Override
	public void updateGUI(ClientStateInterface state) {
		updateVisibility();
		if(isInvisible()) {
			return;
		}
		if(textureResizePending) {
			onInit();
		}
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
					applyLineWidth(command.lineWidth);
					GL11.glBegin(GL11.GL_LINES);
					GL11.glVertex3f(command.x1, command.y1, 0.0F);
					GL11.glVertex3f(command.x2, command.y2, 0.0F);
					GL11.glEnd();
					resetLineWidth();
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
				case CIRCLE:
					drawCircle(command);
					break;
				case POLYGON:
					drawPolygon(command);
					break;
				case TEXT:
					drawText(command);
					break;
				case BITMAP:
					drawBitmap(command);
					break;
			}
		}
	}

	private void drawCircle(GfxApi.DrawCommand command) {
		int segments = Math.max(8, command.segments);
		float cx = command.x1;
		float cy = command.y1;
		float radius = command.x2;
		if(radius <= 0.0f) {
			return;
		}

		if(command.filled) {
			GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			GL11.glVertex3f(cx, cy, 0.0F);
			for(int i = 0; i <= segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				GL11.glVertex3f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0.0F);
			}
			GL11.glEnd();
		} else {
			applyLineWidth(command.lineWidth);
			GL11.glBegin(GL11.GL_LINE_LOOP);
			for(int i = 0; i < segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				GL11.glVertex3f(cx + (float) Math.cos(angle) * radius, cy + (float) Math.sin(angle) * radius, 0.0F);
			}
			GL11.glEnd();
			resetLineWidth();
		}
	}

	private void drawPolygon(GfxApi.DrawCommand command) {
		if(command.points == null || command.points.length < 6 || (command.points.length % 2) != 0) {
			return;
		}

		if(!command.filled) {
			applyLineWidth(command.lineWidth);
		}

		GL11.glBegin(command.filled ? GL11.GL_POLYGON : GL11.GL_LINE_LOOP);
		for(int i = 0; i < command.points.length; i += 2) {
			GL11.glVertex3f(command.points[i], command.points[i + 1], 0.0F);
		}
		GL11.glEnd();

		if(!command.filled) {
			resetLineWidth();
		}
	}

	private void drawText(GfxApi.DrawCommand command) {
		if(command.text == null || command.text.isEmpty()) {
			return;
		}

		float scale = Math.max(1, command.textScale);
		float charAdvance = (FONT_GLYPH_WIDTH + FONT_GLYPH_SPACING) * scale;
		float lineHeight = (FONT_GLYPH_HEIGHT + FONT_GLYPH_SPACING) * scale;
		float clipX1 = command.x1;
		float clipY1 = command.y1;
		float clipX2 = command.textMaxWidth > 0 ? command.x1 + command.textMaxWidth : Float.POSITIVE_INFINITY;
		float clipY2 = command.textMaxHeight > 0 ? command.y1 + command.textMaxHeight : Float.POSITIVE_INFINITY;

		int maxCharsPerLine = Integer.MAX_VALUE;
		if(command.textWrap && command.textMaxWidth > 0) {
			maxCharsPerLine = Math.max(1, (int) Math.floor(command.textMaxWidth / Math.max(1.0f, charAdvance)));
		}

		List<String> lines = layoutTextLines(command.text, maxCharsPerLine);
		if(lines.isEmpty()) {
			return;
		}

		GL11.glBegin(GL11.GL_QUADS);
		for(int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
			float lineY = command.y1 + (lineIndex * lineHeight);
			if(lineY >= clipY2) {
				break;
			}

			String line = lines.get(lineIndex);
			float lineWidth = line.length() * charAdvance;
			float lineX = command.x1;
			if(command.textMaxWidth > 0) {
				if("center".equals(command.textAlign)) {
					lineX = command.x1 + ((command.textMaxWidth - lineWidth) * 0.5f);
				} else if("right".equals(command.textAlign)) {
					lineX = command.x1 + (command.textMaxWidth - lineWidth);
				}
			}

			float cursorX = lineX;
			for(int i = 0; i < line.length(); i++) {
				char ch = line.charAt(i);
				String[] glyph = lookupGlyph(ch);
				if(glyph != null) {
					drawGlyphQuadBatch(cursorX, lineY, scale, glyph, clipX1, clipY1, clipX2, clipY2);
				}
				cursorX += charAdvance;
			}
		}
		GL11.glEnd();
	}

	private void drawBitmap(GfxApi.DrawCommand command) {
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
				addQuad(px, py, px + 1.0f, py + 1.0f);
			}
		}
		GL11.glEnd();
	}

	private static String[] lookupGlyph(char ch) {
		char normalized = Character.toUpperCase(ch);
		String[] glyph = TINY_FONT.get(normalized);
		if(glyph != null) {
			return glyph;
		}
		return TINY_FONT.get('?');
	}

	private void drawGlyphQuadBatch(float x, float y, float scale, String[] glyph, float clipX1, float clipY1, float clipX2, float clipY2) {
		for(int row = 0; row < glyph.length; row++) {
			String rowBits = glyph[row];
			for(int col = 0; col < rowBits.length(); col++) {
				if(rowBits.charAt(col) != '1') {
					continue;
				}

				float x1 = x + (col * scale);
				float y1 = y + (row * scale);
				addClippedQuad(x1, y1, x1 + scale, y1 + scale, clipX1, clipY1, clipX2, clipY2);
			}
		}
	}

	private void addClippedQuad(float x1, float y1, float x2, float y2, float clipX1, float clipY1, float clipX2, float clipY2) {
		float cx1 = Math.max(x1, clipX1);
		float cy1 = Math.max(y1, clipY1);
		float cx2 = Math.min(x2, clipX2);
		float cy2 = Math.min(y2, clipY2);
		if(cx2 <= cx1 || cy2 <= cy1) {
			return;
		}
		addQuad(cx1, cy1, cx2, cy2);
	}

	private static List<String> layoutTextLines(String text, int maxCharsPerLine) {
		List<String> lines = new ArrayList<>();
		String[] rawLines = text.split("\\n", -1);
		for(String raw : rawLines) {
			if(maxCharsPerLine == Integer.MAX_VALUE || raw.length() <= maxCharsPerLine) {
				lines.add(raw);
				continue;
			}

			int index = 0;
			while(index < raw.length()) {
				int end = Math.min(raw.length(), index + maxCharsPerLine);
				lines.add(raw.substring(index, end));
				index = end;
			}
		}
		return lines;
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

	private static Map<Character, String[]> buildTinyFont() {
		Map<Character, String[]> font = new HashMap<>();
		font.put(' ', glyph("00000", "00000", "00000", "00000", "00000", "00000", "00000"));
		font.put('?', glyph("01110", "10001", "00010", "00100", "00100", "00000", "00100"));
		font.put('.', glyph("00000", "00000", "00000", "00000", "00000", "00100", "00100"));
		font.put('!', glyph("00100", "00100", "00100", "00100", "00100", "00000", "00100"));
		font.put(':', glyph("00000", "00100", "00100", "00000", "00100", "00100", "00000"));
		font.put('-', glyph("00000", "00000", "00000", "01110", "00000", "00000", "00000"));
		font.put('+', glyph("00000", "00100", "00100", "11111", "00100", "00100", "00000"));
		font.put('/', glyph("00001", "00010", "00100", "01000", "10000", "00000", "00000"));
		font.put('0', glyph("01110", "10001", "10011", "10101", "11001", "10001", "01110"));
		font.put('1', glyph("00100", "01100", "00100", "00100", "00100", "00100", "01110"));
		font.put('2', glyph("01110", "10001", "00001", "00010", "00100", "01000", "11111"));
		font.put('3', glyph("11110", "00001", "00001", "01110", "00001", "00001", "11110"));
		font.put('4', glyph("00010", "00110", "01010", "10010", "11111", "00010", "00010"));
		font.put('5', glyph("11111", "10000", "10000", "11110", "00001", "00001", "11110"));
		font.put('6', glyph("00110", "01000", "10000", "11110", "10001", "10001", "01110"));
		font.put('7', glyph("11111", "00001", "00010", "00100", "01000", "01000", "01000"));
		font.put('8', glyph("01110", "10001", "10001", "01110", "10001", "10001", "01110"));
		font.put('9', glyph("01110", "10001", "10001", "01111", "00001", "00010", "11100"));
		font.put('A', glyph("01110", "10001", "10001", "11111", "10001", "10001", "10001"));
		font.put('B', glyph("11110", "10001", "10001", "11110", "10001", "10001", "11110"));
		font.put('C', glyph("01110", "10001", "10000", "10000", "10000", "10001", "01110"));
		font.put('D', glyph("11110", "10001", "10001", "10001", "10001", "10001", "11110"));
		font.put('E', glyph("11111", "10000", "10000", "11110", "10000", "10000", "11111"));
		font.put('F', glyph("11111", "10000", "10000", "11110", "10000", "10000", "10000"));
		font.put('G', glyph("01110", "10001", "10000", "10000", "10011", "10001", "01110"));
		font.put('H', glyph("10001", "10001", "10001", "11111", "10001", "10001", "10001"));
		font.put('I', glyph("01110", "00100", "00100", "00100", "00100", "00100", "01110"));
		font.put('J', glyph("00001", "00001", "00001", "00001", "10001", "10001", "01110"));
		font.put('K', glyph("10001", "10010", "10100", "11000", "10100", "10010", "10001"));
		font.put('L', glyph("10000", "10000", "10000", "10000", "10000", "10000", "11111"));
		font.put('M', glyph("10001", "11011", "10101", "10101", "10001", "10001", "10001"));
		font.put('N', glyph("10001", "11001", "10101", "10011", "10001", "10001", "10001"));
		font.put('O', glyph("01110", "10001", "10001", "10001", "10001", "10001", "01110"));
		font.put('P', glyph("11110", "10001", "10001", "11110", "10000", "10000", "10000"));
		font.put('Q', glyph("01110", "10001", "10001", "10001", "10101", "10010", "01101"));
		font.put('R', glyph("11110", "10001", "10001", "11110", "10100", "10010", "10001"));
		font.put('S', glyph("01111", "10000", "10000", "01110", "00001", "00001", "11110"));
		font.put('T', glyph("11111", "00100", "00100", "00100", "00100", "00100", "00100"));
		font.put('U', glyph("10001", "10001", "10001", "10001", "10001", "10001", "01110"));
		font.put('V', glyph("10001", "10001", "10001", "10001", "10001", "01010", "00100"));
		font.put('W', glyph("10001", "10001", "10001", "10101", "10101", "10101", "01010"));
		font.put('X', glyph("10001", "10001", "01010", "00100", "01010", "10001", "10001"));
		font.put('Y', glyph("10001", "10001", "01010", "00100", "00100", "00100", "00100"));
		font.put('Z', glyph("11111", "00001", "00010", "00100", "01000", "10000", "11111"));
		return font;
	}

	private static String[] glyph(String row1, String row2, String row3, String row4, String row5, String row6, String row7) {
		return new String[]{row1, row2, row3, row4, row5, row6, row7};
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
