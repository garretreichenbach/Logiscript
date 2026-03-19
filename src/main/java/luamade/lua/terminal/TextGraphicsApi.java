package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;

import java.util.*;

/**
 * Text-based graphics API for terminal scripts.
 * Coordinates are 1-based to be Lua-friendly.
 */
public class TextGraphicsApi extends LuaMadeUserdata {
	private static final int MIN_SIZE = 1;
	private static final int MAX_WIDTH = 240;
	private static final int MAX_HEIGHT = 120;
	private static final int ANSI_DEFAULT = -1;
	private static final String ANSI_ESCAPE = "\u001B[";
	private static final String ANSI_RESET = "\u001B[0m";
	private static final float MIN_CELL_SCALE = 0.5F;
	private static final float MAX_CELL_SCALE = 4.0F;
	private static final int MAX_CANVAS_LAYERS = 64;
	private Backend backend = Backend.CANVAS;

	private final Console console;
	private int width = 64;
	private int height = 24;
	private int fillCodePoint = ' ';
	private boolean ansiEnabled;
	private int brushFg = ANSI_DEFAULT;
	private int brushBg = ANSI_DEFAULT;
	private int[][] cells;
	private int[][] fgColors;
	private int[][] bgColors;
	private float[][] cellScaleXMap;
	private float[][] cellScaleYMap;
	private float cellScaleX = 1.0F;
	private float cellScaleY = 1.0F;
	/** Insertion-ordered named layers. Empty = single-layer (flat buffer) mode. */
	private final LinkedHashMap<String, CanvasLayer> explicitLayers = new LinkedHashMap<String, CanvasLayer>();
	/** Non-null when an explicit layer is active; null = flat buffer. */
	private CanvasLayer activeLayer;

	@LuaMadeCallable
	public void render() {
		Backend activeBackend = resolveActiveBackend();
		if(activeBackend == Backend.CANVAS) {
			console.setGraphicsFrame(new Console.GraphicsFrame(
					frame(),
					width,
					height,
					cellScaleX,
					cellScaleY,
					ansiEnabled,
					Console.GraphicsFrame.RenderBackend.CANVAS,
					flattenCodePoints(),
					flattenColors(fgColors),
					flattenColors(bgColors),
					createCanvasLayers()
			));
			return;
		}

		console.clearGraphicsFrame();
		console.setTextContents(frame());
	}

