package luamade.lua.gfx;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;

import java.util.*;

/**
 * Thread-safe graphics command buffer exposed to Lua scripts.
 * Commands are captured off-thread and consumed on the render thread.
 */
public class GfxApi extends LuaMadeUserdata {

	private final Object lock = new Object();
	private final Map<String, LayerState> layers = new LinkedHashMap<>();
	private String activeLayer = "default";
	private int nextLayerOrder = 1;
	private int canvasWidth = 1;
	private int canvasHeight = 1;
	private long revision;
	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_BITMAP_PIXELS = 65536;
	private static final int MAX_STROKE_WIDTH = 16;

	public GfxApi() {
		layers.put(activeLayer, new LayerState(0));
	}

	@LuaMadeCallable
	public Boolean setLayer(String name) {
		if(name == null) {
			return false;
		}
		String normalized = normalizeLayerName(name);
		if(normalized.isEmpty()) {
			return false;
		}

		synchronized(lock) {
			if(!layers.containsKey(normalized)) {
				if(layers.size() >= maxLayers()) {
					return false;
				}
				layers.put(normalized, new LayerState(nextLayerOrder));
				nextLayerOrder++;
			}
			activeLayer = normalized;
		}
		return true;
	}

	@LuaMadeCallable
	public Boolean createLayer(String name, Integer order) {
		if(name == null) {
			return false;
		}
		String normalized = normalizeLayerName(name);
		if(normalized.isEmpty()) {
			return false;
		}

		synchronized(lock) {
			LayerState existing = layers.get(normalized);
			if(existing != null) {
				if(order != null) {
					existing.order = order;
					revision++;
				}
				return true;
			}
			if(layers.size() >= maxLayers()) {
				return false;
			}
			layers.put(normalized, new LayerState(order == null ? nextLayerOrder++ : order));
			revision++;
		}
		return true;
	}

	@LuaMadeCallable
	public Boolean removeLayer(String name) {
		if(name == null) {
			return false;
		}
		String normalized = normalizeLayerName(name);
		if(normalized.isEmpty() || "default".equals(normalized)) {
			return false;
		}

		synchronized(lock) {
			LayerState removed = layers.remove(normalized);
			if(removed == null) {
				return false;
			}
			if(normalized.equals(activeLayer)) {
				activeLayer = "default";
			}
			revision++;
		}
		return true;
	}

	@LuaMadeCallable
	public String[] getLayers() {
		synchronized(lock) {
			return layers.keySet().toArray(new String[0]);
		}
	}

	@LuaMadeCallable
	public Boolean setLayerVisible(String name, Boolean visible) {
		if(name == null || visible == null) {
			return false;
		}
		String normalized = normalizeLayerName(name);
		synchronized(lock) {
			LayerState layer = layers.get(normalized);
			if(layer == null) {
				return false;
			}
			layer.visible = visible;
			revision++;
		}
		return true;
	}

	@LuaMadeCallable
	public void clear() {
		synchronized(lock) {
			for(LayerState layer : layers.values()) {
				layer.commands.clear();
			}
			revision++;
		}
	}

	@LuaMadeCallable
	public Boolean clearLayer(String name) {
		if(name == null) {
			return false;
		}
		String normalized = normalizeLayerName(name);
		synchronized(lock) {
			LayerState layer = layers.get(normalized);
			if(layer == null) {
				return false;
			}
			layer.commands.clear();
			revision++;
		}
		return true;
	}

	@LuaMadeCallable
	public void setCanvasSize(Integer width, Integer height) {
		int normalizedWidth = width == null ? 1 : Math.max(1, width);
		int normalizedHeight = height == null ? 1 : Math.max(1, height);
		synchronized(lock) {
			if(canvasWidth == normalizedWidth && canvasHeight == normalizedHeight) {
				return;
			}
			canvasWidth = normalizedWidth;
			canvasHeight = normalizedHeight;
			revision++;
		}
	}

	@LuaMadeCallable
	public Integer getWidth() {
		synchronized(lock) {
			return canvasWidth;
		}
	}

	@LuaMadeCallable
	public Integer getHeight() {
		synchronized(lock) {
			return canvasHeight;
		}
	}

