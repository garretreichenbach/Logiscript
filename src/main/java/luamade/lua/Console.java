package luamade.lua;

import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.Varargs;
import org.schema.game.common.data.SegmentPiece;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Console extends LuaMadeUserdata {

	private final ComputerModule module;
	private final int VERTICAL = 0;
	private final int HORIZONTAL = 1;
	private static final float TRIM_TRIGGER_RATIO = 0.90F;
	private static final float TRIM_TARGET_RATIO = 0.75F;
	private StringBuilder textContents = new StringBuilder();
	private GraphicsFrame graphicsFrame;
	private long graphicsFrameRevision;
	private final int[] cursorPos = {0, 0};

	@LuaMadeCallable
	public synchronized void print(Varargs vargs) {
		if(graphicsFrame != null) {
			graphicsFrame = null;
			graphicsFrameRevision++;
		}
		textContents.append(vargs.arg(1).toString()).append("\n");
		trimScrollbackIfNeeded();
	}

	public Console(ComputerModule module) {
		this.module = module;
	}

	@LuaMadeCallable
	public Long getTime() {
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public Block getBlock() {
		return Block.wrap(module.getSegmentPiece()); //Block is basically a wrapper class for SegmentPiece
	}

	public synchronized void appendInline(Varargs vargs) {
		if(graphicsFrame != null) {
			graphicsFrame = null;
			graphicsFrameRevision++;
		}
		textContents.append(vargs.arg(1).toString());
		trimScrollbackIfNeeded();
	}

	public synchronized void clearTextContents() {
		textContents.setLength(0);
		graphicsFrame = null;
		cursorPos[VERTICAL] = 0;
		cursorPos[HORIZONTAL] = 0;
	}

	public synchronized void setTextContents(String textContents) {
		this.textContents = new StringBuilder(textContents);
		graphicsFrame = null;
		trimScrollbackIfNeeded();
		cursorPos[VERTICAL] = getLineNumber();
		cursorPos[HORIZONTAL] = getLinePos();
	}

	public SegmentPiece getSegmentPiece() {
		return module.getSegmentPiece();
	}

	public synchronized String getTextContents() {
		return textContents.toString();
	}

	public synchronized GraphicsFrame getGraphicsFrame() {
		return graphicsFrame;
	}

	public synchronized void setGraphicsFrame(GraphicsFrame graphicsFrame) {
		this.graphicsFrame = graphicsFrame;
		graphicsFrameRevision++;
	}

	public synchronized long getGraphicsFrameRevision() {
		return graphicsFrameRevision;
	}

	public synchronized void clearGraphicsFrame() {
		if(graphicsFrame != null) {
			graphicsFrame = null;
			graphicsFrameRevision++;
		}
	}

	public static class GraphicsFrame {
		private final RenderBackend backend;

		private final String text;
		private final int width;
		private final int height;
		private final float cellScaleX;
		private final float cellScaleY;
		private final boolean ansiEnabled;
		private final int[] codePoints;
		private final int[] foregroundColors;
		private final int[] backgroundColors;
		private final List<GraphicsLayer> layers;
		public GraphicsFrame(
				String text,
				int width,
				int height,
				float cellScaleX,
				float cellScaleY,
				boolean ansiEnabled,
				RenderBackend backend,
				int[] codePoints,
				int[] foregroundColors,
				int[] backgroundColors,
				List<GraphicsLayer> layers
		) {
			this.text = text == null ? "" : text;
			this.width = width;
			this.height = height;
			this.cellScaleX = cellScaleX;
			this.cellScaleY = cellScaleY;
			this.ansiEnabled = ansiEnabled;
			this.backend = backend == null ? RenderBackend.TERMINAL : backend;
			this.codePoints = codePoints == null ? new int[0] : Arrays.copyOf(codePoints, codePoints.length);
			this.foregroundColors = foregroundColors == null ? new int[0] : Arrays.copyOf(foregroundColors, foregroundColors.length);
			this.backgroundColors = backgroundColors == null ? new int[0] : Arrays.copyOf(backgroundColors, backgroundColors.length);
			if(layers == null || layers.isEmpty()) {
				List<GraphicsLayer> fallbackLayers = new ArrayList<>(1);
				fallbackLayers.add(new GraphicsLayer("base", this.text, cellScaleX, cellScaleY, new int[0], new int[0], new int[0]));
				this.layers = Collections.unmodifiableList(fallbackLayers);
			} else {
				this.layers = Collections.unmodifiableList(new ArrayList<>(layers));
			}
		}

		public RenderBackend getBackend() {
			return backend;
		}

		public String getText() {
			return text;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public float getCellScaleX() {
			return cellScaleX;
		}

		public float getCellScaleY() {
			return cellScaleY;
		}

		public boolean isAnsiEnabled() {
			return ansiEnabled;
		}

		public int[] getCodePoints() {
			return Arrays.copyOf(codePoints, codePoints.length);
		}

		public int[] getForegroundColors() {
			return Arrays.copyOf(foregroundColors, foregroundColors.length);
		}

		public int[] getBackgroundColors() {
			return Arrays.copyOf(backgroundColors, backgroundColors.length);
		}

		public List<GraphicsLayer> getLayers() {
			return layers;
		}

		public static final class GraphicsLayer {
			private final String name;
			private final String text;
			private final float cellScaleX;
			private final float cellScaleY;
			private final int[] codePoints;
			private final int[] foregroundColors;
			private final int[] backgroundColors;

			public GraphicsLayer(
					String name,
					String text,
					float cellScaleX,
					float cellScaleY,
					int[] codePoints,
					int[] foregroundColors,
					int[] backgroundColors
			) {
				this.name = name == null ? "" : name;
				this.text = text == null ? "" : text;
				this.cellScaleX = cellScaleX;
				this.cellScaleY = cellScaleY;
				this.codePoints = codePoints == null ? new int[0] : Arrays.copyOf(codePoints, codePoints.length);
				this.foregroundColors = foregroundColors == null ? new int[0] : Arrays.copyOf(foregroundColors, foregroundColors.length);
				this.backgroundColors = backgroundColors == null ? new int[0] : Arrays.copyOf(backgroundColors, backgroundColors.length);
			}

			public String getName() {
				return name;
			}

			public String getText() {
				return text;
			}

			public float getCellScaleX() {
				return cellScaleX;
			}

			public float getCellScaleY() {
				return cellScaleY;
			}

			public int[] getCodePoints() {
				return Arrays.copyOf(codePoints, codePoints.length);
			}

			public int[] getForegroundColors() {
				return Arrays.copyOf(foregroundColors, foregroundColors.length);
			}

			public int[] getBackgroundColors() {
				return Arrays.copyOf(backgroundColors, backgroundColors.length);
			}
		}

		public enum RenderBackend {
			TERMINAL,
			CANVAS
		}
	}

	private void trimScrollbackIfNeeded() {
		int characterLimit = ConfigManager.getConsoleCharacterLimit();
		int lineLimit = ConfigManager.getConsoleLineLimit();
		if(characterLimit <= 0 || lineLimit <= 0) {
			return;
		}

		int triggerCharacterLimit = Math.max(1, Math.round(characterLimit * TRIM_TRIGGER_RATIO));
		int targetCharacterLimit = Math.max(1, Math.round(characterLimit * TRIM_TARGET_RATIO));
		int triggerLineLimit = Math.max(1, Math.round(lineLimit * TRIM_TRIGGER_RATIO));
		int targetLineLimit = Math.max(1, Math.round(lineLimit * TRIM_TARGET_RATIO));

		int currentLineCount = countLines(textContents);
		if(textContents.length() < triggerCharacterLimit && currentLineCount < triggerLineLimit) {
			return;
		}

		String[] lines = textContents.toString().split("\\n", -1);
		int startIndex = 0;
		int remainingCharacters = textContents.length();
		int remainingLines = lines.length;

		while(startIndex < lines.length - 1 && (remainingCharacters > targetCharacterLimit || remainingLines > targetLineLimit)) {
			remainingCharacters -= lines[startIndex].length();
			if(startIndex < lines.length - 1) {
				remainingCharacters -= 1;
			}
			startIndex++;
			remainingLines--;
		}

		if(startIndex <= 0) {
			return;
		}

		StringBuilder trimmed = new StringBuilder();
		for(int i = startIndex; i < lines.length; i++) {
			trimmed.append(lines[i]);
			if(i < lines.length - 1) {
				trimmed.append('\n');
			}
		}

		textContents = trimmed;
		cursorPos[VERTICAL] = Math.max(0, countLines(textContents) - 1);
		cursorPos[HORIZONTAL] = getLinePos();
	}

	private int countLines(StringBuilder builder) {
		if(builder.length() == 0) {
			return 0;
		}

		int lines = 1;
		for(int i = 0; i < builder.length(); i++) {
			if(builder.charAt(i) == '\n') {
				lines++;
			}
		}
		return lines;
	}

	public int[] getCursorPos() {
		return cursorPos;
	}

	public synchronized int getLineNumber() {
		return textContents.toString().split("\n").length;
	}

	public void setLineNumber(int lineNumber) {
		cursorPos[VERTICAL] = lineNumber;
		trimCursorPos();
	}

	public synchronized int getLinePos() {
		String[] lines = textContents.toString().split("\n");
		if(cursorPos[VERTICAL] >= lines.length) {
			return 0;
		}
		return lines[cursorPos[VERTICAL]].length();
	}

	public void setLinePos(int linePos) {
		String[] lines = textContents.toString().split("\n");
		if(cursorPos[VERTICAL] >= lines.length) {
			return;
		}
		cursorPos[HORIZONTAL] = linePos;
		trimCursorPos();
		if(cursorPos[HORIZONTAL] > lines[cursorPos[VERTICAL]].length()) {
			cursorPos[HORIZONTAL] = lines[cursorPos[VERTICAL]].length();
		}
		trimCursorPos();
	}

	private void trimCursorPos() {
		if(cursorPos[VERTICAL] < 0) {
			cursorPos[VERTICAL] = 0;
		}
		if(cursorPos[HORIZONTAL] < 0) {
			cursorPos[HORIZONTAL] = 0;
		}
		if(cursorPos[VERTICAL] >= getLineNumber()) {
			cursorPos[VERTICAL] = getLineNumber() - 1;
		}
		if(cursorPos[HORIZONTAL] >= getLinePos()) {
			cursorPos[HORIZONTAL] = getLinePos() - 1;
		}
	}
}
