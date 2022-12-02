package thederpgamer.logiscript.api;

import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.api.element.block.Block;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console implements ConsoleInterface {

	private final transient SegmentPiece segmentPiece;

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	@Override
	public String getName() {
		return "console";
	}

	@Override
	public Block getBlock() {
		return new Block(segmentPiece);
	}
}
