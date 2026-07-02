package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.manager.ConfigManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Immutable, server-authoritative snapshot of everything a projector renders in
 * world space. This is the value type that {@link ProjectorModuleContainer}
 * stores per block; it doubles as the sync payload (written inside
 * {@code onTagSerialize} → {@code PacketSCSyncMCModule}) <em>and</em> the
 * world-save persistence format — the same single tag path that drives
 * {@link VaultModuleContainer}.
 *
 * <p>Mirrors {@link luamade.lua.gfx.Gfx2d.FrameSnapshot}: public-final fields,
 * private constructor, defensive copies, and a {@link luamade.lua.gfx.Gfx3d}
 * that builds one via {@code snapshot()}. Coordinates are block-space floats
 * relative to the projector and are clamped to
 * {@code ±projector_max_offset_blocks} on both build and read (authoritative).
 *
 * <p>Wire format is versioned by the enclosing container; this type only writes
 * its own body.
 */
public final class ProjectorFrame {

	public final long revision;
	public final long publishedAtMs;
	public final List<Layer> layers;

	private ProjectorFrame(long revision, long publishedAtMs, List<Layer> layers) {
		this.revision = revision;
		this.publishedAtMs = publishedAtMs;
		this.layers = layers;
	}

	/** Builds a clamped/capped frame from raw layer data (used by {@code Gfx3d.snapshot()}). */
	public static ProjectorFrame build(long revision, long publishedAtMs, List<Layer> rawLayers) {
		List<Layer> sanitized = new ArrayList<>();
		int commandBudget = ConfigManager.getProjectorMaxCommandsPerFrame();
		int surfaceBudget = ConfigManager.getProjectorMaxSurfacesPerFrame();
		if(rawLayers != null) {
			for(Layer layer : rawLayers) {
				Layer kept = sanitizeLayer(layer, commandBudget, surfaceBudget);
				sanitized.add(kept);
				// Decrement by what was actually kept so the running budget never
				// goes negative and later layers see the true remaining headroom.
				commandBudget -= kept.commands.size();
				surfaceBudget -= kept.surfaces.size();
			}
		}
		return new ProjectorFrame(revision, publishedAtMs, sanitized);
	}

	/** Reconstructs a frame received over the network / loaded from disk (authoritative clamp + caps). */
	public static ProjectorFrame readFrom(PacketReadBuffer buffer) throws IOException {
		long revision = buffer.readLong();
		long publishedAtMs = buffer.readLong();
		int layerCount = buffer.readInt();
		List<Layer> rawLayers = new ArrayList<>(Math.max(0, layerCount));
		for(int i = 0; i < layerCount; i++) {
			rawLayers.add(Layer.readFrom(buffer));
		}
		return build(revision, publishedAtMs, rawLayers);
	}

	public void writeTo(PacketWriteBuffer buffer) throws IOException {
		buffer.writeLong(revision);
		buffer.writeLong(publishedAtMs);
		buffer.writeInt(layers.size());
		for(Layer layer : layers) {
			layer.writeTo(buffer);
		}
	}

