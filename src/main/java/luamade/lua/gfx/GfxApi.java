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
		if(x1 == null || y1 == null || x2 == null || y2 == null) {
			return false;
		}
		float fx1 = (float) clampValue(x1, 0, canvasWidth - 1);
		float fy1 = (float) clampValue(y1, 0, canvasHeight - 1);
		float fx2 = (float) clampValue(x2, 0, canvasWidth - 1);
		float fy2 = (float) clampValue(y2, 0, canvasHeight - 1);
		return appendCommand(DrawCommand.line(fx1, fy1, fx2, fy2,
				toColor(r, 1.0), toColor(g, 1.0), toColor(b, 1.0), toColor(a, 1.0)));
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

	private static double clampValue(double value, double min, double max) {
		if(Double.isNaN(value) || Double.isInfinite(value)) {
			return min;
		}
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
			RECT
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

		private DrawCommand(Kind kind, float x1, float y1, float x2, float y2, float r, float g, float b, float a, boolean filled) {
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
		}

		private static DrawCommand point(float x, float y, float r, float g, float b, float a) {
			return new DrawCommand(Kind.POINT, x, y, x, y, r, g, b, a, true);
		}

		private static DrawCommand line(float x1, float y1, float x2, float y2, float r, float g, float b, float a) {
			return new DrawCommand(Kind.LINE, x1, y1, x2, y2, r, g, b, a, true);
		}

		private static DrawCommand rect(float x1, float y1, float x2, float y2, float r, float g, float b, float a, boolean filled) {
			return new DrawCommand(Kind.RECT, x1, y1, x2, y2, r, g, b, a, filled);
		}
	}
}
