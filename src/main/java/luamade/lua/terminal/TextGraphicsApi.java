package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.util.Arrays;
import java.util.Locale;

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

	public TextGraphicsApi(Console console) {
		this.console = console;
		this.cells = new int[height][width];
		this.fgColors = new int[height][width];
		this.bgColors = new int[height][width];
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT);
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
		for(int y = 0; y < clampedHeight; y++) {
			Arrays.fill(next[y], fillCodePoint);
			Arrays.fill(nextFg[y], ANSI_DEFAULT);
			Arrays.fill(nextBg[y], ANSI_DEFAULT);
		}

		int copyHeight = Math.min(height, clampedHeight);
		int copyWidth = Math.min(width, clampedWidth);
		for(int y = 0; y < copyHeight; y++) {
			System.arraycopy(cells[y], 0, next[y], 0, copyWidth);
			System.arraycopy(fgColors[y], 0, nextFg[y], 0, copyWidth);
			System.arraycopy(bgColors[y], 0, nextBg[y], 0, copyWidth);
		}

		width = clampedWidth;
		height = clampedHeight;
		cells = next;
		fgColors = nextFg;
		bgColors = nextBg;
	}

	@LuaMadeCallable
	public void clear() {
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT);
	}

	@LuaMadeCallable
	public void clear(String fill) {
		fillCodePoint = toCodePoint(fill);
		clearInternal(fillCodePoint, ANSI_DEFAULT, ANSI_DEFAULT);
	}

	@LuaMadeCallable
	public void clear(String fill, String foreground, String background) {
		fillCodePoint = toCodePoint(fill);
		clearInternal(fillCodePoint, parseColor(foreground), parseColor(background));
	}

	@LuaMadeCallable
	public void pixel(int x, int y, String glyph) {
		setCell(x - 1, y - 1, toCodePoint(glyph), brushFg, brushBg);
	}

	@LuaMadeCallable
	public void pixel(int x, int y, String glyph, String foreground, String background) {
		setCell(x - 1, y - 1, toCodePoint(glyph), parseColor(foreground), parseColor(background));
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
	public void render() {
		console.setTextContents(frame());
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

	private void clearInternal(int cp, int fg, int bg) {
		for(int y = 0; y < height; y++) {
			Arrays.fill(cells[y], cp);
			Arrays.fill(fgColors[y], fg);
			Arrays.fill(bgColors[y], bg);
		}
	}

	private void setCell(int x, int y, int codePoint, int fg, int bg) {
		if(x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		cells[y][x] = codePoint;
		fgColors[y][x] = fg;
		bgColors[y][x] = bg;
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
