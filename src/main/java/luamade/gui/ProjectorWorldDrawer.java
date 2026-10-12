package luamade.gui;

import api.render.ModWorldDrawer;
import com.bulletphysics.linearmath.Transform;
import luamade.element.ElementRegistry;
import luamade.system.module.ProjectorFrame;
import luamade.system.module.ProjectorModuleContainer;
import org.lwjgl.opengl.GL11;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.debug.DebugLineRenderer;

import java.util.List;

/**
 * Renders every projector's synced {@link ProjectorFrame} in world space each
 * frame. Registered via {@code RegisterWorldDrawersEvent}; {@link #postWorldDraw}
 * is the per-frame world-space draw hook (world/camera matrices are already
 * active at this point).
 *
 * <p>The base game exposes no per-segment-piece draw event, so this enumerates
 * live projectors itself via {@link ProjectorModuleContainer#ACTIVE_CONTAINERS}.
 * Only client-side containers are rendered (in single-player the integrated
 * server holds a second container per entity — {@link ProjectorModuleContainer#isOnServer()}
 * skips it) so each hologram draws exactly once.
 *
 * <p>Unlike the terminal overlay, depth testing is left <em>enabled</em> so
 * holograms z-sort against hull and terrain; blending is enabled for alpha.
 *
 * <p>All geometry goes through the engine's {@link DebugLineRenderer} rather than
 * raw GL: the engine may run on its Vulkan backend (no GL context at all, so any
 * {@code GL11} call aborts the JVM) or a core-profile GL context (no immediate
 * mode). The batcher works on every backend and picks up the {@code GlUtil}
 * modelview when it flushes. Consequence: wide lines and large points are not
 * available there, so every stroke renders 1px regardless of {@code thickness}.
 */
public class ProjectorWorldDrawer extends ModWorldDrawer {

	private static final float EPSILON = 1.0e-6f;

	private final Transform transform = new Transform();
	private final float[] right = new float[3];
	private final float[] down = new float[3];
	private final float[] worldA = new float[3];
	private final float[] worldB = new float[3];
	private final float[] worldC = new float[3];
	private DebugLineRenderer dlr;
	private float cr, cg, cb, ca;
	// DebugLineRenderer silently drops geometry past its buffer (32768 line
	// vertices; 32768 shared by triangles and points), so we count what we've
	// queued and flush into a fresh batch before it fills.
	private static final int LINE_VERTS = 32768;
	private static final int TRI_POINT_VERTS = 32768;
	private int lineVerts, triPointVerts;

	@Override
	public void update(Timer timer) {
		// No per-frame simulation state.
	}

	@Override
	public void onInit() {
		// Nothing to initialize; state is read live from ACTIVE_CONTAINERS.
	}

	@Override
	public void cleanUp() {
		// No GPU resources are retained; DebugLineRenderer is engine-owned.
	}

	@Override
	public boolean isInvisible() {
		return false;
	}

	@Override
	public void postWorldDraw() {
		boolean stateSet = false;
		try {
			short projectorId = ElementRegistry.PROJECTOR.getId();
			for(ProjectorModuleContainer container : ProjectorModuleContainer.ACTIVE_CONTAINERS) {
				if(container == null || container.isOnServer()) {
					continue;
				}
				SegmentController segmentController = container.segmentController;
				if(segmentController == null) {
					continue;
				}
				List<ProjectorModuleContainer.FrameEntry> entries = container.copyFrames();
				if(entries.isEmpty()) {
					continue;
				}
				for(ProjectorModuleContainer.FrameEntry entry : entries) {
					if(entry.frame == null || entry.frame.isEmpty()) {
						continue;
					}
					SegmentPiece piece = segmentController.getSegmentBuffer().getPointUnsave(entry.absIndex);
					if(piece == null || piece.getType() != projectorId) {
						continue;
					}
					if(!stateSet) {
						beginState();
						stateSet = true;
					}
					piece.getTransform(transform);
					GlUtil.glPushMatrix();
					GlUtil.glMultMatrix(transform);
					drawFrame(entry.frame);
					GlUtil.glPopMatrix();
				}
			}
		} catch(Exception e) {
			// Graphics context may be lost during alt-tab / focus loss — bail cleanly.
		} finally {
			if(stateSet) {
				endState();
			}
		}
	}

	// -------------------------------------------------------------------------
	// Render state (GlUtil routes these through whichever backend is active)
	// -------------------------------------------------------------------------

