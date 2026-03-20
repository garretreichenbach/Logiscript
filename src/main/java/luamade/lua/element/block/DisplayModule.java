package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.data.SegmentPiece;

public class DisplayModule extends Block {

	public DisplayModule(SegmentPiece piece) {
		super(piece);
	}

	@LuaMadeCallable
	public void setText(String text) {
		setDisplayText(text);
	}

	@LuaMadeCallable
	public String getText() {
		return getDisplayText();
	}

	@LuaMadeCallable
	public void clearText() {
		setDisplayText("");
	}

	@LuaMadeCallable
	public void appendText(String text) {
		String currentText = getDisplayText();
		setDisplayText((currentText == null ? "" : currentText) + (text == null ? "" : text));
	}

	@LuaMadeCallable
	public void setLines(String[] lines) {
		if(lines == null || lines.length == 0) {
			setDisplayText("");
			return;
		}

		StringBuilder builder = new StringBuilder();
		for(String line : lines) {
			if(builder.length() > 0) {
				builder.append('\n');
			}
			builder.append(line == null ? "" : line);
		}
		setDisplayText(builder.toString());
	}

	@LuaMadeCallable
	public Long getTextBlockIndex() {
		return getSegmentPiece().getTextBlockIndex();
	}
}