	public boolean isEmpty() {
		for(Layer layer : layers) {
			if(!layer.commands.isEmpty() || !layer.surfaces.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	private static Layer sanitizeLayer(Layer layer, int commandBudget, int surfaceBudget) {
		List<Command3d> commands = layer.commands;
		if(commandBudget < commands.size()) {
			commands = new ArrayList<>(commands.subList(0, Math.max(0, commandBudget)));
		}
		List<Surface> surfaces = layer.surfaces;
		if(surfaceBudget < surfaces.size()) {
			surfaces = new ArrayList<>(surfaces.subList(0, Math.max(0, surfaceBudget)));
		}
		return new Layer(layer.name, layer.order, layer.visible, commands, surfaces);
	}

	// -------------------------------------------------------------------------
	// Clamp helpers — single source of truth shared by Gfx3d/ProjectorSurface
	// and the authoritative read path.
	// -------------------------------------------------------------------------

	public static float clampOffset(double value) {
		float max = (float) ConfigManager.getProjectorMaxOffsetBlocks();
		return clamp(value, -max, max);
	}

	public static float clampColor(double value) {
		return clamp(value, 0.0, 1.0);
	}

	public static float clampThickness(double value) {
		return clamp(value, 1.0, 16.0);
	}

	public static float clampCanvas(double value) {
		// Canvas coordinates are logical; keep them finite and within a sane range
		// so a NaN/huge value can never blow up the plane mapping.
		return clamp(value, -100000.0, 100000.0);
	}

	/** A world-space extent (surface size / box dimension): non-negative, at most the full ±offset box. */
	public static float clampSize(double value) {
		float max = (float) ConfigManager.getProjectorMaxOffsetBlocks();
		return clamp(value, 0.0, max * 2.0);
	}

	/** A direction component (surface normal / up): any finite value, NaN/Inf mapped to 0. */
	public static float clampFinite(double value) {
		return (Double.isNaN(value) || Double.isInfinite(value)) ? 0.0f : (float) value;
	}

	private static float clamp(double value, double min, double max) {
		if(Double.isNaN(value) || Double.isInfinite(value)) {
			return (float) min;
		}
		return (float) Math.max(min, Math.min(max, value));
	}

	// -------------------------------------------------------------------------
	// Nested value types
	// -------------------------------------------------------------------------

	/** An ordered draw layer: 3D primitives + oriented 2D surfaces. */
	public static final class Layer {
		public final String name;
		public final int order;
		public final boolean visible;
		public final List<Command3d> commands;
		public final List<Surface> surfaces;

		public Layer(String name, int order, boolean visible, List<Command3d> commands, List<Surface> surfaces) {
			this.name = name == null ? "default" : name;
			this.order = order;
			this.visible = visible;
			this.commands = commands == null ? new ArrayList<>() : commands;
			this.surfaces = surfaces == null ? new ArrayList<>() : surfaces;
		}

		private void writeTo(PacketWriteBuffer buffer) throws IOException {
			buffer.writeString(name);
			buffer.writeInt(order);
			buffer.writeBoolean(visible);
			buffer.writeInt(commands.size());
			for(Command3d command : commands) {
				command.writeTo(buffer);
			}
			buffer.writeInt(surfaces.size());
			for(Surface surface : surfaces) {
				surface.writeTo(buffer);
			}
		}

		private static Layer readFrom(PacketReadBuffer buffer) throws IOException {
			String name = buffer.readString();
			int order = buffer.readInt();
			boolean visible = buffer.readBoolean();
			int commandCount = buffer.readInt();
			List<Command3d> commands = new ArrayList<>(Math.max(0, commandCount));
			for(int i = 0; i < commandCount; i++) {
				commands.add(Command3d.readFrom(buffer));
			}
			int surfaceCount = buffer.readInt();
			List<Surface> surfaces = new ArrayList<>(Math.max(0, surfaceCount));
			for(int i = 0; i < surfaceCount; i++) {
				surfaces.add(Surface.readFrom(buffer));
			}
			return new Layer(name, order, visible, commands, surfaces);
		}
	}

	/**
	 * A single 3D primitive. Union of fields keyed by {@link Kind}, mirroring
	 * {@link luamade.lua.gfx.Gfx2d.DrawCommand}. Positional fields are already
	 * clamped to the ±offset box; {@code points} holds flattened xyz triples for
	 * variable-length primitives (polyline).
	 */
	public static final class Command3d {
		public enum Kind {POINT, LINE, BOX_WIRE, BOX_FILLED, POLYLINE, TRIANGLE}

		public final Kind kind;
		public final float x1, y1, z1;
		public final float x2, y2, z2;
		public final float x3, y3, z3;
		public final float r, g, b, a;
		public final float thickness;
		public final boolean filled;
		public final boolean closed;
		public final float[] points;

		public Command3d(Kind kind, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3,
		                 float r, float g, float b, float a, float thickness, boolean filled, boolean closed, float[] points) {
			this.kind = kind;
			this.x1 = x1; this.y1 = y1; this.z1 = z1;
			this.x2 = x2; this.y2 = y2; this.z2 = z2;
			this.x3 = x3; this.y3 = y3; this.z3 = z3;
			this.r = r; this.g = g; this.b = b; this.a = a;
			this.thickness = thickness;
			this.filled = filled;
			this.closed = closed;
			this.points = points;
		}

		private void writeTo(PacketWriteBuffer buffer) throws IOException {
			buffer.writeByte((byte) kind.ordinal());
			buffer.writeFloat(x1); buffer.writeFloat(y1); buffer.writeFloat(z1);
			buffer.writeFloat(x2); buffer.writeFloat(y2); buffer.writeFloat(z2);
			buffer.writeFloat(x3); buffer.writeFloat(y3); buffer.writeFloat(z3);
			buffer.writeFloat(r); buffer.writeFloat(g); buffer.writeFloat(b); buffer.writeFloat(a);
			buffer.writeFloat(thickness);
			buffer.writeBoolean(filled);
			buffer.writeBoolean(closed);
			writeFloatArray(buffer, points);
		}

		private static Command3d readFrom(PacketReadBuffer buffer) throws IOException {
			Kind kind = kindFromOrdinal(buffer.readByte());
			float x1 = clampOffset(buffer.readFloat()), y1 = clampOffset(buffer.readFloat()), z1 = clampOffset(buffer.readFloat());
			float x2 = clampOffset(buffer.readFloat()), y2 = clampOffset(buffer.readFloat()), z2 = clampOffset(buffer.readFloat());
			float x3 = clampOffset(buffer.readFloat()), y3 = clampOffset(buffer.readFloat()), z3 = clampOffset(buffer.readFloat());
			float r = clampColor(buffer.readFloat()), g = clampColor(buffer.readFloat()), b = clampColor(buffer.readFloat()), a = clampColor(buffer.readFloat());
			float thickness = clampThickness(buffer.readFloat());
			boolean filled = buffer.readBoolean();
			boolean closed = buffer.readBoolean();
			float[] points = readOffsetArray(buffer);
			return new Command3d(kind, x1, y1, z1, x2, y2, z2, x3, y3, z3, r, g, b, a, thickness, filled, closed, points);
		}

		private static Kind kindFromOrdinal(byte ordinal) {
			Kind[] values = Kind.values();
			int index = ordinal & 0xFF;
			return index >= 0 && index < values.length ? values[index] : Kind.POINT;
		}
	}

	/**
	 * An oriented 2D vector canvas floating in 3D. Defined by an origin offset
	 * (block-space, relative to the projector), a normal + up vector, the world
	 * size of the plane, and a logical canvas size that 2D commands are authored
	 * against. Canvas (x, y) maps to world via
	 * {@code origin + (x/canvasW)*worldW*right + (y/canvasH)*worldH*up} where
	 * {@code right = normalize(normal × up)}.
	 */
	public static final class Surface {
		public final float ox, oy, oz;
		public final float nx, ny, nz;
		public final float ux, uy, uz;
		public final float worldW, worldH;
		public final float canvasW, canvasH;
		public final List<Command2d> commands;

		public Surface(float ox, float oy, float oz, float nx, float ny, float nz, float ux, float uy, float uz,
		               float worldW, float worldH, float canvasW, float canvasH, List<Command2d> commands) {
			this.ox = ox; this.oy = oy; this.oz = oz;
			this.nx = nx; this.ny = ny; this.nz = nz;
			this.ux = ux; this.uy = uy; this.uz = uz;
			this.worldW = worldW; this.worldH = worldH;
			this.canvasW = canvasW; this.canvasH = canvasH;
			this.commands = commands == null ? new ArrayList<>() : commands;
		}

		private void writeTo(PacketWriteBuffer buffer) throws IOException {
			buffer.writeFloat(ox); buffer.writeFloat(oy); buffer.writeFloat(oz);
			buffer.writeFloat(nx); buffer.writeFloat(ny); buffer.writeFloat(nz);
			buffer.writeFloat(ux); buffer.writeFloat(uy); buffer.writeFloat(uz);
			buffer.writeFloat(worldW); buffer.writeFloat(worldH);
			buffer.writeFloat(canvasW); buffer.writeFloat(canvasH);
			buffer.writeInt(commands.size());
			for(Command2d command : commands) {
				command.writeTo(buffer);
			}
		}

		private static Surface readFrom(PacketReadBuffer buffer) throws IOException {
			float ox = clampOffset(buffer.readFloat()), oy = clampOffset(buffer.readFloat()), oz = clampOffset(buffer.readFloat());
			float nx = clampFinite(buffer.readFloat()), ny = clampFinite(buffer.readFloat()), nz = clampFinite(buffer.readFloat());
			float ux = clampFinite(buffer.readFloat()), uy = clampFinite(buffer.readFloat()), uz = clampFinite(buffer.readFloat());
			float worldW = clampSize(buffer.readFloat()), worldH = clampSize(buffer.readFloat());
			float canvasW = Math.max(1.0f, clampCanvas(buffer.readFloat()));
			float canvasH = Math.max(1.0f, clampCanvas(buffer.readFloat()));
			int commandCount = buffer.readInt();
			int cap = ConfigManager.getProjectorMaxCommandsPerSurface();
			List<Command2d> commands = new ArrayList<>(Math.max(0, Math.min(commandCount, cap)));
			for(int i = 0; i < commandCount; i++) {
				Command2d command = Command2d.readFrom(buffer);
				if(commands.size() < cap) {
					commands.add(command);
				}
			}
			return new Surface(ox, oy, oz, nx, ny, nz, ux, uy, uz, worldW, worldH, canvasW, canvasH, commands);
		}
	}

	/**
	 * A single 2D primitive authored in surface-canvas coordinates. Mirrors
	 * {@link luamade.lua.gfx.Gfx2d.DrawCommand} (minus bitmap): {@code x1,y1}
	 * position, {@code x2,y2} second point / size / radius depending on kind.
	 */
	public static final class Command2d {
		public enum Kind {POINT, LINE, RECT, CIRCLE, POLYGON, TEXT}

		public final Kind kind;
		public final float x1, y1, x2, y2;
		public final float r, g, b, a;
		public final boolean filled;
		public final int segments;
		public final float thickness;
		public final float[] points;
		public final String text;
		public final int textScale;

		public Command2d(Kind kind, float x1, float y1, float x2, float y2, float r, float g, float b, float a,
		                 boolean filled, int segments, float thickness, float[] points, String text, int textScale) {
			this.kind = kind;
			this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
			this.r = r; this.g = g; this.b = b; this.a = a;
			this.filled = filled;
			this.segments = segments;
			this.thickness = thickness;
			this.points = points;
			this.text = text == null ? "" : text;
			this.textScale = textScale;
		}

		private void writeTo(PacketWriteBuffer buffer) throws IOException {
			buffer.writeByte((byte) kind.ordinal());
			buffer.writeFloat(x1); buffer.writeFloat(y1); buffer.writeFloat(x2); buffer.writeFloat(y2);
			buffer.writeFloat(r); buffer.writeFloat(g); buffer.writeFloat(b); buffer.writeFloat(a);
			buffer.writeBoolean(filled);
			buffer.writeInt(segments);
			buffer.writeFloat(thickness);
			writeFloatArray(buffer, points);
			buffer.writeString(text);
			buffer.writeInt(textScale);
		}

		private static Command2d readFrom(PacketReadBuffer buffer) throws IOException {
			Kind kind = kindFromOrdinal(buffer.readByte());
			float x1 = clampCanvas(buffer.readFloat()), y1 = clampCanvas(buffer.readFloat());
			float x2 = clampCanvas(buffer.readFloat()), y2 = clampCanvas(buffer.readFloat());
			float r = clampColor(buffer.readFloat()), g = clampColor(buffer.readFloat()), b = clampColor(buffer.readFloat()), a = clampColor(buffer.readFloat());
			boolean filled = buffer.readBoolean();
			int segments = Math.max(3, Math.min(256, buffer.readInt()));
			float thickness = clampThickness(buffer.readFloat());
			float[] points = readCanvasArray(buffer);
			String text = buffer.readString();
			int textScale = Math.max(1, Math.min(64, buffer.readInt()));
			return new Command2d(kind, x1, y1, x2, y2, r, g, b, a, filled, segments, thickness, points, text, textScale);
		}

		private static Kind kindFromOrdinal(byte ordinal) {
			Kind[] values = Kind.values();
			int index = ordinal & 0xFF;
			return index >= 0 && index < values.length ? values[index] : Kind.POINT;
		}
	}

	// -------------------------------------------------------------------------
	// Float-array wire helpers (no built-in float[] codec on the buffer)
	// -------------------------------------------------------------------------

	private static void writeFloatArray(PacketWriteBuffer buffer, float[] array) throws IOException {
		if(array == null) {
			buffer.writeInt(0);
			return;
		}
		buffer.writeInt(array.length);
		for(float value : array) {
			buffer.writeFloat(value);
		}
	}

	private static float[] readOffsetArray(PacketReadBuffer buffer) throws IOException {
		int length = Math.max(0, Math.min(buffer.readInt(), 3 * 4096));
		float[] array = new float[length];
		for(int i = 0; i < length; i++) {
			array[i] = clampOffset(buffer.readFloat());
		}
		return array;
	}

	private static float[] readCanvasArray(PacketReadBuffer buffer) throws IOException {
		int length = Math.max(0, Math.min(buffer.readInt(), 2 * 8192));
		float[] array = new float[length];
		for(int i = 0; i < length; i++) {
			array[i] = clampCanvas(buffer.readFloat());
		}
		return array;
	}
}
