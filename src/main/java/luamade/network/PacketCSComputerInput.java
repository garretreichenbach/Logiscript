package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;

/**
 * Client → Server: a single computer session event. Scripts now execute on
 * the server (see {@link ComputerModule}), so a client's terminal dialog no
 * longer runs its own local VM — it forwards every keystroke, mouse event,
 * and submitted command line here, and renders whatever the server streams
 * back via {@link PacketSCConsoleSnapshot} / {@link PacketSCGfxSnapshot}.
 *
 * <p>All event kinds share one packet shape (mirrors {@link PacketCSVaultScriptOp}'s
 * approach): irrelevant fields for a given {@link Kind} are simply ignored.
 *
 * <p>{@link Kind#CONNECT} is the only kind that gets an explicit reply
 * ({@link PacketSCComputerConnectAck}) and the only one that does not require
 * the sender to already be a registered viewer — every other kind is dropped
 * unless the sender has connected first, so a modified client can't puppet a
 * computer it never opened a session against.
 */
public class PacketCSComputerInput extends Packet {

	public enum Kind { CONNECT, DISCONNECT, LINE_INPUT, KEY_EVENT, MOUSE_EVENT, INTERRUPT, RESET, EXIT_EDITOR, VIEWPORT_RESIZE, SET_SAVED_INPUT, UI_LAYOUT }

	private int requestId;
	private String kind;
	private int entityId;
	private long absIndex;

	// LINE_INPUT
	private String text;

	// KEY_EVENT
	private int key;
	private int charCode;
	private boolean down;
	private boolean shift;
	private boolean ctrl;
	private boolean alt;

	// MOUSE_EVENT — already fully resolved to canvas/UI space by the sending
	// client (which owns the GUI layout); the server just forwards these
	// verbatim into the module's InputApi.
	private int mouseButton;
	private boolean mousePressed;
	private int mouseX;
	private int mouseY;
	private int mouseDx;
	private int mouseDy;
	private int mouseWheel;
	private int uiX;
	private int uiY;
	private boolean insideCanvas;
	private boolean dragging;
	private String dragButton;

	// VIEWPORT_RESIZE
	private int viewportWidth;
	private int viewportHeight;

	// UI_LAYOUT
	private int windowX;
	private int windowY;
	private int windowWidth;
	private int windowHeight;
	private int canvasX;
	private int canvasY;
	private int canvasWidth;
	private int canvasHeight;

	public PacketCSComputerInput() {
	}

	private PacketCSComputerInput(Kind kind, int entityId, long absIndex) {
		this.kind = kind.name();
		this.entityId = entityId;
		this.absIndex = absIndex;
		this.dragButton = "none";
	}

