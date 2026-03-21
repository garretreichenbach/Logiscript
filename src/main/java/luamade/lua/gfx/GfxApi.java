package luamade.lua.gfx;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import org.luaj.vm2.LuaError;

import java.util.*;

/**
 * Thread-safe graphics command buffer exposed to Lua scripts.
 * Commands are captured off-thread and consumed on the render thread.
 */
public class GfxApi extends LuaMadeUserdata {

	private static final int MAX_TEXT_LENGTH = 512;
	private static final int MAX_BITMAP_PIXELS = 65536;
	private static final int MAX_STROKE_WIDTH = 16;
	private final Object lock = new Object();
	private final Map<String, LayerState> layers = new LinkedHashMap<>();
	private final Map<String, List<DrawCommand>> pendingBatch = new LinkedHashMap<>();
	private String activeLayer = "default";
	private int nextLayerOrder = 1;
	private volatile int canvasWidth = 1;
	private volatile int canvasHeight = 1;
	private volatile int viewportWidth = 1;
	private volatile int viewportHeight = 1;
	private boolean autoScaleEnabled = true;
	private boolean canvasSizeExplicit;
	private boolean canvasSizeInitialized;
	private long revision;
	private boolean batching;
	private boolean batchClearAll;
	private volatile Runnable cancellationChecker;

	public GfxApi() {
		layers.put(activeLayer, new LayerState(0));
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

	public void setCancellationChecker(Runnable cancellationChecker) {
		this.cancellationChecker = cancellationChecker;
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
			if(batching) {
				pendingBatch.clear();
				batchClearAll = true;
				return;
			}
			for(LayerState layer : layers.values()) {
				layer.commands.clear();
			}
			revision++;
		}
	}

	/**
	 * Hard reset used by terminal interrupts/reboots.
	 * Clears every visible command immediately, even while a batch is open.
	 */
	public void forceClear() {
		synchronized(lock) {
			for(LayerState layer : layers.values()) {
				layer.commands.clear();
			}
			pendingBatch.clear();
			batchClearAll = false;
			batching = false;
			revision++;
		}
	}

	@LuaMadeCallable
	public void beginBatch() {
		synchronized(lock) {
			pendingBatch.clear();
			batchClearAll = false;
			batching = true;
		}
	}

	@LuaMadeCallable
	public void commitBatch() {
		synchronized(lock) {
			if(!batching) {
				return;
			}
			if(batchClearAll) {
				for(LayerState layer : layers.values()) {
					layer.commands.clear();
				}
			}
			for(Map.Entry<String, List<DrawCommand>> entry : pendingBatch.entrySet()) {
				LayerState layer = layers.get(entry.getKey());
				if(layer != null) {
					if(!batchClearAll) {
						layer.commands.clear();
					}
					layer.commands.addAll(entry.getValue());
				}
			}
			pendingBatch.clear();
			batchClearAll = false;
			batching = false;
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
			if(batching) {
				if(!layers.containsKey(normalized)) {
					return false;
				}
				if(!batchClearAll) {
					pendingBatch.put(normalized, new ArrayList<>());
				}
				return true;
			}
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
			if(canvasWidth == normalizedWidth && canvasHeight == normalizedHeight && canvasSizeExplicit) {
				return;
			}
			canvasWidth = normalizedWidth;
			canvasHeight = normalizedHeight;
			canvasSizeExplicit = true;
			canvasSizeInitialized = true;
			revision++;
		}
	}

	/** Called by the render pipeline to report the visible terminal canvas in pixels. */
	public void setViewportSize(int width, int height) {
		int normalizedWidth = Math.max(1, width);
		int normalizedHeight = Math.max(1, height);
		synchronized(lock) {
			if(viewportWidth == normalizedWidth && viewportHeight == normalizedHeight) {
				return;
			}
			viewportWidth = normalizedWidth;
			viewportHeight = normalizedHeight;
			if(!canvasSizeExplicit) {
				if(!autoScaleEnabled || !canvasSizeInitialized) {
					canvasWidth = normalizedWidth;
					canvasHeight = normalizedHeight;
					canvasSizeInitialized = true;
				}
			}
			revision++;
		}
	}

