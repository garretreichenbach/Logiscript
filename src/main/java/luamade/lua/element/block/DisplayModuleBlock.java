package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import org.schema.game.common.data.SegmentPiece;

public class DisplayModuleBlock extends Block {

	public DisplayModuleBlock(SegmentPiece piece) {
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
	public Integer getTextBlockIndex() {
		return getSegmentPiece().getTextBlockIndex();
	}
}