	public static PacketCSComputerInput connect(int requestId, int entityId, long absIndex) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.CONNECT, entityId, absIndex);
		packet.requestId = requestId;
		return packet;
	}

	public static PacketCSComputerInput disconnect(int entityId, long absIndex) {
		return new PacketCSComputerInput(Kind.DISCONNECT, entityId, absIndex);
	}

	public static PacketCSComputerInput lineInput(int entityId, long absIndex, String text) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.LINE_INPUT, entityId, absIndex);
		packet.text = text == null ? "" : text;
		return packet;
	}

	public static PacketCSComputerInput keyEvent(int entityId, long absIndex, int key, char character, boolean down, boolean shift, boolean ctrl, boolean alt) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.KEY_EVENT, entityId, absIndex);
		packet.key = key;
		packet.charCode = character;
		packet.down = down;
		packet.shift = shift;
		packet.ctrl = ctrl;
		packet.alt = alt;
		return packet;
	}

	public static PacketCSComputerInput mouseEvent(int entityId, long absIndex, int button, boolean pressed, int x, int y, int dx, int dy, int wheel, int uiX, int uiY, boolean insideCanvas, boolean dragging, String dragButton) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.MOUSE_EVENT, entityId, absIndex);
		packet.mouseButton = button;
		packet.mousePressed = pressed;
		packet.mouseX = x;
		packet.mouseY = y;
		packet.mouseDx = dx;
		packet.mouseDy = dy;
		packet.mouseWheel = wheel;
		packet.uiX = uiX;
		packet.uiY = uiY;
		packet.insideCanvas = insideCanvas;
		packet.dragging = dragging;
		packet.dragButton = dragButton == null ? "none" : dragButton;
		return packet;
	}

	public static PacketCSComputerInput interrupt(int entityId, long absIndex) {
		return new PacketCSComputerInput(Kind.INTERRUPT, entityId, absIndex);
	}

	public static PacketCSComputerInput reset(int entityId, long absIndex) {
		return new PacketCSComputerInput(Kind.RESET, entityId, absIndex);
	}

	public static PacketCSComputerInput exitEditor(int entityId, long absIndex) {
		return new PacketCSComputerInput(Kind.EXIT_EDITOR, entityId, absIndex);
	}

	public static PacketCSComputerInput viewportResize(int entityId, long absIndex, int width, int height) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.VIEWPORT_RESIZE, entityId, absIndex);
		packet.viewportWidth = width;
		packet.viewportHeight = height;
		return packet;
	}

	public static PacketCSComputerInput setSavedInput(int entityId, long absIndex, String text) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.SET_SAVED_INPUT, entityId, absIndex);
		packet.text = text == null ? "" : text;
		return packet;
	}

	public static PacketCSComputerInput uiLayout(int entityId, long absIndex, int windowX, int windowY, int windowWidth, int windowHeight, int canvasX, int canvasY, int canvasWidth, int canvasHeight) {
		PacketCSComputerInput packet = new PacketCSComputerInput(Kind.UI_LAYOUT, entityId, absIndex);
		packet.windowX = windowX;
		packet.windowY = windowY;
		packet.windowWidth = windowWidth;
		packet.windowHeight = windowHeight;
		packet.canvasX = canvasX;
		packet.canvasY = canvasY;
		packet.canvasWidth = canvasWidth;
		packet.canvasHeight = canvasHeight;
		return packet;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		requestId = buffer.readInt();
		kind = buffer.readString();
		entityId = buffer.readInt();
		absIndex = buffer.readLong();
		text = buffer.readString();
		key = buffer.readInt();
		charCode = buffer.readInt();
		down = buffer.readBoolean();
		shift = buffer.readBoolean();
		ctrl = buffer.readBoolean();
		alt = buffer.readBoolean();
		mouseButton = buffer.readInt();
		mousePressed = buffer.readBoolean();
		mouseX = buffer.readInt();
		mouseY = buffer.readInt();
		mouseDx = buffer.readInt();
		mouseDy = buffer.readInt();
		mouseWheel = buffer.readInt();
		uiX = buffer.readInt();
		uiY = buffer.readInt();
		insideCanvas = buffer.readBoolean();
		dragging = buffer.readBoolean();
		dragButton = buffer.readString();
		viewportWidth = buffer.readInt();
		viewportHeight = buffer.readInt();
		windowX = buffer.readInt();
		windowY = buffer.readInt();
		windowWidth = buffer.readInt();
		windowHeight = buffer.readInt();
		canvasX = buffer.readInt();
		canvasY = buffer.readInt();
		canvasWidth = buffer.readInt();
		canvasHeight = buffer.readInt();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeInt(requestId);
		buffer.writeString(kind);
		buffer.writeInt(entityId);
		buffer.writeLong(absIndex);
		buffer.writeString(text == null ? "" : text);
		buffer.writeInt(key);
		buffer.writeInt(charCode);
		buffer.writeBoolean(down);
		buffer.writeBoolean(shift);
		buffer.writeBoolean(ctrl);
		buffer.writeBoolean(alt);
		buffer.writeInt(mouseButton);
		buffer.writeBoolean(mousePressed);
		buffer.writeInt(mouseX);
		buffer.writeInt(mouseY);
		buffer.writeInt(mouseDx);
		buffer.writeInt(mouseDy);
		buffer.writeInt(mouseWheel);
		buffer.writeInt(uiX);
		buffer.writeInt(uiY);
		buffer.writeBoolean(insideCanvas);
		buffer.writeBoolean(dragging);
		buffer.writeString(dragButton == null ? "none" : dragButton);
		buffer.writeInt(viewportWidth);
		buffer.writeInt(viewportHeight);
		buffer.writeInt(windowX);
		buffer.writeInt(windowY);
		buffer.writeInt(windowWidth);
		buffer.writeInt(windowHeight);
		buffer.writeInt(canvasX);
		buffer.writeInt(canvasY);
		buffer.writeInt(canvasWidth);
		buffer.writeInt(canvasHeight);
	}

	@Override
	public void processPacketOnClient() {
		// Client → Server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		Kind parsedKind;
		try {
			parsedKind = Kind.valueOf(kind);
		} catch(Exception ex) {
			return;
		}

		SegmentPiece piece = ComputerPacketUtil.resolveComputerPiece(entityId, absIndex);
		if(piece == null) {
			if(parsedKind == Kind.CONNECT) {
				PacketUtil.sendPacket(sender, PacketSCComputerConnectAck.failure(requestId, entityId, absIndex, "Computer not found"));
			}
			return;
		}

		ComputerModuleContainer container = ComputerPacketUtil.resolveContainer(piece);
		if(container == null) {
			if(parsedKind == Kind.CONNECT) {
				PacketUtil.sendPacket(sender, PacketSCComputerConnectAck.failure(requestId, entityId, absIndex, "Computer module unavailable"));
			}
			return;
		}

		if(parsedKind == Kind.CONNECT) {
			if(!ComputerPacketUtil.isWithinConnectRange(sender, piece)) {
				PacketUtil.sendPacket(sender, PacketSCComputerConnectAck.failure(requestId, entityId, absIndex, "Too far from the computer"));
				return;
			}
			ComputerModule module = container.getOrCreateModule(piece);
			if(module == null) {
				PacketUtil.sendPacket(sender, PacketSCComputerConnectAck.failure(requestId, entityId, absIndex, "Failed to initialize computer"));
				return;
			}
			container.addViewer(absIndex, sender);
			boolean passwordInputMode = module.getTerminal() != null && module.getTerminal().isPasswordInputMode();
			PacketUtil.sendPacket(sender, PacketSCComputerConnectAck.success(requestId, entityId, absIndex, module.getLastTextContent(), module.getGfxApi().snapshot(), module.getInputApi().isKeyboardConsumed(), module.getInputApi().isMouseConsumed(), (byte) module.getLastMode().ordinal(), module.getLastOpenFile(), passwordInputMode, (byte) module.getScrollMode().ordinal(), module.getSavedTerminalInput()));
			return;
		}

		if(parsedKind == Kind.DISCONNECT) {
			container.removeViewer(absIndex, sender);
			return;
		}

		// Every other event kind requires an established session (reach was
		// already validated at CONNECT time above).
		if(!container.isViewer(absIndex, sender)) {
			return;
		}

		ComputerModule module = container.getModule(piece);
		if(module == null) {
			return;
		}
		module.setTouched();

		switch(parsedKind) {
			case LINE_INPUT:
				module.getTerminal().handleInput(text == null ? "" : text, sender);
				break;
			case KEY_EVENT:
				module.getInputApi().pushKeyEvent(key, (char) charCode, down, shift, ctrl, alt);
				break;
			case MOUSE_EVENT:
				module.getInputApi().pushMouseEvent(mouseButton, mousePressed, mouseX, mouseY, mouseDx, mouseDy, mouseWheel, -1, -1, uiX, uiY, insideCanvas, dragging, dragButton);
				break;
			case INTERRUPT:
				module.getTerminal().interruptForeground();
				break;
			case RESET:
				module.setLastMode(ComputerModule.ComputerMode.TERMINAL);
				module.setSavedTerminalInput("");
				module.getTerminal().hardReset();
				break;
			case EXIT_EDITOR:
				module.setLastMode(ComputerModule.ComputerMode.TERMINAL);
				break;
			case VIEWPORT_RESIZE:
				module.getGfxApi().setViewportSize(viewportWidth, viewportHeight);
				break;
			case SET_SAVED_INPUT:
				module.setSavedTerminalInput(text == null ? "" : text);
				break;
			case UI_LAYOUT:
				module.getInputApi().setUiLayout(windowX, windowY, windowWidth, windowHeight, canvasX, canvasY, canvasWidth, canvasHeight);
				break;
			default:
				break;
		}
	}
}