	@LuaMadeCallable
	public void resetCanvasSize() {
		synchronized(lock) {
			canvasSizeExplicit = false;
			canvasSizeInitialized = false;
			if(!autoScaleEnabled) {
				canvasWidth = viewportWidth;
				canvasHeight = viewportHeight;
			}
			revision++;
		}
	}

	@LuaMadeCallable
	public void setAutoScale(Boolean enabled) {
		if(enabled == null) {
			return;
		}
		synchronized(lock) {
			if(autoScaleEnabled == enabled) {
				return;
			}
			autoScaleEnabled = enabled;
			if(!autoScaleEnabled && !canvasSizeExplicit) {
				canvasWidth = viewportWidth;
				canvasHeight = viewportHeight;
				canvasSizeInitialized = true;
			}
			revision++;
		}
	}

	@LuaMadeCallable
	public Boolean isAutoScaleEnabled() {
		synchronized(lock) {
			return autoScaleEnabled;
		}
	}

	@LuaMadeCallable
	public Integer getWidth() {
		synchronized(lock) {
			return resolveDrawWidthLocked();
		}
	}

	@LuaMadeCallable
	public Integer getHeight() {
		synchronized(lock) {
			return resolveDrawHeightLocked();
		}
	}

	@LuaMadeCallable
	public Integer getViewportWidth() {
		synchronized(lock) {
			return viewportWidth;
		}
	}

	@LuaMadeCallable
	public Integer getViewportHeight() {
		synchronized(lock) {
			return viewportHeight;
		}
	}

	@LuaMadeCallable
	public Double getScaleX() {
		synchronized(lock) {
			return (double) resolveScaleXLocked();
		}
	}

	@LuaMadeCallable
	public Double getScaleY() {
		synchronized(lock) {
			return (double) resolveScaleYLocked();
		}
	}

	public int viewportToCanvasX(int viewportX) {
		synchronized(lock) {
			float scaleX = resolveScaleXLocked();
			if(scaleX <= 0.0f) {
				return 0;
			}
			return clampInt(Math.round(viewportX / scaleX), 0, Math.max(0, resolveDrawWidthLocked() - 1));
		}
	}

	public int viewportToCanvasY(int viewportY) {
		synchronized(lock) {
			float scaleY = resolveScaleYLocked();
			if(scaleY <= 0.0f) {
				return 0;
			}
			return clampInt(Math.round(viewportY / scaleY), 0, Math.max(0, resolveDrawHeightLocked() - 1));
		}
	}

