package luamade.gui;

import api.network.packets.PacketUtil;
import luamade.lua.fs.FileIoRequests;
import luamade.lua.gfx.Gfx2d;
import luamade.network.PacketCSClipboardImport;
import luamade.network.PacketCSComputerInput;
import luamade.network.PacketCSFileRead;
import luamade.network.PacketCSFileWrite;
import luamade.network.PacketCSTerminalQuery;
import luamade.system.module.ComputerModule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Client-side view of a computer session. Scripts now execute server-side
 * (see {@link luamade.system.module.ComputerModule}), so the dialog no longer
 * holds a live, in-process {@code ComputerModule} — it holds this instead: a
 * cache of the last console text / gfx2d frame / mode the server streamed
 * down, plus helpers that forward input as {@link PacketCSComputerInput}
 * packets rather than calling a local {@code Terminal}/{@code InputApi}
 * directly.
 *
 * <p>Fields are updated from the network thread ({@code volatile}, no other
 * synchronization) and read from the render thread each frame — exactly the
 * same "eventually consistent, read whatever's there" contract the old code
 * already had reading a live object across frames.
 */
public final class ComputerSessionView {

	private final int entityId;
	private final long absIndex;

	private volatile boolean connected;
	private volatile String connectFailureMessage;
	private volatile String consoleText = "";
	private volatile Gfx2d.FrameSnapshot gfxSnapshot;
	private volatile boolean keyboardConsumed;
	private volatile boolean mouseConsumed;
	private volatile byte modeOrdinal = (byte) ComputerModule.ComputerMode.TERMINAL.ordinal();
	private volatile String lastOpenFile = "";
	private volatile boolean passwordInputMode;
	private volatile byte scrollModeOrdinal = (byte) ComputerModule.ScrollMode.VERTICAL.ordinal();
	private volatile String savedTerminalInput = "";

	private static final AtomicInteger queryRequestIdGenerator = new AtomicInteger(1);
	private volatile int historyResponseId = -1;
	private volatile String historyResponseText = "";
	private volatile int suggestionsResponseId = -1;
	private volatile List<String> suggestionsResponseList = Collections.emptyList();
	private volatile boolean suggestionsResponsePathMode;

	ComputerSessionView(int entityId, long absIndex) {
		this.entityId = entityId;
		this.absIndex = absIndex;
	}

	public int getEntityId() {
		return entityId;
	}

	public long getAbsIndex() {
		return absIndex;
	}

	public boolean isConnected() {
		return connected;
	}

	public String getConnectFailureMessage() {
		return connectFailureMessage;
	}

	public String getConsoleText() {
		return consoleText;
	}

	/** May be null until the first frame arrives. */
	public Gfx2d.FrameSnapshot getGfxSnapshot() {
		return gfxSnapshot;
	}

	public boolean isKeyboardConsumed() {
		return keyboardConsumed;
	}

	public boolean isMouseConsumed() {
		return mouseConsumed;
	}

	public ComputerModule.ComputerMode getMode() {
		ComputerModule.ComputerMode[] modes = ComputerModule.ComputerMode.values();
		return modeOrdinal >= 0 && modeOrdinal < modes.length ? modes[modeOrdinal] : ComputerModule.ComputerMode.TERMINAL;
	}

	public boolean isFileEditMode() {
		return getMode() == ComputerModule.ComputerMode.FILE_EDIT;
	}

	public String getLastOpenFile() {
		return lastOpenFile;
	}

	public boolean isPasswordInputMode() {
		return passwordInputMode;
	}

	public ComputerModule.ScrollMode getScrollMode() {
		ComputerModule.ScrollMode[] modes = ComputerModule.ScrollMode.values();
		return scrollModeOrdinal >= 0 && scrollModeOrdinal < modes.length ? modes[scrollModeOrdinal] : ComputerModule.ScrollMode.VERTICAL;
	}

	public String getSavedTerminalInput() {
		return savedTerminalInput;
	}

	public int getHistoryResponseId() {
		return historyResponseId;
	}

	public String getHistoryResponseText() {
		return historyResponseText;
	}

	public int getSuggestionsResponseId() {
		return suggestionsResponseId;
	}

	public List<String> getSuggestionsResponseList() {
		return suggestionsResponseList;
	}

	public boolean isSuggestionsResponsePathMode() {
		return suggestionsResponsePathMode;
	}

	// ------------------------------------------------------------------
	// Inbound — called by ComputerSessionRegistry from packet handlers
	// (network thread).
	// ------------------------------------------------------------------

