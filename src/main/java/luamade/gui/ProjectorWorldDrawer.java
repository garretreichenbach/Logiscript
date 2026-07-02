package luamade.gui;

import api.utils.draw.ModWorldDrawer;
import com.bulletphysics.linearmath.Transform;
import luamade.element.ElementRegistry;
import luamade.system.module.ProjectorFrame;
import luamade.system.module.ProjectorModuleContainer;
import org.lwjgl.opengl.GL11;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.Timer;

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
 */
public class ProjectorWorldDrawer extends ModWorldDrawer {

	private static final float EPSILON = 1.0e-6f;

	private final Transform transform = new Transform();
	private final float[] right = new float[3];
	private final float[] down = new float[3];
	private final float[] worldA = new float[3];

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
		// No GL resources are retained between frames.
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
			// OpenGL context may be lost during alt-tab / focus loss — bail cleanly.
		} finally {
			if(stateSet) {
				endState();
			}
		}
	}

	// -------------------------------------------------------------------------
	// GL state
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
		GL11.glLineWidth(1.0f);
		GL11.glPointSize(1.0f);
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
	}

	private void drawCommand3d(ProjectorFrame.Command3d c) {
		GlUtil.glColor4f(c.r, c.g, c.b, c.a);
		switch(c.kind) {
			case POINT:
				GL11.glPointSize(Math.max(1.0f, c.thickness * 2.0f));
				GL11.glBegin(GL11.GL_POINTS);
				GL11.glVertex3f(c.x1, c.y1, c.z1);
				GL11.glEnd();
				GL11.glPointSize(1.0f);
				break;
			case LINE:
				GL11.glLineWidth(Math.max(1.0f, c.thickness));
				GL11.glBegin(GL11.GL_LINES);
				GL11.glVertex3f(c.x1, c.y1, c.z1);
				GL11.glVertex3f(c.x2, c.y2, c.z2);
				GL11.glEnd();
				GL11.glLineWidth(1.0f);
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
					GL11.glBegin(GL11.GL_TRIANGLES);
				} else {
					GL11.glLineWidth(Math.max(1.0f, c.thickness));
					GL11.glBegin(GL11.GL_LINE_LOOP);
				}
				GL11.glVertex3f(c.x1, c.y1, c.z1);
				GL11.glVertex3f(c.x2, c.y2, c.z2);
				GL11.glVertex3f(c.x3, c.y3, c.z3);
				GL11.glEnd();
				if(!c.filled) {
					GL11.glLineWidth(1.0f);
				}
				break;
		}
	}

	private void drawBoxWire(ProjectorFrame.Command3d c) {
		float ox = c.x1, oy = c.y1, oz = c.z1;
		float fx = c.x1 + c.x2, fy = c.y1 + c.y2, fz = c.z1 + c.z2;
		GL11.glLineWidth(Math.max(1.0f, c.thickness));
		GL11.glBegin(GL11.GL_LINES);
		// bottom rectangle (z = oz)
		edge(ox, oy, oz, fx, oy, oz);
		edge(fx, oy, oz, fx, fy, oz);
		edge(fx, fy, oz, ox, fy, oz);
		edge(ox, fy, oz, ox, oy, oz);
		// top rectangle (z = fz)
		edge(ox, oy, fz, fx, oy, fz);
		edge(fx, oy, fz, fx, fy, fz);
		edge(fx, fy, fz, ox, fy, fz);
		edge(ox, fy, fz, ox, oy, fz);
		// vertical struts
		edge(ox, oy, oz, ox, oy, fz);
		edge(fx, oy, oz, fx, oy, fz);
		edge(fx, fy, oz, fx, fy, fz);
		edge(ox, fy, oz, ox, fy, fz);
		GL11.glEnd();
		GL11.glLineWidth(1.0f);
	}

	private void drawBoxFilled(ProjectorFrame.Command3d c) {
		float ox = c.x1, oy = c.y1, oz = c.z1;
		float fx = c.x1 + c.x2, fy = c.y1 + c.y2, fz = c.z1 + c.z2;
		GL11.glBegin(GL11.GL_QUADS);
		// -z / +z
		quad(ox, oy, oz, fx, oy, oz, fx, fy, oz, ox, fy, oz);
		quad(ox, oy, fz, fx, oy, fz, fx, fy, fz, ox, fy, fz);
		// -y / +y
		quad(ox, oy, oz, fx, oy, oz, fx, oy, fz, ox, oy, fz);
		quad(ox, fy, oz, fx, fy, oz, fx, fy, fz, ox, fy, fz);
		// -x / +x
		quad(ox, oy, oz, ox, fy, oz, ox, fy, fz, ox, oy, fz);
		quad(fx, oy, oz, fx, fy, oz, fx, fy, fz, fx, oy, fz);
		GL11.glEnd();
	}

	private void drawPolyline(ProjectorFrame.Command3d c) {
		if(c.points == null || c.points.length < 6) {
			return;
		}
		GL11.glLineWidth(Math.max(1.0f, c.thickness));
		GL11.glBegin(c.closed ? GL11.GL_LINE_LOOP : GL11.GL_LINE_STRIP);
		for(int i = 0; i + 2 < c.points.length; i += 3) {
			GL11.glVertex3f(c.points[i], c.points[i + 1], c.points[i + 2]);
		}
		GL11.glEnd();
		GL11.glLineWidth(1.0f);
	}

	private void edge(float ax, float ay, float az, float bx, float by, float bz) {
		GL11.glVertex3f(ax, ay, az);
		GL11.glVertex3f(bx, by, bz);
	}

	private void quad(float ax, float ay, float az, float bx, float by, float bz,
	                  float cx, float cy, float cz, float dx, float dy, float dz) {
		GL11.glVertex3f(ax, ay, az);
		GL11.glVertex3f(bx, by, bz);
		GL11.glVertex3f(cx, cy, cz);
		GL11.glVertex3f(dx, dy, dz);
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

	private void vertex(ProjectorFrame.Surface s, float cx, float cy) {
		toWorld(s, cx, cy, worldA);
		GL11.glVertex3f(worldA[0], worldA[1], worldA[2]);
	}

	private void drawCommand2d(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		GlUtil.glColor4f(c.r, c.g, c.b, c.a);
		switch(c.kind) {
			case POINT:
				GL11.glPointSize(2.0f);
				GL11.glBegin(GL11.GL_POINTS);
				vertex(s, c.x1, c.y1);
				GL11.glEnd();
				GL11.glPointSize(1.0f);
				break;
			case LINE:
				GL11.glLineWidth(Math.max(1.0f, c.thickness));
				GL11.glBegin(GL11.GL_LINES);
				vertex(s, c.x1, c.y1);
				vertex(s, c.x2, c.y2);
				GL11.glEnd();
				GL11.glLineWidth(1.0f);
				break;
			case RECT: {
				float x0 = c.x1, y0 = c.y1, x1 = c.x1 + c.x2, y1 = c.y1 + c.y2;
				if(c.filled) {
					GL11.glBegin(GL11.GL_QUADS);
				} else {
					GL11.glLineWidth(Math.max(1.0f, c.thickness));
					GL11.glBegin(GL11.GL_LINE_LOOP);
				}
				vertex(s, x0, y0);
				vertex(s, x1, y0);
				vertex(s, x1, y1);
				vertex(s, x0, y1);
				GL11.glEnd();
				if(!c.filled) {
					GL11.glLineWidth(1.0f);
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
		if(c.filled) {
			GL11.glBegin(GL11.GL_TRIANGLE_FAN);
			vertex(s, c.x1, c.y1);
			for(int i = 0; i <= segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				vertex(s, c.x1 + (float) Math.cos(angle) * radius, c.y1 + (float) Math.sin(angle) * radius);
			}
			GL11.glEnd();
		} else {
			GL11.glLineWidth(Math.max(1.0f, c.thickness));
			GL11.glBegin(GL11.GL_LINE_LOOP);
			for(int i = 0; i < segments; i++) {
				double angle = (Math.PI * 2.0 * i) / segments;
				vertex(s, c.x1 + (float) Math.cos(angle) * radius, c.y1 + (float) Math.sin(angle) * radius);
			}
			GL11.glEnd();
			GL11.glLineWidth(1.0f);
		}
	}

	private void drawSurfacePolygon(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		if(c.points == null || c.points.length < 6 || (c.points.length % 2) != 0) {
			return;
		}
		if(c.filled) {
			GL11.glBegin(GL11.GL_POLYGON);
		} else {
			GL11.glLineWidth(Math.max(1.0f, c.thickness));
			GL11.glBegin(GL11.GL_LINE_LOOP);
		}
		for(int i = 0; i + 1 < c.points.length; i += 2) {
			vertex(s, c.points[i], c.points[i + 1]);
		}
		GL11.glEnd();
		if(!c.filled) {
			GL11.glLineWidth(1.0f);
		}
	}

	private void drawSurfaceText(ProjectorFrame.Surface s, ProjectorFrame.Command2d c) {
		if(c.text == null || c.text.isEmpty()) {
			return;
		}
		float pixel = Math.max(1, c.textScale);
		float cursorX = c.x1;
		float cursorY = c.y1;
		GL11.glBegin(GL11.GL_QUADS);
		for(int i = 0; i < c.text.length(); i++) {
			char ch = c.text.charAt(i);
			if(ch == '\n') {
				cursorX = c.x1;
				cursorY += HoloFont.LINE_ADVANCE * pixel;
				continue;
			}
			int[] glyph = HoloFont.glyph(ch);
			for(int row = 0; row < HoloFont.GLYPH_HEIGHT; row++) {
				for(int col = 0; col < HoloFont.GLYPH_WIDTH; col++) {
					if(!HoloFont.pixel(glyph, col, row)) {
						continue;
					}
					float px = cursorX + col * pixel;
					float py = cursorY + row * pixel;
					vertex(s, px, py);
					vertex(s, px + pixel, py);
					vertex(s, px + pixel, py + pixel);
					vertex(s, px, py + pixel);
				}
			}
			cursorX += HoloFont.ADVANCE * pixel;
		}
		GL11.glEnd();
	}

	private static float length(float x, float y, float z) {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}
}
