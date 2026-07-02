package luamade.lua.gfx;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import luamade.system.module.ProjectorFrame;
import org.luaj.vm2.LuaError;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * World-space 3D vector graphics API exposed to Lua scripts, driven through a
 * {@code projector} peripheral. Mirrors {@link Gfx2d} in structure — thread-safe
 * under a single lock, layered, immutable command records, {@code snapshot()}
 * builds an immutable {@link ProjectorFrame} — but coordinates are block-space
 * floats relative to the projector block (clamped to
 * {@code ±projector_max_offset_blocks}) rather than terminal pixels.
 *
 * <p>A {@code Gfx3d} is a detached scratch frame: a script builds one with
 * {@code proj.newFrame()}, draws into it, then hands it to {@code proj.publish()}.
 * The frame only becomes visible when published, so publishing is inherently
 * atomic — bystanders never see a half-constructed frame. {@link #beginBatch()}
 * / {@link #commitBatch()} are provided for {@code gfx2d} parity but, because
 * the in-progress frame is never rendered, they only affect the revision counter.
 */
public class Gfx3d extends LuaMadeUserdata {

	private final Object lock = new Object();
	private final Map<String, LayerState> layers = new LinkedHashMap<>();
	private String activeLayer = "default";
	private int nextLayerOrder = 1;
	private long revision;
	private boolean batching;

	public Gfx3d() {
		layers.put(activeLayer, new LayerState(0));
	}

	private static int maxCommandsPerFrame() {
		return ConfigManager.getProjectorMaxCommandsPerFrame();
	}

	private static int maxSurfacesPerFrame() {
		return ConfigManager.getProjectorMaxSurfacesPerFrame();
	}

	private static int maxLayers() {
		return ConfigManager.getProjectorMaxLayers();
	}

	// -------------------------------------------------------------------------
	// Layers (reused in spirit from gfx2d)
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public Boolean setLayer(String name) {
		String normalized = normalizeLayerName(name);
		if(normalized == null) {
			return false;
		}
		synchronized(lock) {
			if(!layers.containsKey(normalized)) {
				if(layers.size() >= maxLayers()) {
					return false;
				}
				layers.put(normalized, new LayerState(nextLayerOrder++));
			}
			activeLayer = normalized;
		}
		return true;
	}

	@LuaMadeCallable
	public Boolean createLayer(String name, Integer order) {
		String normalized = normalizeLayerName(name);
		if(normalized == null) {
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
		String normalized = normalizeLayerName(name);
		if(normalized == null || "default".equals(normalized)) {
			return false;
		}
		synchronized(lock) {
			if(layers.remove(normalized) == null) {
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
		String normalized = normalizeLayerName(name);
		if(normalized == null || visible == null) {
			return false;
		}
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
				layer.surfaces.clear();
			}
			revision++;
		}
	}

	@LuaMadeCallable
	public Boolean clearLayer(String name) {
		String normalized = normalizeLayerName(name);
		if(normalized == null) {
			return false;
		}
		synchronized(lock) {
			LayerState layer = layers.get(normalized);
			if(layer == null) {
				return false;
			}
			layer.commands.clear();
			layer.surfaces.clear();
			revision++;
		}
		return true;
	}

	@LuaMadeCallable
	public void beginBatch() {
		synchronized(lock) {
			batching = true;
		}
	}

	@LuaMadeCallable
	public void commitBatch() {
		synchronized(lock) {
			batching = false;
			revision++;
		}
	}

	// -------------------------------------------------------------------------
	// 3D primitives (positions first, then r,g,b,a, then shape args)
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public Boolean point3d(Double ox, Double oy, Double oz, Double r, Double g, Double b, Double a) {
		if(ox == null || oy == null || oz == null) {
			return false;
		}
		return append(new ProjectorFrame.Command3d(ProjectorFrame.Command3d.Kind.POINT,
				off(ox), off(oy), off(oz), 0f, 0f, 0f, 0f, 0f, 0f,
				col(r), col(g), col(b), col(a), 1f, false, false, null));
	}

	@LuaMadeCallable
	public Boolean line3d(Double x1, Double y1, Double z1, Double x2, Double y2, Double z2, Double r, Double g, Double b, Double a) {
		return line3d(x1, y1, z1, x2, y2, z2, r, g, b, a, 1.0);
	}

	@LuaMadeCallable
	public Boolean line3d(Double x1, Double y1, Double z1, Double x2, Double y2, Double z2, Double r, Double g, Double b, Double a, Double thickness) {
		if(x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null) {
			return false;
		}
		return append(new ProjectorFrame.Command3d(ProjectorFrame.Command3d.Kind.LINE,
				off(x1), off(y1), off(z1), off(x2), off(y2), off(z2), 0f, 0f, 0f,
				col(r), col(g), col(b), col(a), thick(thickness), false, false, null));
	}

	@LuaMadeCallable
	public Boolean boxWire(Double ox, Double oy, Double oz, Double w, Double h, Double d, Double r, Double g, Double b, Double a) {
		return boxWire(ox, oy, oz, w, h, d, r, g, b, a, 1.0);
	}

	@LuaMadeCallable
	public Boolean boxWire(Double ox, Double oy, Double oz, Double w, Double h, Double d, Double r, Double g, Double b, Double a, Double thickness) {
		return box(ProjectorFrame.Command3d.Kind.BOX_WIRE, ox, oy, oz, w, h, d, r, g, b, a, thickness);
	}

	@LuaMadeCallable
	public Boolean boxFilled(Double ox, Double oy, Double oz, Double w, Double h, Double d, Double r, Double g, Double b, Double a) {
		return box(ProjectorFrame.Command3d.Kind.BOX_FILLED, ox, oy, oz, w, h, d, r, g, b, a, 1.0);
	}

	private Boolean box(ProjectorFrame.Command3d.Kind kind, Double ox, Double oy, Double oz, Double w, Double h, Double d,
	                    Double r, Double g, Double b, Double a, Double thickness) {
		if(ox == null || oy == null || oz == null || w == null || h == null || d == null) {
			return false;
		}
		// Clamp both corners into the ±offset box, then store the far corner as a
		// size so the whole box is guaranteed to stay in bounds.
		float x0 = off(ox), y0 = off(oy), z0 = off(oz);
		float x1 = off(ox + w), y1 = off(oy + h), z1 = off(oz + d);
		return append(new ProjectorFrame.Command3d(kind,
				x0, y0, z0, x1 - x0, y1 - y0, z1 - z0, 0f, 0f, 0f,
				col(r), col(g), col(b), col(a), thick(thickness), kind == ProjectorFrame.Command3d.Kind.BOX_FILLED, false, null));
	}

	@LuaMadeCallable
	public Boolean polyline3d(Double[] points, Double r, Double g, Double b, Double a) {
		return polyline3d(points, r, g, b, a, 1.0, false);
	}

	@LuaMadeCallable
	public Boolean polyline3d(Double[] points, Double r, Double g, Double b, Double a, Double thickness) {
		return polyline3d(points, r, g, b, a, thickness, false);
	}

	@LuaMadeCallable
	public Boolean polyline3d(Double[] points, Double r, Double g, Double b, Double a, Double thickness, Boolean closed) {
		if(points == null || points.length < 6 || (points.length % 3) != 0) {
			return false;
		}
		float[] flat = new float[points.length];
		for(int i = 0; i < points.length; i++) {
			if(points[i] == null) {
				return false;
			}
			flat[i] = off(points[i]);
		}
		return append(new ProjectorFrame.Command3d(ProjectorFrame.Command3d.Kind.POLYLINE,
				0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f,
				col(r), col(g), col(b), col(a), thick(thickness), false, closed != null && closed, flat));
	}

	@LuaMadeCallable
	public Boolean triangle3d(Double x1, Double y1, Double z1, Double x2, Double y2, Double z2, Double x3, Double y3, Double z3, Double r, Double g, Double b, Double a) {
		return triangle3d(x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a, false);
	}

	@LuaMadeCallable
	public Boolean triangle3d(Double x1, Double y1, Double z1, Double x2, Double y2, Double z2, Double x3, Double y3, Double z3, Double r, Double g, Double b, Double a, Boolean filled) {
		if(x1 == null || y1 == null || z1 == null || x2 == null || y2 == null || z2 == null || x3 == null || y3 == null || z3 == null) {
			return false;
		}
		return append(new ProjectorFrame.Command3d(ProjectorFrame.Command3d.Kind.TRIANGLE,
				off(x1), off(y1), off(z1), off(x2), off(y2), off(z2), off(x3), off(y3), off(z3),
				col(r), col(g), col(b), col(a), 1f, filled != null && filled, false, null));
	}

	// -------------------------------------------------------------------------
	// 2D surfaces in 3D
	// -------------------------------------------------------------------------

	@LuaMadeCallable
	public ProjectorSurface newSurface(Double ox, Double oy, Double oz, Double nx, Double ny, Double nz, Double ux, Double uy, Double uz, Double worldW, Double worldH) {
		return newSurface(ox, oy, oz, nx, ny, nz, ux, uy, uz, worldW, worldH, 128.0, 128.0);
	}

	@LuaMadeCallable
	public ProjectorSurface newSurface(Double ox, Double oy, Double oz, Double nx, Double ny, Double nz, Double ux, Double uy, Double uz, Double worldW, Double worldH, Double canvasW, Double canvasH) {
		if(ox == null || oy == null || oz == null || nx == null || ny == null || nz == null || ux == null || uy == null || uz == null || worldW == null || worldH == null) {
			throw new LuaError("newSurface requires origin, normal, up, and world size");
		}
		ProjectorSurface surface = new ProjectorSurface(
				off(ox), off(oy), off(oz),
				(float) safe(nx), (float) safe(ny), (float) safe(nz),
				(float) safe(ux), (float) safe(uy), (float) safe(uz),
				ProjectorFrame.clampSize(worldW), ProjectorFrame.clampSize(worldH),
				Math.max(1f, ProjectorFrame.clampCanvas(canvasW == null ? 128.0 : canvasW)),
				Math.max(1f, ProjectorFrame.clampCanvas(canvasH == null ? 128.0 : canvasH)));
		if(Thread.currentThread().isInterrupted()) {
			throw new LuaError("Script canceled");
		}
		synchronized(lock) {
			if(totalSurfacesLocked() >= maxSurfacesPerFrame()) {
				throw new LuaError("Projector surface limit reached (projector_max_surfaces_per_frame)");
			}
			LayerState layer = layers.computeIfAbsent(activeLayer, k -> new LayerState(nextLayerOrder++));
			layer.surfaces.add(surface);
			revision++;
		}
		return surface;
	}

	// -------------------------------------------------------------------------
	// Snapshot
	// -------------------------------------------------------------------------

	/** Builds an immutable, clamped/capped frame. Not exposed to Lua. */
	public ProjectorFrame snapshot() {
		synchronized(lock) {
			List<ProjectorFrame.Layer> ordered = new ArrayList<>(layers.size());
			for(Map.Entry<String, LayerState> entry : layers.entrySet()) {
				LayerState state = entry.getValue();
				List<ProjectorFrame.Surface> surfaces = new ArrayList<>(state.surfaces.size());
				for(ProjectorSurface surface : state.surfaces) {
					surfaces.add(surface.snapshot());
				}
				ordered.add(new ProjectorFrame.Layer(entry.getKey(), state.order, state.visible,
						new ArrayList<>(state.commands), surfaces));
			}
			ordered.sort(Comparator.comparingInt(layer -> layer.order));
			return ProjectorFrame.build(revision, System.currentTimeMillis(), ordered);
		}
	}

	// -------------------------------------------------------------------------
	// Internals
	// -------------------------------------------------------------------------

	private Boolean append(ProjectorFrame.Command3d command) {
		if(Thread.currentThread().isInterrupted()) {
			throw new LuaError("Script canceled");
		}
		synchronized(lock) {
			if(totalCommandsLocked() >= maxCommandsPerFrame()) {
				return false;
			}
			LayerState layer = layers.computeIfAbsent(activeLayer, k -> new LayerState(nextLayerOrder++));
			layer.commands.add(command);
			if(!batching) {
				revision++;
			}
		}
		return true;
	}

	private int totalCommandsLocked() {
		int total = 0;
		for(LayerState layer : layers.values()) {
			total += layer.commands.size();
		}
		return total;
	}

	private int totalSurfacesLocked() {
		int total = 0;
		for(LayerState layer : layers.values()) {
			total += layer.surfaces.size();
		}
		return total;
	}

	private static String normalizeLayerName(String name) {
		if(name == null) {
			return null;
		}
		String normalized = name.trim();
		if(normalized.isEmpty()) {
			return null;
		}
		return normalized.length() > 48 ? normalized.substring(0, 48) : normalized;
	}

	private static float off(double value) {
		return ProjectorFrame.clampOffset(value);
	}

	private static float col(Double value) {
		return ProjectorFrame.clampColor(value == null ? 1.0 : value);
	}

	private static float thick(Double value) {
		return ProjectorFrame.clampThickness(value == null ? 1.0 : value);
	}

	private static double safe(double value) {
		return (Double.isNaN(value) || Double.isInfinite(value)) ? 0.0 : value;
	}

	private static final class LayerState {
		private final List<ProjectorFrame.Command3d> commands = new ArrayList<>();
		private final List<ProjectorSurface> surfaces = new ArrayList<>();
		private int order;
		private boolean visible = true;

		private LayerState(int order) {
			this.order = order;
		}
	}
}
