package luamade.lua;

import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.Varargs;
import org.schema.game.common.data.SegmentPiece;

public class Console extends LuaMadeUserdata {

	private final ComputerModule module;
	private final int VERTICAL = 0;
	private final int HORIZONTAL = 1;
	private StringBuilder textContents = new StringBuilder();
	private int[] cursorPos = {0, 0};

	public Console(ComputerModule module) {
		this.module = module;
	}

	@LuaMadeCallable
	public Long getTime() {
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public Block getBlock() {
		return new Block(module.getSegmentPiece()); //Block is basically a wrapper class for SegmentPiece
	}

	@LuaMadeCallable
	public void print(Varargs vargs) {
		textContents.append(vargs.arg(1).toString()).append("\n");
	}

	public SegmentPiece getSegmentPiece() {
		return module.getSegmentPiece();
	}

	public String getTextContents() {
		return textContents.toString();
	}

	public void setTextContents(String textContents) {
		this.textContents = new StringBuilder(textContents);
		cursorPos[VERTICAL] = getLineNumber();
		cursorPos[HORIZONTAL] = getLinePos();
	}

	public int[] getCursorPos() {
		return cursorPos;
	}

	public int getLineNumber() {
		return textContents.toString().split("\n").length;
	}

	public void setLineNumber(int lineNumber) {
		cursorPos[VERTICAL] = lineNumber;
		trimCursorPos();
	}

	public int getLinePos() {
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
