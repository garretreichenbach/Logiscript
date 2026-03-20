package luamade.utils;

import com.bulletphysics.linearmath.Transform;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.PositionControl;
import org.schema.game.common.controller.SegmentBufferInterface;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Locale;

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
		if(segmentPiece == null || dir == null) {
			return null;
		}

		Vector3i offset = getDirectionalOffset(segmentPiece, dir);
		if(offset == null) {
			return null;
		}

		Vector3i absolutePos = new Vector3i(segmentPiece.getAbsolutePos(new Vector3i()));
		absolutePos.add(offset);
		return segmentPiece.getSegmentController().getSegmentBuffer().getPointUnsave(absolutePos);
	}

	private static Vector3i getDirectionalOffset(SegmentPiece segmentPiece, String dir) {
		String side = dir.toLowerCase(Locale.ROOT);
		switch(side) {
			case "left":
				return getOrientedLeft(segmentPiece);
			case "right":
				return negate(getOrientedLeft(segmentPiece));
			case "up":
				return getOrientedUp(segmentPiece);
			case "down":
				return negate(getOrientedUp(segmentPiece));
			case "front":
				return getOrientedFront(segmentPiece);
			case "back":
				return negate(getOrientedFront(segmentPiece));
			default:
				return null;
		}
	}

	private static Vector3i getOrientedLeft(SegmentPiece segmentPiece) {
		Vector3f localRight = getBasisDirection(segmentPiece, new Vector3f(1.0f, 0.0f, 0.0f));
		return negate(snapToAxis(localRight));
	}

	private static Vector3i getOrientedUp(SegmentPiece segmentPiece) {
		Vector3f localUp = getBasisDirection(segmentPiece, new Vector3f(0.0f, 1.0f, 0.0f));
		return snapToAxis(localUp);
	}

	private static Vector3i getOrientedFront(SegmentPiece segmentPiece) {
		// SegmentPiece local +Z points opposite the expected user-facing "front" side.
		Vector3f localFront = getBasisDirection(segmentPiece, new Vector3f(0.0f, 0.0f, -1.0f));
		return snapToAxis(localFront);
	}

	private static Vector3f getBasisDirection(SegmentPiece segmentPiece, Vector3f localAxis) {
		Transform transform = new Transform();
		segmentPiece.getTransform(transform);
		Vector3f out = new Vector3f(localAxis);
		transform.basis.transform(out);
		return out;
	}

	private static Vector3i snapToAxis(Vector3f direction) {
		float absX = Math.abs(direction.x);
		float absY = Math.abs(direction.y);
		float absZ = Math.abs(direction.z);

		if(absX >= absY && absX >= absZ) {
			return new Vector3i(direction.x >= 0 ? 1 : -1, 0, 0);
		}
		if(absY >= absX && absY >= absZ) {
			return new Vector3i(0, direction.y >= 0 ? 1 : -1, 0);
		}
		return new Vector3i(0, 0, direction.z >= 0 ? 1 : -1);
	}

	private static Vector3i negate(Vector3i vec) {
		if(vec == null) {
			return null;
		}
		return new Vector3i(-vec.x, -vec.y, -vec.z);
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