	void applyConnectSuccess(String consoleText, Gfx2d.FrameSnapshot gfxSnapshot, boolean keyboardConsumed, boolean mouseConsumed, byte modeOrdinal, String lastOpenFile, boolean passwordInputMode, byte scrollModeOrdinal, String savedTerminalInput) {
		this.connected = true;
		this.connectFailureMessage = null;
		this.consoleText = consoleText == null ? "" : consoleText;
		this.gfxSnapshot = gfxSnapshot;
		this.keyboardConsumed = keyboardConsumed;
		this.mouseConsumed = mouseConsumed;
		this.modeOrdinal = modeOrdinal;
		this.lastOpenFile = lastOpenFile == null ? "" : lastOpenFile;
		this.passwordInputMode = passwordInputMode;
		this.scrollModeOrdinal = scrollModeOrdinal;
		this.savedTerminalInput = savedTerminalInput == null ? "" : savedTerminalInput;
	}

	void applyConnectFailure(String message) {
		this.connected = false;
		this.connectFailureMessage = message;
	}

	void applyConsoleSnapshot(String consoleText, boolean keyboardConsumed, boolean mouseConsumed, byte modeOrdinal, String lastOpenFile, boolean passwordInputMode, byte scrollModeOrdinal, String savedTerminalInput) {
		this.consoleText = consoleText == null ? "" : consoleText;
		this.keyboardConsumed = keyboardConsumed;
		this.mouseConsumed = mouseConsumed;
		this.passwordInputMode = passwordInputMode;
		this.modeOrdinal = modeOrdinal;
		this.lastOpenFile = lastOpenFile == null ? "" : lastOpenFile;
		this.scrollModeOrdinal = scrollModeOrdinal;
		this.savedTerminalInput = savedTerminalInput == null ? "" : savedTerminalInput;
	}

	void applyGfxSnapshot(Gfx2d.FrameSnapshot snapshot) {
		this.gfxSnapshot = snapshot;
	}

	void applyHistoryResult(int requestId, String text) {
		this.historyResponseText = text == null ? "" : text;
		this.historyResponseId = requestId;
	}

	void applySuggestionsResult(int requestId, List<String> list, boolean pathMode) {
		this.suggestionsResponseList = list == null ? Collections.emptyList() : list;
		this.suggestionsResponsePathMode = pathMode;
		this.suggestionsResponseId = requestId;
	}

	// ------------------------------------------------------------------
	// Outbound — forward input over the network instead of calling a
	// local Terminal/InputApi.
	// ------------------------------------------------------------------