	private void beginState() {
		GlUtil.glDisable(GL11.GL_TEXTURE_2D);
		GlUtil.glDisable(GL11.GL_LIGHTING);
		// Holograms should read from any angle, so filled shapes are two-sided.
		GlUtil.glDisable(GL11.GL_CULL_FACE);
		// GL_DEPTH_TEST is intentionally left enabled so holograms z-sort with world geometry.
		GlUtil.glEnable(GL11.GL_BLEND);
		GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void endState() {
		GlUtil.glDisable(GL11.GL_BLEND);
		GlUtil.glEnable(GL11.GL_CULL_FACE);
		GlUtil.glEnable(GL11.GL_TEXTURE_2D);
		GlUtil.glEnable(GL11.GL_LIGHTING);
		GlUtil.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
	}

	// -------------------------------------------------------------------------
	// Frame / primitives
	// -------------------------------------------------------------------------

	private void drawFrame(ProjectorFrame frame) {
		dlr = DebugLineRenderer.get();
		if(dlr.isDrawing()) {
			return; // someone else's batch is open this frame; skip rather than corrupt it
		}
		dlr.begin();
		lineVerts = 0;
		triPointVerts = 0;
		try {
			for(ProjectorFrame.Layer layer : frame.layers) {
				if(!layer.visible) {
					continue;
				}
				for(ProjectorFrame.Command3d command : layer.commands) {
					drawCommand3d(command);
				}
				for(ProjectorFrame.Surface surface : layer.surfaces) {
					drawSurface(surface);
				}
			}
		} finally {
			dlr.end();
		}
	}

	private void color(float r, float g, float b, float a) {
		cr = r;
		cg = g;
		cb = b;
		ca = a;
	}

	private void reserve(int lines, int trisPoints) {
		if(lineVerts + lines > LINE_VERTS || triPointVerts + trisPoints > TRI_POINT_VERTS) {
			dlr.end();
			dlr.begin();
			lineVerts = 0;
			triPointVerts = 0;
		}
		lineVerts += lines;
		triPointVerts += trisPoints;
	}

	private void line(float ax, float ay, float az, float bx, float by, float bz) {
		reserve(2, 0);
		dlr.line(ax, ay, az, bx, by, bz, cr, cg, cb, ca);
	}

	private void tri(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
		reserve(0, 3);
		dlr.tri(ax, ay, az, bx, by, bz, cx, cy, cz, cr, cg, cb, ca);
	}

	private void quad(float ax, float ay, float az, float bx, float by, float bz,
	                  float cx, float cy, float cz, float dx, float dy, float dz) {
		reserve(0, 6);
		dlr.quad(ax, ay, az, bx, by, bz, cx, cy, cz, dx, dy, dz, cr, cg, cb, ca);
	}

	private void point(float x, float y, float z) {
		reserve(0, 1);
		dlr.point(x, y, z, cr, cg, cb, ca);
	}

	private void drawCommand3d(ProjectorFrame.Command3d c) {
		color(c.r, c.g, c.b, c.a);
		switch(c.kind) {
			case POINT:
				point(c.x1, c.y1, c.z1);
				break;
			case LINE:
				line(c.x1, c.y1, c.z1, c.x2, c.y2, c.z2);
				break;
			case BOX_WIRE:
				drawBoxWire(c);
				break;
			case BOX_FILLED:
				drawBoxFilled(c);
				break;
			case POLYLINE:
				drawPolyline(c);
				break;
			case TRIANGLE:
				if(c.filled) {
					tri(c.x1, c.y1, c.z1, c.x2, c.y2, c.z2, c.x3, c.y3, c.z3);
				} else {
					line(c.x1, c.y1, c.z1, c.x2, c.y2, c.z2);
					line(c.x2, c.y2, c.z2, c.x3, c.y3, c.z3);
					line(c.x3, c.y3, c.z3, c.x1, c.y1, c.z1);
				}
				break;
		}
	}

	private void drawBoxWire(ProjectorFrame.Command3d c) {
		float ox = c.x1, oy = c.y1, oz = c.z1;
		float fx = c.x1 + c.x2, fy = c.y1 + c.y2, fz = c.z1 + c.z2;
		// bottom rectangle (z = oz)
		line(ox, oy, oz, fx, oy, oz);
		line(fx, oy, oz, fx, fy, oz);
		line(fx, fy, oz, ox, fy, oz);
		line(ox, fy, oz, ox, oy, oz);
		// top rectangle (z = fz)
		line(ox, oy, fz, fx, oy, fz);
		line(fx, oy, fz, fx, fy, fz);
		line(fx, fy, fz, ox, fy, fz);
		line(ox, fy, fz, ox, oy, fz);
		// vertical struts
		line(ox, oy, oz, ox, oy, fz);
		line(fx, oy, oz, fx, oy, fz);
		line(fx, fy, oz, fx, fy, fz);
		line(ox, fy, oz, ox, fy, fz);
	}

	private void drawBoxFilled(ProjectorFrame.Command3d c) {
		float ox = c.x1, oy = c.y1, oz = c.z1;
		float fx = c.x1 + c.x2, fy = c.y1 + c.y2, fz = c.z1 + c.z2;
		// -z / +z
		quad(ox, oy, oz, fx, oy, oz, fx, fy, oz, ox, fy, oz);
		quad(ox, oy, fz, fx, oy, fz, fx, fy, fz, ox, fy, fz);
		// -y / +y
		quad(ox, oy, oz, fx, oy, oz, fx, oy, fz, ox, oy, fz);
		quad(ox, fy, oz, fx, fy, oz, fx, fy, fz, ox, fy, fz);
		// -x / +x
		quad(ox, oy, oz, ox, fy, oz, ox, fy, fz, ox, oy, fz);
		quad(fx, oy, oz, fx, fy, oz, fx, fy, fz, fx, oy, fz);
	}

	private void drawPolyline(ProjectorFrame.Command3d c) {
		float[] p = c.points;
		if(p == null || p.length < 6) {
			return;
		}
		int n = p.length / 3;
		for(int i = 0; i + 1 < n; i++) {
			int a = i * 3;
			line(p[a], p[a + 1], p[a + 2], p[a + 3], p[a + 4], p[a + 5]);
		}
		if(c.closed && n > 2) {
			int last = (n - 1) * 3;
			line(p[last], p[last + 1], p[last + 2], p[0], p[1], p[2]);
		}
	}

	// -------------------------------------------------------------------------
	// Surfaces (2D canvas mapped onto an oriented plane)
	// -------------------------------------------------------------------------

	private void drawSurface(ProjectorFrame.Surface s) {
		// right = normalize(up × n); down = normalize(right × n). Canvas (0,0) is
		// the surface origin corner, +x runs along right, +y runs "down" the plane.
		if(!computeBasis(s)) {
			return; // degenerate: up parallel to normal.
		}
		for(ProjectorFrame.Command2d c : s.commands) {
			drawCommand2d(s, c);
		}
	}

	private boolean computeBasis(ProjectorFrame.Surface s) {
		float nx = s.nx, ny = s.ny, nz = s.nz;
		float nlen = length(nx, ny, nz);
		if(!(nlen > EPSILON)) { // also rejects NaN
			return false;
		}
		nx /= nlen; ny /= nlen; nz /= nlen;
		// right = up × n
		float rx = s.uy * nz - s.uz * ny;
		float ry = s.uz * nx - s.ux * nz;
		float rz = s.ux * ny - s.uy * nx;
		float rlen = length(rx, ry, rz);
		if(!(rlen > EPSILON)) { // also rejects NaN
			return false;
		}
		rx /= rlen; ry /= rlen; rz /= rlen;
		// down = right × n
		float dx = ry * nz - rz * ny;
		float dy = rz * nx - rx * nz;
		float dz = rx * ny - ry * nx;
		float dlen = length(dx, dy, dz);
		if(!(dlen > EPSILON)) { // also rejects NaN
			return false;
		}
		right[0] = rx; right[1] = ry; right[2] = rz;
		down[0] = dx / dlen; down[1] = dy / dlen; down[2] = dz / dlen;
		return true;
	}

	private void toWorld(ProjectorFrame.Surface s, float cx, float cy, float[] out) {
		float u = (cx / s.canvasW) * s.worldW;
		float v = (cy / s.canvasH) * s.worldH;
		out[0] = s.ox + u * right[0] + v * down[0];
		out[1] = s.oy + u * right[1] + v * down[1];
		out[2] = s.oz + u * right[2] + v * down[2];
	}

	private void line2d(ProjectorFrame.Surface s, float x1, float y1, float x2, float y2) {
		toWorld(s, x1, y1, worldA);
		toWorld(s, x2, y2, worldB);
		line(worldA[0], worldA[1], worldA[2], worldB[0], worldB[1], worldB[2]);
	}

	private void tri2d(ProjectorFrame.Surface s, float x1, float y1, float x2, float y2, float x3, float y3) {
		toWorld(s, x1, y1, worldA);
		toWorld(s, x2, y2, worldB);
		toWorld(s, x3, y3, worldC);
		tri(worldA[0], worldA[1], worldA[2], worldB[0], worldB[1], worldB[2], worldC[0], worldC[1], worldC[2]);
	}

	private void rect2d(ProjectorFrame.Surface s, float x0, float y0, float x1, float y1) {
		tri2d(s, x0, y0, x1, y0, x1, y1);
		tri2d(s, x0, y0, x1, y1, x0, y1);
	}

	private void drawCommand2d(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		color(c.r, c.g, c.b, c.a);
		switch(c.kind) {
			case POINT:
				toWorld(s, c.x1, c.y1, worldA);
				point(worldA[0], worldA[1], worldA[2]);
				break;
			case LINE:
				line2d(s, c.x1, c.y1, c.x2, c.y2);
				break;
			case RECT: {
				float x0 = c.x1, y0 = c.y1, x1 = c.x1 + c.x2, y1 = c.y1 + c.y2;
				if(c.filled) {
					rect2d(s, x0, y0, x1, y1);
				} else {
					line2d(s, x0, y0, x1, y0);
					line2d(s, x1, y0, x1, y1);
					line2d(s, x1, y1, x0, y1);
					line2d(s, x0, y1, x0, y0);
				}
				break;
			}
			case CIRCLE:
				drawSurfaceCircle(s, c);
				break;
			case POLYGON:
				drawSurfacePolygon(s, c);
				break;
			case TEXT:
				drawSurfaceText(s, c);
				break;
		}
	}

	private void drawSurfaceCircle(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		int segments = Math.max(3, c.segments);
		float radius = c.x2;
		if(radius <= 0.0f) {
			return;
		}
		float px = c.x1 + radius, py = c.y1;
		for(int i = 1; i <= segments; i++) {
			double angle = (Math.PI * 2.0 * i) / segments;
			float x = c.x1 + (float) Math.cos(angle) * radius;
			float y = c.y1 + (float) Math.sin(angle) * radius;
			if(c.filled) {
				tri2d(s, c.x1, c.y1, px, py, x, y);
			} else {
				line2d(s, px, py, x, y);
			}
			px = x;
			py = y;
		}
	}

	private void drawSurfacePolygon(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		float[] p = c.points;
		if(p == null || p.length < 6 || (p.length % 2) != 0) {
			return;
		}
		int n = p.length / 2;
		if(c.filled) {
			// Fan from the first vertex: the same convex-polygon contract GL_POLYGON had.
			for(int i = 1; i + 1 < n; i++) {
				tri2d(s, p[0], p[1], p[i * 2], p[i * 2 + 1], p[i * 2 + 2], p[i * 2 + 3]);
			}
		} else {
			for(int i = 0; i < n; i++) {
				int j = (i + 1) % n;
				line2d(s, p[i * 2], p[i * 2 + 1], p[j * 2], p[j * 2 + 1]);
			}
		}
	}

	private void drawSurfaceText(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		if(c.text == null || c.text.isEmpty()) {
			return;
		}
		float pixel = Math.max(1, c.textScale);
		float cursorX = c.x1;
		float cursorY = c.y1;
		for(int i = 0; i < c.text.length(); i++) {
			char ch = c.text.charAt(i);
			if(ch == '\n') {
				cursorX = c.x1;
				cursorY += HoloFont.LINE_ADVANCE * pixel;
				continue;
			}
			int[] glyph = HoloFont.glyph(ch);
			for(int row = 0; row < HoloFont.GLYPH_HEIGHT; row++) {
				// One quad per horizontal run of lit pixels rather than per pixel.
				int col = 0;
				while(col < HoloFont.GLYPH_WIDTH) {
					if(!HoloFont.pixel(glyph, col, row)) {
						col++;
						continue;
					}
					int end = col;
					while(end < HoloFont.GLYPH_WIDTH && HoloFont.pixel(glyph, end, row)) {
						end++;
					}
					float py = cursorY + row * pixel;
					rect2d(s, cursorX + col * pixel, py, cursorX + end * pixel, py + pixel);
					col = end;
				}
			}
			cursorX += HoloFont.ADVANCE * pixel;
		}
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
}