	@LuaMadeCallable
	public Boolean point(Double x, Double y, Double r, Double g, Double b, Double a) {
		if(x == null || y == null) {
			return false;
		}
		return appendCommand(DrawCommand.point((float) clampValue(x, 0, canvasWidth - 1), (float) clampValue(y, 0, canvasHeight - 1),
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0)));
	}

	@LuaMadeCallable
	public Boolean line(Double x1, Double y1, Double x2, Double y2, Double r, Double g, Double b, Double a) {
		return line(x1, y1, x2, y2, r, g, b, a, 1.0);
	}

	@LuaMadeCallable
	public Boolean line(Double x1, Double y1, Double x2, Double y2, Double r, Double g, Double b, Double a, Double thickness) {
		if(x1 == null || y1 == null || x2 == null || y2 == null) {
			return false;
		}
		float fx1 = (float) clampValue(x1, 0, canvasWidth - 1);
		float fy1 = (float) clampValue(y1, 0, canvasHeight - 1);
		float fx2 = (float) clampValue(x2, 0, canvasWidth - 1);
		float fy2 = (float) clampValue(y2, 0, canvasHeight - 1);
		return appendCommand(DrawCommand.line(fx1, fy1, fx2, fy2,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), toStrokeWidth(thickness, 1.0)));
	}

	@LuaMadeCallable
	public Boolean rect(Double x, Double y, Double width, Double height, Double r, Double g, Double b, Double a, Boolean filled) {
		if(x == null || y == null || width == null || height == null) {
			return false;
		}

		double w = Math.max(0.0, width);
		double h = Math.max(0.0, height);
		double x1 = clampValue(x, 0, canvasWidth);
		double y1 = clampValue(y, 0, canvasHeight);
		double x2 = clampValue(x + w, 0, canvasWidth);
		double y2 = clampValue(y + h, 0, canvasHeight);
		if(x2 <= x1 || y2 <= y1) {
			return false;
		}

		return appendCommand(DrawCommand.rect((float) x1, (float) y1, (float) x2, (float) y2,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled));
	}

	@LuaMadeCallable
	public Boolean circle(Double x, Double y, Double radius, Double r, Double g, Double b, Double a, Boolean filled, Integer segments) {
		return circle(x, y, radius, r, g, b, a, filled, segments, 1.0);
	}

	@LuaMadeCallable
	public Boolean circle(Double x, Double y, Double radius, Double r, Double g, Double b, Double a, Boolean filled, Integer segments, Double thickness) {
		if(x == null || y == null || radius == null) {
			return false;
		}

		float centerX = (float) clampValue(x, 0, canvasWidth - 1);
		float centerY = (float) clampValue(y, 0, canvasHeight - 1);
		float clampedRadius = (float) clampValue(radius, 0.0, Math.max(canvasWidth, canvasHeight));
		if(clampedRadius <= 0.0f) {
			return false;
		}

		int normalizedSegments = clampInt(segments == null ? 24 : segments, 8, 128);
		return appendCommand(DrawCommand.circle(centerX, centerY, clampedRadius,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled, normalizedSegments,
				toStrokeWidth(thickness, 1.0)));
	}

	@LuaMadeCallable
	public Boolean polygon(Double[] points, Double r, Double g, Double b, Double a, Boolean filled) {
		return polygon(points, r, g, b, a, filled, 1.0);
	}

	@LuaMadeCallable
	public Boolean polygon(Double[] points, Double r, Double g, Double b, Double a, Boolean filled, Double thickness) {
		if(points == null || points.length < 6 || (points.length % 2) != 0) {
			return false;
		}

		float[] normalizedPoints = new float[points.length];
		for(int i = 0; i < points.length; i++) {
			Double value = points[i];
			if(value == null) {
				return false;
			}
			if((i % 2) == 0) {
				normalizedPoints[i] = (float) clampValue(value, 0, canvasWidth - 1);
			} else {
				normalizedPoints[i] = (float) clampValue(value, 0, canvasHeight - 1);
			}
		}

		return appendCommand(DrawCommand.polygon(normalizedPoints,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled,
				toStrokeWidth(thickness, 1.0)));
	}

	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a, Integer scale) {
		return text(x, y, value, r, g, b, a, scale, null, null, "left", false);
	}

	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a,
					Integer scale, Integer maxWidth, Integer maxHeight, String align, Boolean wrap) {
		if(x == null || y == null || value == null || value.isEmpty()) {
			return false;
		}

		String normalizedText = value;
		if(normalizedText.length() > MAX_TEXT_LENGTH) {
			normalizedText = normalizedText.substring(0, MAX_TEXT_LENGTH);
		}

		int normalizedScale = clampInt(scale == null ? 1 : scale, 1, 16);
		float drawX = (float) clampValue(x, 0, canvasWidth - 1);
		float drawY = (float) clampValue(y, 0, canvasHeight - 1);
		int clipWidth = maxWidth == null ? -1 : Math.max(1, maxWidth);
		int clipHeight = maxHeight == null ? -1 : Math.max(1, maxHeight);
		String normalizedAlign = normalizeTextAlign(align);
		boolean shouldWrap = wrap != null && wrap;

		return appendCommand(DrawCommand.text(drawX, drawY,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), normalizedText,
				normalizedScale, clipWidth, clipHeight, normalizedAlign, shouldWrap));
	}

	@LuaMadeCallable
	public Boolean bitmap(Double x, Double y, Integer width, Integer height, Integer[] rgbaPixels) {
		if(x == null || y == null || width == null || height == null || rgbaPixels == null) {
			return false;
		}

		int normalizedWidth = Math.max(1, width);
		int normalizedHeight = Math.max(1, height);
		long expectedPixels = (long) normalizedWidth * (long) normalizedHeight;
		if(expectedPixels > MAX_BITMAP_PIXELS || rgbaPixels.length < expectedPixels) {
			return false;
		}

		int[] packedPixels = new int[(int) expectedPixels];
		for(int i = 0; i < expectedPixels; i++) {
			Integer pixel = rgbaPixels[i];
			if(pixel == null) {
				return false;
			}
			packedPixels[i] = pixel;
		}

		float drawX = (float) clampValue(x, 0, canvasWidth - 1);
		float drawY = (float) clampValue(y, 0, canvasHeight - 1);
		return appendCommand(DrawCommand.bitmap(drawX, drawY, normalizedWidth, normalizedHeight, packedPixels));
	}

	public FrameSnapshot snapshot() {
		synchronized(lock) {
			List<LayerSnapshot> orderedLayers = new ArrayList<>(layers.size());
			for(Map.Entry<String, LayerState> entry : layers.entrySet()) {
				LayerState state = entry.getValue();
				orderedLayers.add(new LayerSnapshot(entry.getKey(), state.order, state.visible, new ArrayList<>(state.commands)));
			}
			orderedLayers.sort(Comparator.comparingInt(layer -> layer.order));
			return new FrameSnapshot(canvasWidth, canvasHeight, revision, orderedLayers);
		}
	}

	public boolean hasVisibleCommands() {
		synchronized(lock) {
			for(LayerState layer : layers.values()) {
				if(layer.visible && !layer.commands.isEmpty()) {
					return true;
				}
			}
			return false;
		}
	}

	private Boolean appendCommand(DrawCommand command) {
		synchronized(lock) {
			LayerState layer = layers.get(activeLayer);
			if(layer == null) {
				if(layers.size() >= maxLayers()) {
					return false;
				}
				layer = new LayerState(nextLayerOrder);
				nextLayerOrder++;
				layers.put(activeLayer, layer);
			}
			if(layer.commands.size() >= maxCommandsPerLayer()) {
				return false;
			}
			layer.commands.add(command);
			revision++;
		}
		return true;
	}

	private static int maxCommandsPerLayer() {
		return ConfigManager.getGfxMaxCommandsPerLayer();
	}

	private static int maxLayers() {
		return ConfigManager.getGfxMaxLayers();
	}

	private static String normalizeLayerName(String name) {
		String normalized = name.trim();
		if(normalized.length() > 48) {
			normalized = normalized.substring(0, 48);
		}
		return normalized;
	}

	private static float toColor(Double value, double fallback) {
		double source = value == null ? fallback : value;
		return (float) clampValue(source, 0.0, 1.0);
	}

	private static float toStrokeWidth(Double value, double fallback) {
		double source = value == null ? fallback : value;
		return (float) clampValue(source, 1.0, MAX_STROKE_WIDTH);
	}

	private static String normalizeTextAlign(String align) {
		if(align == null) {
			return "left";
		}
		String normalized = align.trim().toLowerCase(Locale.ROOT);
		if("center".equals(normalized) || "right".equals(normalized)) {
			return normalized;
		}
		return "left";
	}

	private static double clampValue(double value, double min, double max) {
		if(Double.isNaN(value) || Double.isInfinite(value)) {
			return min;
		}
		return Math.max(min, Math.min(max, value));
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static final class LayerState {
		private int order;
		private boolean visible = true;
		private final List<DrawCommand> commands = new ArrayList<>();

		private LayerState(int order) {
			this.order = order;
		}
	}

	public static final class FrameSnapshot {
		public final int width;
		public final int height;
		public final long revision;
		public final List<LayerSnapshot> layers;

		private FrameSnapshot(int width, int height, long revision, List<LayerSnapshot> layers) {
			this.width = width;
			this.height = height;
			this.revision = revision;
			this.layers = layers;
		}
	}

	public static final class LayerSnapshot {
		public final String name;
		public final int order;
		public final boolean visible;
		public final List<DrawCommand> commands;

		private LayerSnapshot(String name, int order, boolean visible, List<DrawCommand> commands) {
			this.name = name;
			this.order = order;
			this.visible = visible;
			this.commands = commands;
		}
	}

	public static final class DrawCommand {
		public enum Kind {
			POINT,
			LINE,
			RECT,
			CIRCLE,
			POLYGON,
			TEXT,
			BITMAP
		}

		public final Kind kind;
		public final float x1;
		public final float y1;
		public final float x2;
		public final float y2;
		public final float r;
		public final float g;
		public final float b;
		public final float a;
		public final boolean filled;
		public final int segments;
		public final float[] points;
		public final String text;
		public final int textScale;
		public final int bitmapWidth;
		public final int bitmapHeight;
		public final int[] bitmapPixels;
		public final float lineWidth;
		public final int textMaxWidth;
		public final int textMaxHeight;
		public final String textAlign;
		public final boolean textWrap;

		private DrawCommand(Kind kind, float x1, float y1, float x2, float y2, float r, float g, float b, float a,
				boolean filled, int segments, float[] points, String text, int textScale, int bitmapWidth, int bitmapHeight,
				int[] bitmapPixels, float lineWidth, int textMaxWidth, int textMaxHeight, String textAlign, boolean textWrap) {
			this.kind = kind;
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			this.filled = filled;
			this.segments = segments;
			this.points = points;
			this.text = text;
			this.textScale = textScale;
			this.bitmapWidth = bitmapWidth;
			this.bitmapHeight = bitmapHeight;
			this.bitmapPixels = bitmapPixels;
			this.lineWidth = lineWidth;
			this.textMaxWidth = textMaxWidth;
			this.textMaxHeight = textMaxHeight;
			this.textAlign = textAlign;
			this.textWrap = textWrap;
		}

		private static DrawCommand point(float x, float y, float r, float g, float b, float a) {
			return new DrawCommand(Kind.POINT, x, y, x, y, r, g, b, a, true, 0, null, null,
					1, 0, 0, null, 1.0f, -1, -1, "left", false);
		}

		private static DrawCommand line(float x1, float y1, float x2, float y2, float r, float g, float b, float a, float lineWidth) {
			return new DrawCommand(Kind.LINE, x1, y1, x2, y2, r, g, b, a, true, 0, null, null,
					1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand rect(float x1, float y1, float x2, float y2, float r, float g, float b, float a, boolean filled) {
			return new DrawCommand(Kind.RECT, x1, y1, x2, y2, r, g, b, a, filled, 0, null, null,
					1, 0, 0, null, 1.0f, -1, -1, "left", false);
		}

		private static DrawCommand circle(float x, float y, float radius, float r, float g, float b, float a,
									 boolean filled, int segments, float lineWidth) {
			return new DrawCommand(Kind.CIRCLE, x, y, radius, 0.0f, r, g, b, a, filled, segments, null, null,
					1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand polygon(float[] points, float r, float g, float b, float a, boolean filled, float lineWidth) {
			return new DrawCommand(Kind.POLYGON, 0.0f, 0.0f, 0.0f, 0.0f, r, g, b, a, filled, 0,
					Arrays.copyOf(points, points.length), null, 1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand text(float x, float y, float r, float g, float b, float a, String text, int textScale,
									 int textMaxWidth, int textMaxHeight, String textAlign, boolean textWrap) {
			return new DrawCommand(Kind.TEXT, x, y, 0.0f, 0.0f, r, g, b, a, true, 0, null, text, textScale,
					0, 0, null, 1.0f, textMaxWidth, textMaxHeight, textAlign, textWrap);
		}

		private static DrawCommand bitmap(float x, float y, int width, int height, int[] rgbaPixels) {
			return new DrawCommand(Kind.BITMAP, x, y, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f,
					true, 0, null, null, 1, width, height, Arrays.copyOf(rgbaPixels, rgbaPixels.length),
					1.0f, -1, -1, "left", false);
		}
	}
}
