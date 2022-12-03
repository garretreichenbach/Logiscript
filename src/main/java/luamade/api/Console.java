package luamade.api;

import org.schema.game.common.data.SegmentPiece;
import luamade.api.element.block.Block;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console {

	private final SegmentPiece segmentPiece;

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	public Block getLuaBlock() {
		return new Block(segmentPiece);
	}
}
