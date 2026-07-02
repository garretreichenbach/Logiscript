package luamade.network;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.lua.gfx.Gfx2d;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared wire format for {@link Gfx2d.FrameSnapshot}, used by both the initial
 * connect-ack (which bundles a full snapshot) and the steady-state gfx sync
 * packet. Scripts now execute server-side, so the server's {@code Gfx2d}
 * instance is the source of truth — this codec is how its draw-command buffer
 * crosses the wire to the client(s) actually rendering it.
 */
final class GfxSnapshotCodec {

	private GfxSnapshotCodec() {
	}

	static void write(PacketWriteBuffer buffer, Gfx2d.FrameSnapshot snapshot) throws IOException {
		buffer.writeInt(snapshot.width);
		buffer.writeInt(snapshot.height);
		buffer.writeInt(snapshot.viewportWidth);
		buffer.writeInt(snapshot.viewportHeight);
		buffer.writeFloat(snapshot.scaleX);
		buffer.writeFloat(snapshot.scaleY);
		buffer.writeLong(snapshot.revision);
		buffer.writeInt(snapshot.layers.size());
		for(Gfx2d.LayerSnapshot layer : snapshot.layers) {
			buffer.writeString(layer.name);
			buffer.writeInt(layer.order);
			buffer.writeBoolean(layer.visible);
			buffer.writeInt(layer.commands.size());
			for(Gfx2d.DrawCommand command : layer.commands) {
				writeCommand(buffer, command);
			}
		}
	}

	static Gfx2d.FrameSnapshot read(PacketReadBuffer buffer) throws IOException {
		int width = buffer.readInt();
		int height = buffer.readInt();
		int viewportWidth = buffer.readInt();
		int viewportHeight = buffer.readInt();
		float scaleX = buffer.readFloat();
		float scaleY = buffer.readFloat();
		long revision = buffer.readLong();

		int layerCount = buffer.readInt();
		List<Gfx2d.LayerSnapshot> layers = new ArrayList<>(layerCount);
		for(int i = 0; i < layerCount; i++) {
			String name = buffer.readString();
			int order = buffer.readInt();
			boolean visible = buffer.readBoolean();
			int commandCount = buffer.readInt();
			List<Gfx2d.DrawCommand> commands = new ArrayList<>(commandCount);
			for(int c = 0; c < commandCount; c++) {
				commands.add(readCommand(buffer));
			}
			layers.add(Gfx2d.LayerSnapshot.fromWire(name, order, visible, commands));
		}

		return Gfx2d.FrameSnapshot.fromWire(width, height, viewportWidth, viewportHeight, scaleX, scaleY, revision, layers);
	}

	private static void writeCommand(PacketWriteBuffer buffer, Gfx2d.DrawCommand command) throws IOException {
		buffer.writeByte((byte) command.kind.ordinal());
		buffer.writeFloat(command.x1);
		buffer.writeFloat(command.y1);
		buffer.writeFloat(command.x2);
		buffer.writeFloat(command.y2);
		buffer.writeFloat(command.r);
		buffer.writeFloat(command.g);
		buffer.writeFloat(command.b);
		buffer.writeFloat(command.a);
		buffer.writeBoolean(command.filled);
		buffer.writeInt(command.segments);

		if(command.points == null) {
			buffer.writeInt(-1);
		} else {
			buffer.writeInt(command.points.length);
			for(float point : command.points) {
				buffer.writeFloat(point);
			}
		}

		boolean hasText = command.text != null;
		buffer.writeBoolean(hasText);
		if(hasText) {
			buffer.writeString(command.text);
		}
		buffer.writeInt(command.textScale);

		buffer.writeInt(command.bitmapWidth);
		buffer.writeInt(command.bitmapHeight);
		if(command.bitmapPixels == null) {
			buffer.writeInt(-1);
		} else {
			buffer.writeInt(command.bitmapPixels.length);
			for(int pixel : command.bitmapPixels) {
				buffer.writeInt(pixel);
			}
		}

		buffer.writeFloat(command.lineWidth);
		buffer.writeInt(command.textMaxWidth);
		buffer.writeInt(command.textMaxHeight);
		buffer.writeString(command.textAlign == null ? "left" : command.textAlign);
		buffer.writeBoolean(command.textWrap);
	}

	private static Gfx2d.DrawCommand readCommand(PacketReadBuffer buffer) throws IOException {
		Gfx2d.DrawCommand.Kind kind = Gfx2d.DrawCommand.Kind.values()[buffer.readByte()];
		float x1 = buffer.readFloat();
		float y1 = buffer.readFloat();
		float x2 = buffer.readFloat();
		float y2 = buffer.readFloat();
		float r = buffer.readFloat();
		float g = buffer.readFloat();
		float b = buffer.readFloat();
		float a = buffer.readFloat();
		boolean filled = buffer.readBoolean();
		int segments = buffer.readInt();

		int pointsLength = buffer.readInt();
		float[] points = null;
		if(pointsLength >= 0) {
			points = new float[pointsLength];
			for(int i = 0; i < pointsLength; i++) {
				points[i] = buffer.readFloat();
			}
		}

		boolean hasText = buffer.readBoolean();
		String text = hasText ? buffer.readString() : null;
		int textScale = buffer.readInt();

		int bitmapWidth = buffer.readInt();
		int bitmapHeight = buffer.readInt();
		int bitmapPixelsLength = buffer.readInt();
		int[] bitmapPixels = null;
		if(bitmapPixelsLength >= 0) {
			bitmapPixels = new int[bitmapPixelsLength];
			for(int i = 0; i < bitmapPixelsLength; i++) {
				bitmapPixels[i] = buffer.readInt();
			}
		}

		float lineWidth = buffer.readFloat();
		int textMaxWidth = buffer.readInt();
		int textMaxHeight = buffer.readInt();
		String textAlign = buffer.readString();
		boolean textWrap = buffer.readBoolean();

		return Gfx2d.DrawCommand.fromWire(kind, x1, y1, x2, y2, r, g, b, a, filled, segments, points, text, textScale, bitmapWidth, bitmapHeight, bitmapPixels, lineWidth, textMaxWidth, textMaxHeight, textAlign, textWrap);
	}
}
