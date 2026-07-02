package luamade.lua.gfx;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import luamade.system.module.ProjectorFrame;
import org.luaj.vm2.LuaError;

import java.util.ArrayList;
import java.util.List;

/**
 * A 2D vector canvas floating in 3D — the projector analogue of drawing on a
 * flat panel. Obtained via {@link Gfx3d#newSurface}. Defines an oriented plane
 * (origin offset in block-space, a normal + up vector, world size, and a logical
 * canvas size) and exposes the familiar {@code gfx2d} 2D primitives in canvas
 * coordinates.
 *
 * <p>At render time each canvas {@code (x, y)} maps to world space via
 * {@code origin + (x/canvasW)*worldW*right + (y/canvasH)*worldH*up}, where
 * {@code right = normalize(normal × up)}. Mirrors {@link Gfx2d}: thread-safe
 * under a single lock, immutable command records, {@code snapshot()} produces an
 * immutable {@link ProjectorFrame.Surface}.
 */
public class ProjectorSurface extends LuaMadeUserdata {

	private static final int MAX_TEXT_LENGTH = 256;

	private final Object lock = new Object();
	private final List<ProjectorFrame.Command2d> commands = new ArrayList<>();

	private final float ox, oy, oz;
	private final float nx, ny, nz;
	private final float ux, uy, uz;
	private final float worldW, worldH;
	private final float canvasW, canvasH;

	ProjectorSurface(float ox, float oy, float oz, float nx, float ny, float nz, float ux, float uy, float uz,
	                 float worldW, float worldH, float canvasW, float canvasH) {
		this.ox = ox; this.oy = oy; this.oz = oz;
		this.nx = nx; this.ny = ny; this.nz = nz;
		this.ux = ux; this.uy = uy; this.uz = uz;
		this.worldW = worldW; this.worldH = worldH;
		this.canvasW = canvasW; this.canvasH = canvasH;
	}

	private static int maxCommands() {
		return ConfigManager.getProjectorMaxCommandsPerSurface();
	}

	@LuaMadeCallable
	public Double getCanvasWidth() {
		return (double) canvasW;
	}

	@LuaMadeCallable
	public Double getCanvasHeight() {
		return (double) canvasH;
	}

	@LuaMadeCallable
	public Boolean point(Double x, Double y, Double r, Double g, Double b, Double a) {
		if(x == null || y == null) {
			return false;
		}
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.POINT,
				canvas(x), canvas(y), 0f, 0f, color(r), color(g), color(b), color(a),
				true, 0, 1f, null, null, 1));
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
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.LINE,
				canvas(x1), canvas(y1), canvas(x2), canvas(y2), color(r), color(g), color(b), color(a),
				false, 0, thickness(thickness), null, null, 1));
	}

	@LuaMadeCallable
	public Boolean rect(Double x, Double y, Double width, Double height, Double r, Double g, Double b, Double a, Boolean filled) {
		if(x == null || y == null || width == null || height == null) {
			return false;
		}
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.RECT,
				canvas(x), canvas(y), canvas(width), canvas(height), color(r), color(g), color(b), color(a),
				filled != null && filled, 0, 1f, null, null, 1));
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
		int seg = clampInt(segments == null ? 24 : segments, 3, 256);
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.CIRCLE,
				canvas(x), canvas(y), canvas(radius), 0f, color(r), color(g), color(b), color(a),
				filled != null && filled, seg, thickness(thickness), null, null, 1));
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
		float[] flat = new float[points.length];
		for(int i = 0; i < points.length; i++) {
			if(points[i] == null) {
				return false;
			}
			flat[i] = canvas(points[i]);
		}
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.POLYGON,
				0f, 0f, 0f, 0f, color(r), color(g), color(b), color(a),
				filled != null && filled, 0, thickness(thickness), flat, null, 1));
	}

	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a, Integer scale) {
		return text(x, y, value, r, g, b, a, scale, null, null, "left", false);
	}

	/**
	 * Extended text overload accepted for {@code gfx2d} parity. In v1 the
	 * world renderer draws left-aligned and does not wrap, so {@code maxWidth},
	 * {@code maxHeight}, {@code align}, and {@code wrap} are accepted but only
	 * {@code scale} affects the output.
	 */
	@LuaMadeCallable
	public Boolean text(Double x, Double y, String value, Double r, Double g, Double b, Double a, Integer scale,
	                    Integer maxWidth, Integer maxHeight, String align, Boolean wrap) {
		if(x == null || y == null || value == null || value.isEmpty()) {
			return false;
		}
		String normalized = value.length() > MAX_TEXT_LENGTH ? value.substring(0, MAX_TEXT_LENGTH) : value;
		int glyphScale = clampInt(scale == null ? 1 : scale, 1, 64);
		return append(new ProjectorFrame.Command2d(ProjectorFrame.Command2d.Kind.TEXT,
				canvas(x), canvas(y), 0f, 0f, color(r), color(g), color(b), color(a),
				true, 0, 1f, null, normalized, glyphScale));
	}

	/** Immutable snapshot of this surface for the frame. Not exposed to Lua. */
	public ProjectorFrame.Surface snapshot() {
		synchronized(lock) {
			return new ProjectorFrame.Surface(ox, oy, oz, nx, ny, nz, ux, uy, uz, worldW, worldH, canvasW, canvasH,
					new ArrayList<>(commands));
		}
	}

	private Boolean append(ProjectorFrame.Command2d command) {
		if(Thread.currentThread().isInterrupted()) {
			throw new LuaError("Script canceled");
		}
		synchronized(lock) {
			if(commands.size() >= maxCommands()) {
				return false;
			}
			commands.add(command);
		}
		return true;
	}

	private static float canvas(double value) {
		return ProjectorFrame.clampCanvas(value);
	}

	private static float color(Double value) {
		return ProjectorFrame.clampColor(value == null ? 1.0 : value);
	}

	private static float thickness(Double value) {
		return ProjectorFrame.clampThickness(value == null ? 1.0 : value);
	}

	private static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
