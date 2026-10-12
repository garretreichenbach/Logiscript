package luamade.gui;

import org.lwjgl.opengl.GL11;
import org.schema.schine.graphicsengine.core.GLFrame;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIAnchor;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.vbogui.GuiBatchRenderer;
import org.schema.schine.input.InputState;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Full-screen HUD for Remote Control controller mode. With a live camera feed it draws a CCTV look over the world
 * (teal tint, scanlines, vignette, grain, REC marker, camera label and clock); out of range it draws a "NO SIGNAL"
 * static screen; with no feed it only shows a slim status bar so the player can see where they are.
 *
 * <p>Geometry goes through the engine's {@link GuiBatchRenderer}, never raw GL: on the Vulkan backend (no GL
 * context) or a core-profile context any {@code GL11} draw call aborts the JVM.
 */
final class CctvOverlay extends GUIAnchor {

	enum Mode { STATUS, LIVE, NO_SIGNAL }

	private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

	private final String computerName;
	private Mode mode = Mode.STATUS;
	private GUITextOverlay label;
	private GUITextOverlay clock;
	private GUITextOverlay center;

	CctvOverlay(InputState state, String computerName) {
		super(state, GLFrame.getWidth(), GLFrame.getHeight());
		this.computerName = computerName == null || computerName.isEmpty() ? "computer" : computerName;
	}

	void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public void onInit() {
		super.onInit();
		label = text(FontLibrary.FontSize.MEDIUM);
		clock = text(FontLibrary.FontSize.MEDIUM);
		center = text(FontLibrary.FontSize.BIG);
	}

	private GUITextOverlay text(FontLibrary.FontSize size) {
		GUITextOverlay overlay = new GUITextOverlay(600, 30, size, getState());
		overlay.onInit();
		attach(overlay);
		return overlay;
	}

	@Override
	public void draw() {
		int w = GLFrame.getWidth();
		int h = GLFrame.getHeight();
		setWidth(w);
		setHeight(h);
		boolean blink = (System.currentTimeMillis() / 600) % 2 == 0;

		GlUtil.glPushMatrix();
		GlUtil.glDisable(GL11.GL_TEXTURE_2D);
		GlUtil.glDisable(GL11.GL_DEPTH_TEST);
		GlUtil.glEnable(GL11.GL_BLEND);
		GlUtil.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		batch.begin();
		try {
			switch(mode) {
				case LIVE -> {
					quad(0, 0, w, h, 0.05f, 0.30f, 0.27f, 0.16f);
					scanlines(w, h, 0.10f);
					vignette(w, h);
					grain(w, h, 350, 0.12f);
					brackets(w, h);
					if(blink) quad(28, 30, 12, 12, 0.9f, 0.1f, 0.1f, 0.9f);
				}
				case NO_SIGNAL -> {
					quad(0, 0, w, h, 0.02f, 0.03f, 0.04f, 0.94f);
					grain(w, h, 2500, 0.35f);
					scanlines(w, h, 0.25f);
				}
				case STATUS -> quad(0, 0, w, 44, 0.02f, 0.03f, 0.05f, 0.75f);
			}
		} finally {
			batch.end();
		}
		GlUtil.glDisable(GL11.GL_BLEND);
		GlUtil.glEnable(GL11.GL_TEXTURE_2D);
		GlUtil.glEnable(GL11.GL_DEPTH_TEST);
		GlUtil.glColor4f(1, 1, 1, 1);
		GlUtil.glPopMatrix();

		String hint = "Controlling " + computerName + "  -  Esc to stop";
		switch(mode) {
			case LIVE -> {
				label.setTextSimple("REC   CAM // " + computerName.toUpperCase());
				label.setPos(48, 26, 0);
				clock.setTextSimple(LocalTime.now().format(CLOCK) + "     Esc to stop");
				clock.setPos(w - 260, 26, 0);
				center.setTextSimple("");
			}
			case NO_SIGNAL -> {
				label.setTextSimple("CAM // " + computerName.toUpperCase());
				label.setPos(48, 26, 0);
				clock.setTextSimple("Esc to stop");
				clock.setPos(w - 160, 26, 0);
				center.setTextSimple(blink ? "NO SIGNAL" : "");
				center.setPos(w / 2f - 70, h / 2f - 16, 0);
			}
			case STATUS -> {
				label.setTextSimple(hint);
				label.setPos(16, 12, 0);
				clock.setTextSimple("");
				center.setTextSimple("");
			}
		}
		super.draw();
	}

	private static void quad(float x, float y, float w, float h, float r, float g, float b, float a) {
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		batch.setColor(r, g, b, a);
		batch.quadColored(x, y, w, h);
	}

	private static void scanlines(int w, int h, float alpha) {
		for(int y = 0; y < h; y += 3) {
			quad(0, y, w, 1, 0, 0, 0, alpha);
		}
	}

	/** Darkened edges: four gradient bands fading from black at the border to clear inside. */
	private static void vignette(int w, int h) {
		float band = Math.min(w, h) * 0.22f;
		float d = 0.55f;
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		// Corner colours are top-left, top-right, bottom-right, bottom-left; only alpha varies.
		batch.quadGradient(0, 0, w, band, 0, 0, 0, d, 0, 0, 0, d, 0, 0, 0, 0, 0, 0, 0, 0);
		batch.quadGradient(0, h - band, w, band, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, d, 0, 0, 0, d);
		batch.quadGradient(0, 0, band, h, 0, 0, 0, d, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, d);
		batch.quadGradient(w - band, 0, band, h, 0, 0, 0, 0, 0, 0, 0, d, 0, 0, 0, d, 0, 0, 0, 0);
	}

	private static void grain(int w, int h, int count, float maxAlpha) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for(int i = 0; i < count; i++) {
			float v = random.nextFloat();
			quad(random.nextInt(w), random.nextInt(h), 2, 2, v, v, v, random.nextFloat() * maxAlpha);
		}
	}

	/** Viewfinder corner brackets. */
	private static void brackets(int w, int h) {
		float m = 20;
		float l = 42;
		GuiBatchRenderer batch = GuiBatchRenderer.get();
		batch.setColor(0.85f, 0.95f, 0.95f, 0.7f);
		float[][] corners = {{m, m, 1, 1}, {w - m, m, -1, 1}, {m, h - m, 1, -1}, {w - m, h - m, -1, -1}};
		for(float[] c : corners) {
			batch.line(c[0], c[1], c[0] + l * c[2], c[1], 2);
			batch.line(c[0], c[1], c[0], c[1] + l * c[3], 2);
		}
	}
}