	@LuaMadeCallable
	public Boolean point(Double x, Double y, Double r, Double g, Double b, Double a) {
		if(x == null || y == null) {
			return false;
		}
		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();
		return appendCommand(DrawCommand.point((float) clampValue(x, 0, drawWidth - 1), (float) clampValue(y, 0, drawHeight - 1), toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0)));
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
		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();
		float fx1 = (float) clampValue(x1, 0, drawWidth - 1);
		float fy1 = (float) clampValue(y1, 0, drawHeight - 1);
		float fx2 = (float) clampValue(x2, 0, drawWidth - 1);
		float fy2 = (float) clampValue(y2, 0, drawHeight - 1);
		return appendCommand(DrawCommand.line(fx1, fy1, fx2, fy2, toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), toStrokeWidth(thickness, 1.0)));
	}

	@LuaMadeCallable
	public Boolean rect(Double x, Double y, Double width, Double height, Double r, Double g, Double b, Double a, Boolean filled) {
		if(x == null || y == null || width == null || height == null) {
			return false;
		}
		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();

		double w = Math.max(0.0, width);
		double h = Math.max(0.0, height);
		double x1 = clampValue(x, 0, drawWidth);
		double y1 = clampValue(y, 0, drawHeight);
		double x2 = clampValue(x + w, 0, drawWidth);
		double y2 = clampValue(y + h, 0, drawHeight);
		if(x2 <= x1 || y2 <= y1) {
			return false;
		}

		return appendCommand(DrawCommand.rect((float) x1, (float) y1, (float) x2, (float) y2, toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled));
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
		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();

		float centerX = (float) clampValue(x, 0, drawWidth - 1);
		float centerY = (float) clampValue(y, 0, drawHeight - 1);
		float clampedRadius = (float) clampValue(radius, 0.0, Math.max(drawWidth, drawHeight));
		if(clampedRadius <= 0.0f) {
			return false;
		}

		int normalizedSegments = clampInt(segments == null ? 24 : segments, 8, 128);
		return appendCommand(DrawCommand.circle(centerX, centerY, clampedRadius, toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled, normalizedSegments, toStrokeWidth(thickness, 1.0)));
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
		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();

		float[] normalizedPoints = new float[points.length];
		for(int i = 0; i < points.length; i++) {
			Double value = points[i];
			if(value == null) {
				return false;
			}
			if((i % 2) == 0) {
				normalizedPoints[i] = (float) clampValue(value, 0, drawWidth - 1);
			} else {
				normalizedPoints[i] = (float) clampValue(value, 0, drawHeight - 1);
			}
		}

		return appendCommand(DrawCommand.polygon(normalizedPoints, toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), filled != null && filled, toStrokeWidth(thickness, 1.0)));
	}

	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a, Integer scale) {
		return text(x, y, value, r, g, b, a, scale, null, null, "left", false);
	}

	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a, Integer scale, Integer maxWidth, Integer maxHeight, String align, Boolean wrap) {
		if(x == null || y == null || value == null || value.isEmpty()) {
			return false;
		}

		String normalizedText = value;
		if(normalizedText.length() > MAX_TEXT_LENGTH) {
			normalizedText = normalizedText.substring(0, MAX_TEXT_LENGTH);
		}

		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();
		int normalizedScale = clampInt(scale == null ? 1 : scale, 1, 16);
		float drawX = (float) clampValue(x, 0, drawWidth - 1);
		float drawY = (float) clampValue(y, 0, drawHeight - 1);
		int clipWidth = maxWidth == null ? -1 : Math.max(1, maxWidth);
		int clipHeight = maxHeight == null ? -1 : Math.max(1, maxHeight);
		String normalizedAlign = normalizeTextAlign(align);
		boolean shouldWrap = wrap != null && wrap;

		return appendCommand(DrawCommand.text(drawX, drawY, toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0), normalizedText, normalizedScale, clipWidth, clipHeight, normalizedAlign, shouldWrap));
	}

	@LuaMadeCallable
	public Boolean bitmap(Double x, Double y, Integer width, Integer height, Integer[] rgbaPixels) {
		if(x == null || y == null || width == null || height == null || rgbaPixels == null) {
			return false;
		}

		int drawWidth = getDrawWidth();
		int drawHeight = getDrawHeight();
		int normalizedWidth = Math.max(1, width);
		int normalizedHeight = Math.max(1, height);
		long expectedPixels = (long) normalizedWidth * normalizedHeight;
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

		float drawX = (float) clampValue(x, 0, drawWidth - 1);
		float drawY = (float) clampValue(y, 0, drawHeight - 1);
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
			int drawWidth = resolveDrawWidthLocked();
			int drawHeight = resolveDrawHeightLocked();
			return new FrameSnapshot(drawWidth, drawHeight, viewportWidth, viewportHeight, resolveScaleXLocked(), resolveScaleYLocked(), revision, orderedLayers);
		}
	}

	private int getDrawWidth() {
		synchronized(lock) {
			return resolveDrawWidthLocked();
		}
	}

	private int getDrawHeight() {
		synchronized(lock) {
			return resolveDrawHeightLocked();
		}
	}

	private int resolveDrawWidthLocked() {
		return Math.max(1, canvasWidth);
	}

	private int resolveDrawHeightLocked() {
		return Math.max(1, canvasHeight);
	}

	private float resolveScaleXLocked() {
		int drawWidth = resolveDrawWidthLocked();
		return drawWidth <= 0 ? 1.0f : Math.max(0.0001f, viewportWidth / (float) drawWidth);
	}

	private float resolveScaleYLocked() {
		int drawHeight = resolveDrawHeightLocked();
		return drawHeight <= 0 ? 1.0f : Math.max(0.0001f, viewportHeight / (float) drawHeight);
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
		throwIfCanceled();
		synchronized(lock) {
			if(batching) {
				if(!layers.containsKey(activeLayer)) {
					if(layers.size() >= maxLayers()) {
						return false;
					}
					layers.put(activeLayer, new LayerState(nextLayerOrder));
					nextLayerOrder++;
				}
				List<DrawCommand> pending = pendingBatch.computeIfAbsent(activeLayer, k -> new ArrayList<>());
				if(pending.size() >= maxCommandsPerLayer()) {
					return false;
				}
				pending.add(command);
				return true;
			}
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

	private void throwIfCanceled() {
		Runnable checker = cancellationChecker;
		if(checker != null) {
			checker.run();
			return;
		}
		if(Thread.currentThread().isInterrupted()) {
			throw new LuaError("Script canceled");
		}
	}

	private static final class LayerState {
		private final List<DrawCommand> commands = new ArrayList<>();
		private int order;
		private boolean visible = true;

		private LayerState(int order) {
			this.order = order;
		}
	}

	public static final class FrameSnapshot {
		public final int width;
		public final int height;
		public final int viewportWidth;
		public final int viewportHeight;
		public final float scaleX;
		public final float scaleY;
		public final long revision;
		public final List<LayerSnapshot> layers;

		private FrameSnapshot(int width, int height, int viewportWidth, int viewportHeight, float scaleX, float scaleY, long revision, List<LayerSnapshot> layers) {
			this.width = width;
			this.height = height;
			this.viewportWidth = viewportWidth;
			this.viewportHeight = viewportHeight;
			this.scaleX = scaleX;
			this.scaleY = scaleY;
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

		private DrawCommand(Kind kind, float x1, float y1, float x2, float y2, float r, float g, float b, float a, boolean filled, int segments, float[] points, String text, int textScale, int bitmapWidth, int bitmapHeight, int[] bitmapPixels, float lineWidth, int textMaxWidth, int textMaxHeight, String textAlign, boolean textWrap) {
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
			return new DrawCommand(Kind.POINT, x, y, x, y, r, g, b, a, true, 0, null, null, 1, 0, 0, null, 1.0f, -1, -1, "left", false);
		}

		private static DrawCommand line(float x1, float y1, float x2, float y2, float r, float g, float b, float a, float lineWidth) {
			return new DrawCommand(Kind.LINE, x1, y1, x2, y2, r, g, b, a, true, 0, null, null, 1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand rect(float x1, float y1, float x2, float y2, float r, float g, float b, float a, boolean filled) {
			return new DrawCommand(Kind.RECT, x1, y1, x2, y2, r, g, b, a, filled, 0, null, null, 1, 0, 0, null, 1.0f, -1, -1, "left", false);
		}

		private static DrawCommand circle(float x, float y, float radius, float r, float g, float b, float a, boolean filled, int segments, float lineWidth) {
			return new DrawCommand(Kind.CIRCLE, x, y, radius, 0.0f, r, g, b, a, filled, segments, null, null, 1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand polygon(float[] points, float r, float g, float b, float a, boolean filled, float lineWidth) {
			return new DrawCommand(Kind.POLYGON, 0.0f, 0.0f, 0.0f, 0.0f, r, g, b, a, filled, 0, Arrays.copyOf(points, points.length), null, 1, 0, 0, null, lineWidth, -1, -1, "left", false);
		}

		private static DrawCommand text(float x, float y, float r, float g, float b, float a, String text, int textScale, int textMaxWidth, int textMaxHeight, String textAlign, boolean textWrap) {
			return new DrawCommand(Kind.TEXT, x, y, 0.0f, 0.0f, r, g, b, a, true, 0, null, text, textScale, 0, 0, null, 1.0f, textMaxWidth, textMaxHeight, textAlign, textWrap);
		}

		private static DrawCommand bitmap(float x, float y, int width, int height, int[] rgbaPixels) {
			return new DrawCommand(Kind.BITMAP, x, y, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f, true, 0, null, null, 1, width, height, Arrays.copyOf(rgbaPixels, rgbaPixels.length), 1.0f, -1, -1, "left", false);
		}

		public enum Kind {
			POINT,
			LINE,
			RECT,
			CIRCLE,
			POLYGON,
			TEXT,
			BITMAP
		}
	}
}
