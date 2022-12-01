package thederpgamer.logiscript.utils;

import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.PositionControl;
import org.schema.game.common.controller.SegmentBufferInterface;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;
import java.util.Locale;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SegmentPieceUtils {

	public static ArrayList<SegmentPiece> getControlledPiecesMatching(SegmentPiece segmentPiece, short type) {
		ArrayList<SegmentPiece> controlledPieces = new ArrayList<>();
		PositionControl control = segmentPiece.getSegmentController().getControlElementMap().getControlledElements(type, new Vector3i(segmentPiece.x, segmentPiece.y, segmentPiece.z));
		if (control != null) {
			for (long l : control.getControlMap().toLongArray()) {
				SegmentPiece p = segmentPiece.getSegmentController().getSegmentBuffer().getPointUnsave(l);
				if (p != null && p.getType() == type) controlledPieces.add(p);
			}
		}
		return controlledPieces;
	}

	public static ArrayList<SegmentPiece> getControlledPieces(SegmentPiece segmentPiece) {
		ArrayList<SegmentPiece> controlledPieces = new ArrayList<>();
		for(ElementInformation info : ElementKeyMap.getInfoArray()) {
			try {
				controlledPieces.addAll(getControlledPiecesMatching(segmentPiece, info.getId()));
			} catch(Exception ignored) {}
		}
		return controlledPieces;
	}

	public static SegmentPiece getFirstMatchingAdjacent(SegmentPiece segmentPiece, short type) {
		ArrayList<SegmentPiece> matching = getMatchingAdjacent(segmentPiece, type);
		if (matching.isEmpty()) return null;
		else return matching.get(0);
	}

	public static SegmentPiece getAdjacentDir(SegmentPiece segmentPiece, String dir) {
		ArrayList<SegmentPiece> matching = getAdjacent(segmentPiece);
		if(!matching.isEmpty()) {
			int dirInt = -1;
			switch(dir.toLowerCase()) {
				case "left":
					dirInt = 0;
					break;
				case "right":
					dirInt = 1;
					break;
				case "down":
					dirInt = 2;
					break;
				case "up":
					dirInt = 3;
					break;
				case "back":
					dirInt = 4;
					break;
				case "front":
					dirInt = 5;
					break;
			}
			for(int i = 0; i < 6; i ++) {
				if(i == dirInt && i < matching.size() && matching.get(i) != null) return matching.get(i);
			}
		}
		return null;
	}

	public static ArrayList<SegmentPiece> getMatchingAdjacent(SegmentPiece segmentPiece, short type) {
		ArrayList<SegmentPiece> matchingAdjacent = new ArrayList<>();
		SegmentBufferInterface buffer = segmentPiece.getSegmentController().getSegmentBuffer();
		Vector3i pos = new Vector3i(segmentPiece.getAbsolutePos(new Vector3i()));
		Vector3i[] offsets = getAdjacencyOffsets(pos);
		for (Vector3i offset : offsets) {
			if (buffer.existsPointUnsave(offset)) {
				SegmentPiece piece = buffer.getPointUnsave(offset);
				if (piece.getType() == type) matchingAdjacent.add(piece);
			}
		}
		return matchingAdjacent;
	}

	public static ArrayList<SegmentPiece> getAdjacent(SegmentPiece segmentPiece) {
		ArrayList<SegmentPiece> adjacent = new ArrayList<>();
		SegmentBufferInterface buffer = segmentPiece.getSegmentController().getSegmentBuffer();
		Vector3i pos = new Vector3i(segmentPiece.getAbsolutePos(new Vector3i()));
		Vector3i[] offsets = getAdjacencyOffsets(pos);
		for(Vector3i offset : offsets) {
			if(buffer.existsPointUnsave(offset)) {
				SegmentPiece piece = buffer.getPointUnsave(offset);
				adjacent.add(piece);
			}
		}
		return adjacent;
	}

	private static Vector3i[] getAdjacencyOffsets(Vector3i absPos) {
		return new Vector3i[] {
				new Vector3i(absPos.x - 1, absPos.y, absPos.z),
				new Vector3i(absPos.x + 1, absPos.y, absPos.z),
				new Vector3i(absPos.x, absPos.y - 1, absPos.z),
				new Vector3i(absPos.x, absPos.y + 1, absPos.z),
				new Vector3i(absPos.x, absPos.y, absPos.z - 1),
				new Vector3i(absPos.x, absPos.y, absPos.z + 1)
		};
	}

	public static String setValue(SegmentPiece segmentPiece, Object value) {
		ElementInformation info = segmentPiece.getInfo();
		if(info.getName().toLowerCase(Locale.ENGLISH).contains("light")) {
			//Is a type of light block, value should be true or false
			if(value instanceof Boolean) segmentPiece.setActive((Boolean) value);
			else return "Value must be a boolean!";
		} else if(info.id == 405 || info.id == 993 || info.id == 666 || info.id == 399) { //Activation and button blocks
			if(value instanceof Boolean) segmentPiece.setActive((Boolean) value);
			else return "Value must be a boolean!";
		} else if(value instanceof Boolean) segmentPiece.setActive((Boolean) value);
		segmentPiece.applyToSegment(segmentPiece.getSegmentController().isOnServer());
		return "";
	}
}