	public TextGraphicsApi(Console console) {
		this.console = console;
		cells = new int[height][width];
		fgColors = new int[height][width];
		bgColors = new int[height][width];
		cellScaleXMap = new float[height][width];
		cellScaleYMap = new float[height][width];
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT, cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public int getWidth() {
		return width;
	}

	@LuaMadeCallable
	public int getHeight() {
		return height;
	}

	@LuaMadeCallable
	public void setAnsiEnabled(boolean enabled) {
		ansiEnabled = enabled;
	}

	@LuaMadeCallable
	public boolean isAnsiEnabled() {
		return ansiEnabled;
	}

	@LuaMadeCallable
	public void setForeground(String color) {
		brushFg = parseColor(color);
	}

	@LuaMadeCallable
	public void setBackground(String color) {
		brushBg = parseColor(color);
	}

	@LuaMadeCallable
	public void setColor(String foreground) {
		brushFg = parseColor(foreground);
	}

	@LuaMadeCallable
	public void setColor(String foreground, String background) {
		brushFg = parseColor(foreground);
		brushBg = parseColor(background);
	}

	@LuaMadeCallable
	public void resetColor() {
		brushFg = ANSI_DEFAULT;
		brushBg = ANSI_DEFAULT;
	}

	@LuaMadeCallable
	public void setSize(int newWidth, int newHeight) {
		int clampedWidth = clamp(newWidth, MIN_SIZE, MAX_WIDTH);
		int clampedHeight = clamp(newHeight, MIN_SIZE, MAX_HEIGHT);

		int[][] next = new int[clampedHeight][clampedWidth];
		int[][] nextFg = new int[clampedHeight][clampedWidth];
		int[][] nextBg = new int[clampedHeight][clampedWidth];
		float[][] nextScaleX = new float[clampedHeight][clampedWidth];
		float[][] nextScaleY = new float[clampedHeight][clampedWidth];
		for(int y = 0; y < clampedHeight; y++) {
			Arrays.fill(next[y], fillCodePoint);
			Arrays.fill(nextFg[y], ANSI_DEFAULT);
			Arrays.fill(nextBg[y], ANSI_DEFAULT);
			Arrays.fill(nextScaleX[y], cellScaleX);
			Arrays.fill(nextScaleY[y], cellScaleY);
		}

		int copyHeight = Math.min(height, clampedHeight);
		int copyWidth = Math.min(width, clampedWidth);
		for(int y = 0; y < copyHeight; y++) {
			System.arraycopy(cells[y], 0, next[y], 0, copyWidth);
			System.arraycopy(fgColors[y], 0, nextFg[y], 0, copyWidth);
			System.arraycopy(bgColors[y], 0, nextBg[y], 0, copyWidth);
			System.arraycopy(cellScaleXMap[y], 0, nextScaleX[y], 0, copyWidth);
			System.arraycopy(cellScaleYMap[y], 0, nextScaleY[y], 0, copyWidth);
		}

		for(CanvasLayer layer : explicitLayers.values()) {
			layer.resize(clampedWidth, clampedHeight, width, height, fillCodePoint);
		}
		width = clampedWidth;
		height = clampedHeight;
		cells = next;
		fgColors = nextFg;
		bgColors = nextBg;
		cellScaleXMap = nextScaleX;
		cellScaleYMap = nextScaleY;
	}

	@LuaMadeCallable
	public void clear() {
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT, cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public void clear(String fill) {
		fillCodePoint = toCodePoint(fill);
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT, cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public void clear(String fill, String foreground, String background) {
		fillCodePoint = toCodePoint(fill);
		clearInternal(fillCodePoint, parseColor(foreground), parseColor(background), cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public void pixel(int x, int y, String glyph) {
		setCell(x - 1, y - 1, toCodePoint(glyph), brushFg, brushBg, cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public void pixel(int x, int y, String glyph, String foreground, String background) {
		setCell(x - 1, y - 1, toCodePoint(glyph), parseColor(foreground), parseColor(background), cellScaleX, cellScaleY);
	}

	@LuaMadeCallable
	public void pixelScaled(int x, int y, String glyph, double scale) {
		float clamped = clampScale((float) scale);
		setCell(x - 1, y - 1, toCodePoint(glyph), brushFg, brushBg, clamped, clamped);
	}

	@LuaMadeCallable
	public void pixelScaled(int x, int y, String glyph, double scaleX, double scaleY) {
		setCell(x - 1, y - 1, toCodePoint(glyph), brushFg, brushBg, clampScale((float) scaleX), clampScale((float) scaleY));
	}

	@LuaMadeCallable
	public void pixelScaled(int x, int y, String glyph, String foreground, String background, double scaleX, double scaleY) {
		setCell(
				x - 1,
				y - 1,
				toCodePoint(glyph),
				parseColor(foreground),
				parseColor(background),
				clampScale((float) scaleX),
				clampScale((float) scaleY)
		);
	}

	@LuaMadeCallable
	public void setPixelScale(int x, int y, double scale) {
		setPixelScale(x, y, scale, scale);
	}

	@LuaMadeCallable
	public void setPixelScale(int x, int y, double scaleX, double scaleY) {
		int px = x - 1;
		int py = y - 1;
		if(px < 0 || py < 0 || px >= width || py >= height) {
			return;
		}

		cellScaleXMap[py][px] = clampScale((float) scaleX);
		cellScaleYMap[py][px] = clampScale((float) scaleY);
	}

	@LuaMadeCallable
	public double[] getPixelScale(int x, int y) {
		int px = x - 1;
		int py = y - 1;
		if(px < 0 || py < 0 || px >= width || py >= height) {
			return new double[]{cellScaleX, cellScaleY};
		}
		return new double[]{cellScaleXMap[py][px], cellScaleYMap[py][px]};
	}

	@LuaMadeCallable
	public void line(int x1, int y1, int x2, int y2, String glyph) {
		int cp = toCodePoint(glyph);
		int xStart = x1 - 1;
		int yStart = y1 - 1;
		int xEnd = x2 - 1;
		int yEnd = y2 - 1;

		int dx = Math.abs(xEnd - xStart);
		int sx = xStart < xEnd ? 1 : -1;
		int dy = -Math.abs(yEnd - yStart);
		int sy = yStart < yEnd ? 1 : -1;
		int err = dx + dy;
		int fg = brushFg;
		int bg = brushBg;

		while(true) {
			setCell(xStart, yStart, cp, fg, bg);
			if(xStart == xEnd && yStart == yEnd) {
				break;
			}
			int e2 = 2 * err;
			if(e2 >= dy) {
				err += dy;
				xStart += sx;
			}
			if(e2 <= dx) {
				err += dx;
				yStart += sy;
			}
		}
	}

	@LuaMadeCallable
	public void rect(int x, int y, int w, int h, String glyph) {
		if(w <= 0 || h <= 0) {
			return;
		}

		int cp = toCodePoint(glyph);
		int x0 = x - 1;
		int y0 = y - 1;
		int x1 = x0 + w - 1;
		int y1 = y0 + h - 1;
		int fg = brushFg;
		int bg = brushBg;

		for(int xx = x0; xx <= x1; xx++) {
			setCell(xx, y0, cp, fg, bg);
			setCell(xx, y1, cp, fg, bg);
		}
		for(int yy = y0; yy <= y1; yy++) {
			setCell(x0, yy, cp, fg, bg);
			setCell(x1, yy, cp, fg, bg);
		}
	}

	@LuaMadeCallable
	public void fillRect(int x, int y, int w, int h, String glyph) {
		if(w <= 0 || h <= 0) {
			return;
		}

		int cp = toCodePoint(glyph);
		int x0 = x - 1;
		int y0 = y - 1;
		int x1 = x0 + w - 1;
		int y1 = y0 + h - 1;
		int fg = brushFg;
		int bg = brushBg;

		for(int yy = y0; yy <= y1; yy++) {
			for(int xx = x0; xx <= x1; xx++) {
				setCell(xx, yy, cp, fg, bg);
			}
		}
	}

	@LuaMadeCallable
	public void circle(int cx, int cy, int radius, String glyph) {
		if(radius < 0) {
			return;
		}

		int cp = toCodePoint(glyph);
		int x = radius;
		int y = 0;
		int decision = 1 - radius;
		int cpx = cx - 1;
		int cpy = cy - 1;
		int fg = brushFg;
		int bg = brushBg;

		while(y <= x) {
			plotCirclePoints(cpx, cpy, x, y, cp, fg, bg);
			y++;
			if(decision <= 0) {
				decision += 2 * y + 1;
			} else {
				x--;
				decision += 2 * (y - x) + 1;
			}
		}
	}

	@LuaMadeCallable
	public void fillCircle(int cx, int cy, int radius, String glyph) {
		if(radius < 0) {
			return;
		}

		int cp = toCodePoint(glyph);
		int cpx = cx - 1;
		int cpy = cy - 1;
		int radiusSquared = radius * radius;
		int fg = brushFg;
		int bg = brushBg;

		for(int yy = -radius; yy <= radius; yy++) {
			for(int xx = -radius; xx <= radius; xx++) {
				if((xx * xx) + (yy * yy) <= radiusSquared) {
					setCell(cpx + xx, cpy + yy, cp, fg, bg);
				}
			}
		}
	}

	@LuaMadeCallable
	public void text(int x, int y, String content) {
		if(content == null || content.isEmpty()) {
			return;
		}

		int px = x - 1;
		int py = y - 1;
		if(py < 0 || py >= height) {
			return;
		}

		int cursorX = px;
		int fg = brushFg;
		int bg = brushBg;
		for(int offset = 0; offset < content.length();) {
			int cp = content.codePointAt(offset);
			offset += Character.charCount(cp);
			setCell(cursorX, py, cp, fg, bg);
			cursorX++;
		}
	}

	@LuaMadeCallable
	public String frame() {
		if(!ansiEnabled) {
			return frameWithoutAnsi();
		}

		StringBuilder builder = new StringBuilder((width + 16) * height);
		int activeFg = ANSI_DEFAULT;
		int activeBg = ANSI_DEFAULT;

		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				int fg = fgColors[y][x];
				int bg = bgColors[y][x];
				if(fg != activeFg || bg != activeBg) {
					builder.append(buildAnsiSequence(fg, bg));
					activeFg = fg;
					activeBg = bg;
				}
				builder.appendCodePoint(cells[y][x]);
			}

			if(activeFg != ANSI_DEFAULT || activeBg != ANSI_DEFAULT) {
				builder.append(ANSI_RESET);
				activeFg = ANSI_DEFAULT;
				activeBg = ANSI_DEFAULT;
			}

			if(y < height - 1) {
				builder.append('\n');
			}
		}

		return builder.toString();
	}

	private String frameWithoutAnsi() {
		StringBuilder builder = new StringBuilder((width + 1) * height);
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				builder.appendCodePoint(cells[y][x]);
			}
			if(y < height - 1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}

	@LuaMadeCallable
	public String getBackend() {
		Backend activeBackend = resolveActiveBackend();
		return activeBackend == Backend.CANVAS ? "canvas" : "terminal";
	}

	@LuaMadeCallable
	public void setBackend(String backendName) {
		if(backendName == null) {
			backend = Backend.CANVAS;
			return;
		}

		String normalized = backendName.trim().toLowerCase(Locale.ROOT);
		if("canvas".equals(normalized)) {
			backend = Backend.CANVAS;
		} else {
			backend = Backend.TERMINAL;
		}
	}

	@LuaMadeCallable
	public void setCellScale(double scaleX, double scaleY) {
		cellScaleX = clampScale((float) scaleX);
		cellScaleY = clampScale((float) scaleY);
		if(activeLayer != null) {
			activeLayer.scaleX = cellScaleX;
			activeLayer.scaleY = cellScaleY;
		} else {
			for(int y = 0; y < height; y++) {
				Arrays.fill(cellScaleXMap[y], cellScaleX);
				Arrays.fill(cellScaleYMap[y], cellScaleY);
			}
		}
	}

	@LuaMadeCallable
	public double[] getCellScale() {
		return new double[]{cellScaleX, cellScaleY};
	}

	@LuaMadeCallable
	public void setCellScale(double scale) {
		float clamped = clampScale((float) scale);
		cellScaleX = clamped;
		cellScaleY = clamped;
		if(activeLayer != null) {
			activeLayer.scaleX = clamped;
			activeLayer.scaleY = clamped;
		} else {
			for(int y = 0; y < height; y++) {
				Arrays.fill(cellScaleXMap[y], clamped);
				Arrays.fill(cellScaleYMap[y], clamped);
			}
		}
	}

	private float clampScale(float value) {
		if(Float.isNaN(value) || Float.isInfinite(value)) {
			return 1.0F;
		}
		return Math.max(MIN_CELL_SCALE, Math.min(MAX_CELL_SCALE, value));
	}

	private Backend resolveActiveBackend() {
		if(backend == Backend.CANVAS && !ConfigManager.isGfxCanvasBackendEnabled()) {
			return Backend.TERMINAL;
		}
		return backend;
	}

	private int[] flattenCodePoints() {
		int[] flattened = new int[width * height];
		int idx = 0;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				flattened[idx] = cells[y][x];
				idx++;
			}
		}
		return flattened;
	}

	private List<Console.GraphicsFrame.GraphicsLayer> createCanvasLayers() {
		List<Console.GraphicsFrame.GraphicsLayer> result =
				new ArrayList<Console.GraphicsFrame.GraphicsLayer>();

		// --- flat buffer: auto-group by per-cell scale (unchanged behaviour) ---
		if(explicitLayers.isEmpty()) {
			Map<ScaleKey, Integer> layerIndices = new LinkedHashMap<ScaleKey, Integer>();
			List<int[]> layerBuffers = new ArrayList<int[]>();
			List<int[]> layerFgBuffers = new ArrayList<int[]>();
			List<int[]> layerBgBuffers = new ArrayList<int[]>();
			List<ScaleKey> layerKeys = new ArrayList<ScaleKey>();
			ScaleKey fallbackKey = new ScaleKey(cellScaleX, cellScaleY);

			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					ScaleKey key = new ScaleKey(cellScaleXMap[y][x], cellScaleYMap[y][x]);
					Integer idx = layerIndices.get(key);
					if(idx == null) {
						if(layerIndices.size() >= MAX_CANVAS_LAYERS) {
							idx = layerIndices.get(fallbackKey);
							if(idx == null) {
								idx = 0;
								layerIndices.put(fallbackKey, idx);
								layerKeys.add(fallbackKey);
								layerBuffers.add(createEmptyLayerBuffer());
								layerFgBuffers.add(createEmptyColorBuffer());
								layerBgBuffers.add(createEmptyColorBuffer());
							}
						} else {
							idx = layerBuffers.size();
							layerIndices.put(key, idx);
							layerKeys.add(key);
							layerBuffers.add(createEmptyLayerBuffer());
							layerFgBuffers.add(createEmptyColorBuffer());
							layerBgBuffers.add(createEmptyColorBuffer());
						}
					}
					int flatIndex = (y * width) + x;
					layerBuffers.get(idx)[flatIndex] = cells[y][x];
					layerFgBuffers.get(idx)[flatIndex] = fgColors[y][x];
					layerBgBuffers.get(idx)[flatIndex] = bgColors[y][x];
				}
			}

			if(layerBuffers.isEmpty()) {
				result.add(new Console.GraphicsFrame.GraphicsLayer(
						"", frameWithoutAnsi(), cellScaleX, cellScaleY, flattenCodePoints(), flattenColors(fgColors), flattenColors(bgColors)));
				return result;
			}

			for(int i = 0; i < layerBuffers.size(); i++) {
				ScaleKey key = layerKeys.get(i);
				int[] buf = layerBuffers.get(i);
				int[] fg = layerFgBuffers.get(i);
				int[] bg = layerBgBuffers.get(i);
				result.add(new Console.GraphicsFrame.GraphicsLayer(
						"", buildLayerText(buf), key.scaleX, key.scaleY, buf, fg, bg));
			}
			return result;
		}

		// --- explicit-layer mode: flat buffer is the base (z = -1), then named layers in z-order ---
		// Flat base (always bottom)
		int[] baseCodePoints = flattenCodePoints();
		result.add(new Console.GraphicsFrame.GraphicsLayer(
				"", buildLayerText(baseCodePoints), cellScaleX, cellScaleY, baseCodePoints, flattenColors(fgColors), flattenColors(bgColors)));

		for(CanvasLayer layer : sortedLayers()) {
			int[] buf = layer.flattenCodePoints(width, height);
			int[] fg = layer.flattenColors(layer.fgColors, width, height);
			int[] bg = layer.flattenColors(layer.bgColors, width, height);
			result.add(new Console.GraphicsFrame.GraphicsLayer(
					layer.name, buildLayerText(buf), layer.scaleX, layer.scaleY, buf, fg, bg));
		}
		return result;
	}

	private int[] createEmptyLayerBuffer() {
		int[] buffer = new int[width * height];
		Arrays.fill(buffer, ' ');
		return buffer;
	}

	private int[] createEmptyColorBuffer() {
		int[] buffer = new int[width * height];
		Arrays.fill(buffer, ANSI_DEFAULT);
		return buffer;
	}

	private String buildLayerText(int[] codePointBuffer) {
		StringBuilder builder = new StringBuilder((width + 1) * height);
		int idx = 0;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				builder.appendCodePoint(codePointBuffer[idx]);
				idx++;
			}
			if(y < height - 1) {
				builder.append('\n');
			}
		}
		return builder.toString();
	}

	private int[] flattenColors(int[][] colors) {
		int[] flattened = new int[width * height];
		int idx = 0;
		for(int y = 0; y < height; y++) {
			for(int x = 0; x < width; x++) {
				flattened[idx] = colors[y][x];
				idx++;
			}
		}
		return flattened;
	}

	private void plotCirclePoints(int cx, int cy, int x, int y, int cp, int fg, int bg) {
		setCell(cx + x, cy + y, cp, fg, bg);
		setCell(cx + y, cy + x, cp, fg, bg);
		setCell(cx - y, cy + x, cp, fg, bg);
		setCell(cx - x, cy + y, cp, fg, bg);
		setCell(cx - x, cy - y, cp, fg, bg);
		setCell(cx - y, cy - x, cp, fg, bg);
		setCell(cx + y, cy - x, cp, fg, bg);
		setCell(cx + x, cy - y, cp, fg, bg);
	}

	private void clearInternal(int cp, int fg, int bg, float scaleX, float scaleY) {
		if(activeLayer != null) {
			activeLayer.clear(cp, fg);
		} else {
			for(int y = 0; y < height; y++) {
				Arrays.fill(cells[y], cp);
				Arrays.fill(fgColors[y], fg);
				Arrays.fill(bgColors[y], bg);
				Arrays.fill(cellScaleXMap[y], scaleX);
				Arrays.fill(cellScaleYMap[y], scaleY);
			}
		}
	}

	private void setCell(int x, int y, int codePoint, int fg, int bg) {
		setCell(x, y, codePoint, fg, bg, cellScaleX, cellScaleY);
	}

	private int[][] getActiveCells() {
		return activeLayer != null ? activeLayer.cells : cells;
	}

	private int[][] getActiveFg() {
		return activeLayer != null ? activeLayer.fgColors : fgColors;
	}

	private int[][] getActiveBg() {
		return activeLayer != null ? activeLayer.bgColors : bgColors;
	}

	private void setCell(int x, int y, int codePoint, int fg, int bg, float scaleX, float scaleY) {
		if(x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		getActiveCells()[y][x] = codePoint;
		getActiveFg()[y][x] = fg;
		getActiveBg()[y][x] = bg;
		if(activeLayer == null) {
			cellScaleXMap[y][x] = scaleX;
			cellScaleYMap[y][x] = scaleY;
		}
	}

	// -------------------------------------------------------------------------
	// Explicit layer management API
	// -------------------------------------------------------------------------

	/**
	 * Creates a named layer above all existing layers.
	 * If the layer already exists its parameters are updated.
	 */
	@LuaMadeCallable
	public void createLayer(String name) {
		if(name == null || name.isEmpty()) return;
		if(explicitLayers.size() >= MAX_CANVAS_LAYERS) return;
		if(!explicitLayers.containsKey(name)) {
			explicitLayers.put(name, new CanvasLayer(name, width, height, cellScaleX, cellScaleY,
					explicitLayers.size(), fillCodePoint));
		}
	}

	/** Removes a named layer. If that layer was active, active layer resets to none. */
	@LuaMadeCallable
	public void removeLayer(String name) {
		if(name == null) return;
		explicitLayers.remove(name);
		if(activeLayer != null && name.equals(activeLayer.name)) {
			activeLayer = null;
		}
	}

	/** Activates a named layer so all draw calls target it. Pass "" or call setLayer() with no layer to return to the flat buffer. */
	@LuaMadeCallable
	public void setLayer(String name) {
		if(name == null || name.isEmpty()) {
			activeLayer = null;
			return;
		}
		activeLayer = explicitLayers.get(name);
	}

	/** Returns to drawing on the flat (default) buffer. */
	@LuaMadeCallable
	public void clearActiveLayer() {
		activeLayer = null;
	}

	/** Clears a specific named layer to spaces. */
	@LuaMadeCallable
	public void clearLayer(String name) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer != null) {
			layer.clear(fillCodePoint, ANSI_DEFAULT);
		}
	}

	/** Sets the pixel density (scale) of a named layer. */
	@LuaMadeCallable
	public void setLayerScale(String name, double scale) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer != null) {
			layer.scaleX = clampScale((float) scale);
			layer.scaleY = clampScale((float) scale);
		}
	}

	/** Sets independent X/Y pixel density of a named layer. */
	@LuaMadeCallable
	public void setLayerScale(String name, double scaleX, double scaleY) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer != null) {
			layer.scaleX = clampScale((float) scaleX);
			layer.scaleY = clampScale((float) scaleY);
		}
	}

	/** Returns {scaleX, scaleY} for a named layer, or the global cell scale if the layer doesn't exist. */
	@LuaMadeCallable
	public double[] getLayerScale(String name) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer == null) return new double[]{cellScaleX, cellScaleY};
		return new double[]{layer.scaleX, layer.scaleY};
	}

	/** Returns the z-index of a named layer (0 = bottom-most explicit layer). */
	@LuaMadeCallable
	public int getLayerZ(String name) {
		CanvasLayer layer = explicitLayers.get(name);
		return layer == null ? -1 : layer.zIndex;
	}

	/** Sets the z-index of a named layer, then re-sorts the layer stack. */
	@LuaMadeCallable
	public void setLayerZ(String name, int z) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer == null) return;
		layer.zIndex = z;
		rebuildLayerOrder();
	}

	/** Returns a Lua table (array) of layer names in current z-order (bottom to top). */
	@LuaMadeCallable
	public String[] getLayerNames() {
		List<CanvasLayer> sorted = sortedLayers();
		String[] names = new String[sorted.size()];
		for(int i = 0; i < sorted.size(); i++) {
			names[i] = sorted.get(i).name;
		}
		return names;
	}

	/** Returns true if a layer with the given name exists. */
	@LuaMadeCallable
	public boolean hasLayer(String name) {
		return explicitLayers.containsKey(name);
	}

	/** Returns the name of the currently active layer, or "" if on the flat buffer. */
	@LuaMadeCallable
	public String getActiveLayer() {
		return activeLayer == null ? "" : activeLayer.name;
	}

	/** Moves a layer one step toward the top of the stack. */
	@LuaMadeCallable
	public void moveLayerUp(String name) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer == null) return;
		list_swap_up(layer);
		rebuildLayerOrder();
	}

	/** Moves a layer one step toward the bottom of the stack. */
	@LuaMadeCallable
	public void moveLayerDown(String name) {
		CanvasLayer layer = explicitLayers.get(name);
		if(layer == null) return;
		list_swap_down(layer);
		rebuildLayerOrder();
	}

	private List<CanvasLayer> sortedLayers() {
		List<CanvasLayer> list = new ArrayList<CanvasLayer>(explicitLayers.values());
		java.util.Collections.sort(list, new java.util.Comparator<CanvasLayer>() {
			@Override
			public int compare(CanvasLayer a, CanvasLayer b) {
				return Integer.compare(a.zIndex, b.zIndex);
			}
		});
		return list;
	}

	private void rebuildLayerOrder() {
		List<CanvasLayer> sorted = sortedLayers();
		for(int i = 0; i < sorted.size(); i++) {
			sorted.get(i).zIndex = i;
		}
	}

	private void list_swap_up(CanvasLayer target) {
		List<CanvasLayer> sorted = sortedLayers();
		int idx = sorted.indexOf(target);
		if(idx < sorted.size() - 1) {
			int tmp = sorted.get(idx).zIndex;
			sorted.get(idx).zIndex = sorted.get(idx + 1).zIndex;
			sorted.get(idx + 1).zIndex = tmp;
		}
	}

	private void list_swap_down(CanvasLayer target) {
		List<CanvasLayer> sorted = sortedLayers();
		int idx = sorted.indexOf(target);
		if(idx > 0) {
			int tmp = sorted.get(idx).zIndex;
			sorted.get(idx).zIndex = sorted.get(idx - 1).zIndex;
			sorted.get(idx - 1).zIndex = tmp;
		}
	}

	private int toCodePoint(String glyph) {
		if(glyph == null || glyph.isEmpty()) {
			return ' ';
		}
		return glyph.codePointAt(0);
	}

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	/**
	 * Mutable named canvas layer with its own cell/color buffers, scale, and z-index.
	 */
	private final class CanvasLayer {
		private final String name;
		private int[][] cells;
		private int[][] fgColors;
		private int[][] bgColors;
		private float scaleX;
		private float scaleY;
		private int zIndex;

		private CanvasLayer(String name, int w, int h, float scaleX, float scaleY, int zIndex, int fillCp) {
			this.name = name;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
			this.zIndex = zIndex;
			cells = new int[h][w];
			fgColors = new int[h][w];
			bgColors = new int[h][w];
			clear(fillCp, ANSI_DEFAULT);
		}

		private void clear(int fillCp, int fg) {
			int h = cells.length;
			int w = h > 0 ? cells[0].length : 0;
			for(int y = 0; y < h; y++) {
				Arrays.fill(cells[y], fillCp);
				Arrays.fill(fgColors[y], fg);
				Arrays.fill(bgColors[y], ANSI_DEFAULT);
			}
		}

		private void resize(int newW, int newH, int oldW, int oldH, int fillCp) {
			int[][] next = new int[newH][newW];
			int[][] nextFg = new int[newH][newW];
			int[][] nextBg = new int[newH][newW];
			for(int y = 0; y < newH; y++) {
				Arrays.fill(next[y], fillCp);
				Arrays.fill(nextFg[y], ANSI_DEFAULT);
				Arrays.fill(nextBg[y], ANSI_DEFAULT);
			}
			int copyH = Math.min(oldH, newH);
			int copyW = Math.min(oldW, newW);
			for(int y = 0; y < copyH; y++) {
				System.arraycopy(cells[y], 0, next[y], 0, copyW);
				System.arraycopy(fgColors[y], 0, nextFg[y], 0, copyW);
				System.arraycopy(bgColors[y], 0, nextBg[y], 0, copyW);
			}
			cells = next;
			fgColors = nextFg;
			bgColors = nextBg;
		}

		private int[] flattenCodePoints(int w, int h) {
			int[] out = new int[w * h];
			Arrays.fill(out, ' ');
			int idx = 0;
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					out[idx] = cells[y][x];
					idx++;
				}
			}
			return out;
		}

		private int[] flattenColors(int[][] colors, int w, int h) {
			int[] out = new int[w * h];
			Arrays.fill(out, ANSI_DEFAULT);
			int idx = 0;
			for(int y = 0; y < h; y++) {
				for(int x = 0; x < w; x++) {
					out[idx] = colors[y][x];
					idx++;
				}
			}
			return out;
		}
	}

	private static final class ScaleKey {
		private final float scaleX;
		private final float scaleY;

		private ScaleKey(float scaleX, float scaleY) {
			this.scaleX = scaleX;
			this.scaleY = scaleY;
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			if(!(obj instanceof ScaleKey)) {
				return false;
			}
			ScaleKey other = (ScaleKey) obj;
			return Float.floatToIntBits(scaleX) == Float.floatToIntBits(other.scaleX)
					&& Float.floatToIntBits(scaleY) == Float.floatToIntBits(other.scaleY);
		}

		@Override
		public int hashCode() {
			int result = Float.floatToIntBits(scaleX);
			result = 31 * result + Float.floatToIntBits(scaleY);
			return result;
		}
	}

	private enum Backend {
		TERMINAL,
		CANVAS
	}

	private int parseColor(String colorValue) {
		if(colorValue == null) {
			return ANSI_DEFAULT;
		}

		String value = colorValue.trim().toLowerCase(Locale.ROOT);
		if(value.isEmpty() || "default".equals(value) || "none".equals(value) || "reset".equals(value)) {
			return ANSI_DEFAULT;
		}

		try {
			int numeric = Integer.parseInt(value);
			return clamp(numeric, 0, 255);
		} catch(NumberFormatException ignored) {
			// Fall through to named colors.
		}

		switch(value) {
			case "black":
				return 0;
			case "red":
				return 1;
			case "green":
				return 2;
			case "yellow":
				return 3;
			case "blue":
				return 4;
			case "magenta":
				return 5;
			case "cyan":
				return 6;
			case "white":
				return 7;
			case "gray":
			case "grey":
			case "bright_black":
			case "bright-black":
				return 8;
			case "bright_red":
			case "bright-red":
				return 9;
			case "bright_green":
			case "bright-green":
				return 10;
			case "bright_yellow":
			case "bright-yellow":
				return 11;
			case "bright_blue":
			case "bright-blue":
				return 12;
			case "bright_magenta":
			case "bright-magenta":
				return 13;
			case "bright_cyan":
			case "bright-cyan":
				return 14;
			case "bright_white":
			case "bright-white":
				return 15;
			default:
				return ANSI_DEFAULT;
		}
	}

	private String buildAnsiSequence(int fg, int bg) {
		if(fg == ANSI_DEFAULT && bg == ANSI_DEFAULT) {
			return ANSI_RESET;
		}

		StringBuilder codes = new StringBuilder();
		appendAnsiColorCode(codes, fg, true);
		appendAnsiColorCode(codes, bg, false);
		return ANSI_ESCAPE + codes + "m";
	}

	private void appendAnsiColorCode(StringBuilder codes, int color, boolean foreground) {
		if(codes.length() > 0) {
			codes.append(';');
		}

		if(color == ANSI_DEFAULT) {
			codes.append(foreground ? 39 : 49);
			return;
		}

		if(color <= 7) {
			codes.append((foreground ? 30 : 40) + color);
			return;
		}

		if(color <= 15) {
			codes.append((foreground ? 90 : 100) + (color - 8));
			return;
		}

		codes.append(foreground ? 38 : 48).append(';').append(5).append(';').append(color);
	}
}
