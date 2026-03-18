package luamade.lua.terminal;

import luamade.lua.Console;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

import java.util.Arrays;

/**
 * Text-based graphics API for terminal scripts.
 * Coordinates are 1-based to be Lua-friendly.
 */
public class TextGraphicsApi extends LuaMadeUserdata {
	private static final int MIN_SIZE = 1;
	private static final int MAX_WIDTH = 240;
	private static final int MAX_HEIGHT = 120;

	private final Console console;
	private int width = 64;
	private int height = 24;
	private int fillCodePoint = ' ';
	private int[][] cells;

	public TextGraphicsApi(Console console) {
		this.console = console;
		this.cells = new int[height][width];
		clearInternal(fillCodePoint);
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
	public void setSize(int newWidth, int newHeight) {
		int clampedWidth = clamp(newWidth, MIN_SIZE, MAX_WIDTH);
		int clampedHeight = clamp(newHeight, MIN_SIZE, MAX_HEIGHT);

		int[][] next = new int[clampedHeight][clampedWidth];
		for(int y = 0; y < clampedHeight; y++) {
			Arrays.fill(next[y], fillCodePoint);
		}

		int copyHeight = Math.min(height, clampedHeight);
		int copyWidth = Math.min(width, clampedWidth);
		for(int y = 0; y < copyHeight; y++) {
			System.arraycopy(cells[y], 0, next[y], 0, copyWidth);
		}

		width = clampedWidth;
		height = clampedHeight;
		cells = next;
	}

	@LuaMadeCallable
	public void clear() {
		clearInternal(fillCodePoint);
	}

	@LuaMadeCallable
	public void clear(String fill) {
		fillCodePoint = toCodePoint(fill);
		clearInternal(fillCodePoint);
	}

	@LuaMadeCallable
	public void pixel(int x, int y, String glyph) {
		setCell(x - 1, y - 1, toCodePoint(glyph));
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

		while(true) {
			setCell(xStart, yStart, cp);
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

		for(int xx = x0; xx <= x1; xx++) {
			setCell(xx, y0, cp);
			setCell(xx, y1, cp);
		}
		for(int yy = y0; yy <= y1; yy++) {
			setCell(x0, yy, cp);
			setCell(x1, yy, cp);
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

		for(int yy = y0; yy <= y1; yy++) {
			for(int xx = x0; xx <= x1; xx++) {
				setCell(xx, yy, cp);
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

		while(y <= x) {
			plotCirclePoints(cpx, cpy, x, y, cp);
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

		for(int yy = -radius; yy <= radius; yy++) {
			for(int xx = -radius; xx <= radius; xx++) {
				if((xx * xx) + (yy * yy) <= radiusSquared) {
					setCell(cpx + xx, cpy + yy, cp);
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
		for(int offset = 0; offset < content.length();) {
			int cp = content.codePointAt(offset);
			offset += Character.charCount(cp);
			setCell(cursorX, py, cp);
			cursorX++;
		}
	}

	@LuaMadeCallable
	public String frame() {
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

	private void plotCirclePoints(int cx, int cy, int x, int y, int cp) {
		setCell(cx + x, cy + y, cp);
		setCell(cx + y, cy + x, cp);
		setCell(cx - y, cy + x, cp);
		setCell(cx - x, cy + y, cp);
		setCell(cx - x, cy - y, cp);
		setCell(cx - y, cy - x, cp);
		setCell(cx + y, cy - x, cp);
		setCell(cx + x, cy - y, cp);
	}

	private void clearInternal(int cp) {
		for(int y = 0; y < height; y++) {
			Arrays.fill(cells[y], cp);
		}
	}

	private void setCell(int x, int y, int codePoint) {
		if(x < 0 || y < 0 || x >= width || y >= height) {
			return;
		}
		cells[y][x] = codePoint;
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
}