	public void sendConnect() {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.connect(0, entityId, absIndex));
	}

	public void sendDisconnect() {
		connected = false;
		PacketUtil.sendPacketToServer(PacketCSComputerInput.disconnect(entityId, absIndex));
	}

	public void sendLineInput(String text) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.lineInput(entityId, absIndex, text));
	}

	public void sendKeyEvent(int key, char character, boolean down, boolean shift, boolean ctrl, boolean alt) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.keyEvent(entityId, absIndex, key, character, down, shift, ctrl, alt));
	}

	public void sendMouseEvent(int button, boolean pressed, int x, int y, int dx, int dy, int wheel, int uiX, int uiY, boolean insideCanvas, boolean dragging, String dragButton) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.mouseEvent(entityId, absIndex, button, pressed, x, y, dx, dy, wheel, uiX, uiY, insideCanvas, dragging, dragButton));
	}

	public void sendInterrupt() {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.interrupt(entityId, absIndex));
	}

	public void sendReset() {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.reset(entityId, absIndex));
	}

	public void sendExitEditor() {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.exitEditor(entityId, absIndex));
	}

	public void sendViewportResize(int width, int height) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.viewportResize(entityId, absIndex, width, height));
	}

	/** Fire-and-forget — persists the partial input line so it can be restored if the dialog is reopened later. */
	public void sendSetSavedInput(String text) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.setSavedInput(entityId, absIndex, text));
	}

	/** Feeds the server-side InputApi so scripts calling {@code input.getUiLayout()} see real window/canvas bounds. */
	public void sendUiLayout(int windowX, int windowY, int windowWidth, int windowHeight, int canvasX, int canvasY, int canvasWidth, int canvasHeight) {
		PacketUtil.sendPacketToServer(PacketCSComputerInput.uiLayout(entityId, absIndex, windowX, windowY, windowWidth, windowHeight, canvasX, canvasY, canvasWidth, canvasHeight));
	}

	/**
	 * Requests the previous history entry (server also records {@code currentInput}
	 * as the "in progress" line, matching {@code Terminal.getPreviousCommand()}'s
	 * existing semantics). Async — call {@link #getHistoryResponseId()} /
	 * {@link #getHistoryResponseText()} on a later frame to pick up the result;
	 * never blocks the calling thread.
	 */
	public int sendHistoryPrev(String currentInput) {
		int requestId = queryRequestIdGenerator.getAndIncrement();
		ComputerSessionRegistry.registerPendingQuery(requestId, this);
		PacketUtil.sendPacketToServer(PacketCSTerminalQuery.historyPrev(requestId, entityId, absIndex, currentInput));
		return requestId;
	}

	public int sendHistoryNext() {
		int requestId = queryRequestIdGenerator.getAndIncrement();
		ComputerSessionRegistry.registerPendingQuery(requestId, this);
		PacketUtil.sendPacketToServer(PacketCSTerminalQuery.historyNext(requestId, entityId, absIndex));
		return requestId;
	}

	public int sendCommandSuggestions(String partialInput) {
		int requestId = queryRequestIdGenerator.getAndIncrement();
		ComputerSessionRegistry.registerPendingQuery(requestId, this);
		PacketUtil.sendPacketToServer(PacketCSTerminalQuery.commandSuggestions(requestId, entityId, absIndex, partialInput));
		return requestId;
	}

	public int sendPathSuggestions(String partialInput) {
		int requestId = queryRequestIdGenerator.getAndIncrement();
		ComputerSessionRegistry.registerPendingQuery(requestId, this);
		PacketUtil.sendPacketToServer(PacketCSTerminalQuery.pathSuggestions(requestId, entityId, absIndex, partialInput));
		return requestId;
	}

	/**
	 * Fire-and-forget file write — for GUI callers on the render thread that
	 * must not block waiting on a network round trip (e.g. the in-dialog
	 * editor's Ctrl+S). A rare failed save silently no-ops; use
	 * {@link #writeFileBlocking(String, String, long)} where a caller can
	 * afford to wait and wants to know the outcome (e.g. the Swing editor).
	 */
	public void sendFileWrite(String path, String content) {
		PacketUtil.sendPacketToServer(new PacketCSFileWrite(0, entityId, absIndex, path, content));
	}

	/** Blocking file write with a result. Safe from a background/Swing thread — must not be called from the render thread. */
	public FileIoRequests.ReadResult writeFileBlocking(String path, String content, long timeoutMs) throws InterruptedException, TimeoutException {
		CompletableFuture<FileIoRequests.ReadResult> future = new CompletableFuture<>();
		int requestId = FileIoRequests.allocate(future);
		try {
			PacketUtil.sendPacketToServer(new PacketCSFileWrite(requestId, entityId, absIndex, path, content));
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch(java.util.concurrent.ExecutionException e) {
			FileIoRequests.cancel(requestId);
			return new FileIoRequests.ReadResult(false, "Write failed: " + e.getMessage(), null);
		} catch(TimeoutException e) {
			FileIoRequests.cancel(requestId);
			throw e;
		}
	}

	/** Blocking file read with a result. Safe from a background/Swing thread — must not be called from the render thread. */
	public FileIoRequests.ReadResult readFileBlocking(String path, long timeoutMs) throws InterruptedException, TimeoutException {
		CompletableFuture<FileIoRequests.ReadResult> future = new CompletableFuture<>();
		int requestId = FileIoRequests.allocate(future);
		try {
			PacketUtil.sendPacketToServer(new PacketCSFileRead(requestId, entityId, absIndex, path));
			return future.get(timeoutMs, TimeUnit.MILLISECONDS);
		} catch(java.util.concurrent.ExecutionException e) {
			FileIoRequests.cancel(requestId);
			return new FileIoRequests.ReadResult(false, "Read failed: " + e.getMessage(), null);
		} catch(TimeoutException e) {
			FileIoRequests.cancel(requestId);
			throw e;
		}
	}

	public void sendClipboardImportFiles(Map<String, String> files) {
		PacketUtil.sendPacketToServer(PacketCSClipboardImport.ofFiles(entityId, absIndex, files));
	}

	public void sendClipboardImportProtocol(String protocolText) {
		PacketUtil.sendPacketToServer(PacketCSClipboardImport.ofProtocolText(entityId, absIndex, protocolText));
	}
}
