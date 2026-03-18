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
	public Long getTextBlockIndex() {
		return getSegmentPiece().getTextBlockIndex();
	}
}