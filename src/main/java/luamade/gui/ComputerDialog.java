package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.schema.game.client.controller.PlayerInput;
import org.schema.schine.common.TabCallback;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComputerDialog extends PlayerInput {

	/** Tracks the currently open ComputerPanel so event listeners can access it. */
	private static ComputerPanel activePanel;
	/** Tracks the currently open dialog instance so listeners can force-close it. */
	private static ComputerDialog activeDialog;
	protected final ComputerModule computerModule;
	private final ComputerPanel computerPanel;

	public ComputerDialog(ComputerModule computerModule) {
		super(GameClient.getClientState());
		this.computerModule = computerModule;
		computerPanel = new ComputerPanel(getState(), this, computerModule);
	}

	public static ComputerPanel getActivePanel() {
		return activePanel;
	}

	public static ComputerDialog getActiveDialog() {
		return activeDialog;
	}

	public static void deactivateActiveDialog() {
		ComputerDialog dialog = activeDialog;
		if(dialog != null) {
			dialog.deactivate();
		}
	}

	@Override
	public void activate() {
		super.activate();
		activeDialog = this;
		activePanel = computerPanel;
		computerModule.resumeFromLastMode();
		computerPanel.requestConsoleFocus();
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
		if(!isOccluded() && mouseEvent.pressedLeftMouse()) {
			if(callingElement.getUserPointer() != null) {
				switch((String) callingElement.getUserPointer()) {
					case "DOCS":
						openDocumentationPanel();
						break;
					case "RESET":
						resetComputerRuntime();
						break;
					case "X":
					case "CANCEL":
						deactivate();
						break;
					case "OK":
						deactivate();
						break;
				}
			}
		}
	}

	private void openDocumentationPanel() {
		deactivate();
		new DocsViewerDialog(computerModule).activate();
	}

	private void resetComputerRuntime() {
		if(computerModule == null || computerModule.getTerminal() == null) {
			return;
		}

		computerModule.setLastMode(ComputerModule.ComputerMode.TERMINAL);
		computerModule.setSavedTerminalInput("");
		computerModule.getTerminal().hardReset();
		if(computerPanel != null) {
			computerPanel.requestConsoleFocus();
		}
	}

	@Override
	public ComputerPanel getInputPanel() {
		return computerPanel;
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
		if(computerModule == null) return;
		if(computerPanel != null) {
			computerPanel.pushMouseEvent(mouseEvent);
		}
	}

	@Override
	public void onDeactivate() {
		if(activeDialog == this) {
			activeDialog = null;
		}
		if(activePanel == computerPanel) {
			activePanel = null;
		}
		if(computerPanel != null) {
			computerPanel.saveCurrentInput();
			computerPanel.resetMouseState();
		}
		if(computerModule != null) {
			computerModule.getInputApi().clear();
		}
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private static final String PROMPT_MARKER = " $ ";
		private static final int LINE_WRAP = 100;
		private static final String EDITOR_HINT_PREFIX = "Editor: Ctrl+S Save | Ctrl+X Exit | Ctrl+R Save & Run";
		private static final int DOCS_BUTTON_OFFSET_X = 12;
		private static final int DOCS_BUTTON_OFFSET_Y = 30;
		private static final int RESET_BUTTON_GAP_X = 8;
		private static final long SUGGESTION_IDLE_DELAY_MS = 3000L;
		private static final int SUGGESTION_PREVIEW_LIMIT = 4;
		/** Pixel height of the console text-box in terminal mode. */
		private static final int TEXT_BOX_HEIGHT = 500;
		/** Pixels reserved at the bottom for the editor hint bar overlay. */
		private static final int EDITOR_HINT_RESERVE_PX = 36;

		private final ComputerModule computerModule;
		private GUIScrollablePanel consolePanel;
		private GUIActivatableTextBar consolePane;
		private String currentInputLine = "";
		private String lastModuleContent = "";
		private boolean userIsTyping;
		private int promptStartPosition = -1;
		private String lastSavedInput = "";
		private GUIScrollablePanel textBarScrollPanel;
		private boolean textBarScrollPanelResolved;
		private boolean focusConsoleOnOpen = true;
		private ComputerModule.ComputerMode renderedMode = ComputerModule.ComputerMode.OFF;
		private GUITextOverlay editorHintsOverlay;
		private GUITextButton docsButton;
		private GUITextButton resetButton;
		private TerminalGfxOverlay terminalGfxOverlay;
		private String lastEditorHintText = "";
		/** Reference to the content pane so we can adjust text-box height dynamically. */
		private GUIContentPane mainContentPane;
		/**
		 * Last cursor state at which we forced auto-scroll. We skip repeated
		 * frame-forced scrolling when neither the cursor line nor wrapped line count
		 * changed so users can manually scroll in both terminal and editor modes.
		 */
		private int lastAutoScrollCursorLine = -1;
		private int lastAutoScrollTotalLines = -1;
		/** Tracks console text length to follow new output without forcing scroll every frame. */
		private int lastAutoFollowContentLength = -1;
		private boolean leftMouseDown;
		private boolean rightMouseDown;
		private boolean middleMouseDown;
		private final List<String> commandSuggestions = new ArrayList<>();
		private int selectedSuggestionIndex = -1;
		private String suggestionSeedInput = "";
		private boolean pathCompletionMode;
		private String pathCompletionPrefix = "";
		private long lastInputEditAtMs = System.currentTimeMillis();

		public ComputerPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "COMPUTER_PANEL", "", "", 850, 650, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(true);
		}

		private static String stripAnsi(String text) {
			if(text == null || text.isEmpty()) {
				return "";
			}
			return text.replaceAll("\\u001B\\[[0-9;]*m", "");
		}

		/**
		 * Saves the current input line to the computer module for persistence
		 */
		public void saveCurrentInput() {
			if(computerModule != null) {
				computerModule.setSavedTerminalInput(currentInputLine);
			}
		}

		public void requestConsoleFocus() {
			focusConsoleOnOpen = true;
			activateConsoleFocusIfPending();
		}

		public void pushMouseEvent(MouseEvent mouseEvent) {
			if(mouseEvent == null || computerModule == null) {
				return;
			}

			int button = mouseEvent.button;
			boolean pressed = mouseEvent.state;
			boolean hasMovement = mouseEvent.dx != 0 || mouseEvent.dy != 0;
			if(button < 0 && mouseEvent.dWheel == 0 && !hasMovement) {
				return;
			}

			updateMouseButtonState(button, pressed);
			boolean dragging = leftMouseDown || rightMouseDown || middleMouseDown;
			String dragButton = dragButtonName();

			int uiX = -1;
			int uiY = -1;
			boolean insideCanvas = false;
			if(consolePane != null) {
				int width = Math.max(1, Math.round(consolePane.getWidth()));
				int height = Math.max(1, Math.round(consolePane.getHeight()));
				int localX = Math.round(mouseEvent.x - consolePane.getPos().x);
				int localY = Math.round(mouseEvent.y - consolePane.getPos().y);
				insideCanvas = localX >= 0 && localY >= 0 && localX < width && localY < height;
				if(insideCanvas) {
					uiX = localX;
					uiY = localY;
				}
			}

			computerModule.getInputApi().pushMouseEvent(
				button,
				pressed,
				mouseEvent.x,
				mouseEvent.y,
				mouseEvent.dx,
				mouseEvent.dy,
				mouseEvent.dWheel,
				-1,
				-1,
				uiX,
				uiY,
				insideCanvas,
				dragging,
				dragButton
			);
		}

		private void updateMouseButtonState(int button, boolean pressed) {
			switch(button) {
				case 0:
					leftMouseDown = pressed;
					break;
				case 1:
					rightMouseDown = pressed;
					break;
				case 2:
					middleMouseDown = pressed;
					break;
			}
		}

		private String dragButtonName() {
			if(leftMouseDown) {
				return "left";
			}
			if(rightMouseDown) {
				return "right";
			}
			if(middleMouseDown) {
				return "middle";
			}
			return "none";
		}

		public void resetMouseState() {
			leftMouseDown = false;
			rightMouseDown = false;
			middleMouseDown = false;
		}

		public boolean isFileEditMode() {
			return computerModule != null && computerModule.getLastMode() == ComputerModule.ComputerMode.FILE_EDIT;
		}

		/** Returns the {@link ComputerModule} associated with this panel (may be null). */
		public ComputerModule getComputerModule() {
			return computerModule;
		}

		/**
		 * Returns true if the console text area currently has a non-empty text selection.
		 * Used to decide whether Ctrl+C should kill the foreground script or copy selected text.
		 */
		public boolean hasSelectedText() {
			if(consolePane == null) return false;
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) return false;
			String selected = textArea.getCacheSelect();
			return selected != null && !selected.isEmpty();
		}

		public void handleEditorShortcut(int glfwKey) {
			if(!isFileEditMode()) {
				return;
			}

			switch(glfwKey) {
				case GLFW.GLFW_KEY_S:
					saveEditorBuffer();
					break;
				case GLFW.GLFW_KEY_X:
					exitEditorToTerminal();
					break;
				case GLFW.GLFW_KEY_R:
					runEditorFile();
					break;
			}
		}

		private void saveEditorBuffer() {
			if(!isFileEditMode() || consolePane == null) {
				return;
			}

			String file = computerModule.getLastOpenFile();
			if(file == null || file.isEmpty()) {
				return;
			}

			String content = consolePane.getText();
			if(content == null) {
				content = "";
			}
			computerModule.getFileSystem().write(file, content);
		}

		private void exitEditorToTerminal() {
			if(computerModule == null) {
				return;
			}

			computerModule.setLastMode(ComputerModule.ComputerMode.TERMINAL);
			lastModuleContent = computerModule.getLastTextContent();
			if(consolePane != null) {
				consolePane.setTextWithoutCallback(lastModuleContent);
			}
			currentInputLine = "";
			userIsTyping = false;
			clearCommandSuggestions();
			lastAutoScrollCursorLine = -1;
			lastAutoScrollTotalLines = -1;
			if(mainContentPane != null) {
				mainContentPane.setTextBoxHeightLast(TEXT_BOX_HEIGHT);
			}
			refreshPromptStartPositionFromCurrentText();
			scrollPaneToCursor();
			requestConsoleFocus();
		}

		private void runEditorFile() {
			if(!isFileEditMode() || computerModule == null) {
				return;
			}

			String file = computerModule.getLastOpenFile();
			saveEditorBuffer();
			exitEditorToTerminal();
			if(file != null && !file.isEmpty()) {
				computerModule.getTerminal().handleInput("run " + file);
			}
		}

		private void updateEditorHintOverlay() {
			if(editorHintsOverlay == null) {
				return;
			}

			boolean inEditor = isFileEditMode();
			if(!inEditor) {
				String hintText = buildTerminalSuggestionHint();
				if(!hintText.equals(lastEditorHintText)) {
					editorHintsOverlay.setTextSimple(hintText);
					lastEditorHintText = hintText;
				}
				positionHintOverlay();
				return;
			}

			String currentFile = computerModule.getLastOpenFile();
			if(currentFile == null || currentFile.isEmpty()) {
				currentFile = "(unspecified file)";
			}

			String hintText = EDITOR_HINT_PREFIX + " | " + currentFile;
			if(!hintText.equals(lastEditorHintText)) {
				editorHintsOverlay.setTextSimple(hintText);
				lastEditorHintText = hintText;
			}

			positionHintOverlay();
		}

		private String buildTerminalSuggestionHint() {
			if(commandSuggestions.isEmpty() || selectedSuggestionIndex < 0 || selectedSuggestionIndex >= commandSuggestions.size()) {
				return "";
			}

			String selectedSuggestion = commandSuggestions.get(selectedSuggestionIndex);
			// In path completion mode show only the path token, not the full input line.
			String displaySelected = pathCompletionMode && selectedSuggestion.length() >= pathCompletionPrefix.length()
					? selectedSuggestion.substring(pathCompletionPrefix.length())
					: selectedSuggestion;
			StringBuilder builder = new StringBuilder();
			builder.append("Suggest ")
					.append(selectedSuggestionIndex + 1)
					.append("/")
					.append(commandSuggestions.size())
					.append(": ")
					.append(displaySelected)
					.append("  (Tab accept, Up/Down cycle)");

			int shown = 0;
			for(String suggestion : commandSuggestions) {
				if(suggestion == null || suggestion.isEmpty() || suggestion.equals(selectedSuggestion)) {
					continue;
				}
				String displaySuggestion = pathCompletionMode && suggestion.length() >= pathCompletionPrefix.length()
						? suggestion.substring(pathCompletionPrefix.length())
						: suggestion;
				if(shown == 0) {
					builder.append("  [");
				} else {
					builder.append(", ");
				}
				builder.append(displaySuggestion);
				shown++;
				if(shown >= SUGGESTION_PREVIEW_LIMIT) {
					break;
				}
			}

			if(shown > 0) {
				builder.append("]");
			}

			return builder.toString();
		}

		private void positionHintOverlay() {
			if(editorHintsOverlay == null) {
				return;
			}

			float x = 8.0F;
			float y;
			if(getButtonOK() != null) {
				y = Math.max(0.0F, getButtonOK().getPos().y - editorHintsOverlay.getTextHeight() - 6.0F);
			} else if(background != null) {
				y = Math.max(0.0F, background.getHeight() - 48.0F);
			} else {
				y = Math.max(0.0F, getHeight() - 48.0F);
			}
			editorHintsOverlay.setPos(x, y, 0.0F);
		}

		private void updateActionButtonPositions() {
			if(docsButton == null || resetButton == null || getButtonOK() == null) {
				return;
			}

			int x = (int) (getButtonOK().getPos().x + getButtonOK().getWidth() + 5) + DOCS_BUTTON_OFFSET_X;
			int y = (int) getButtonOK().getPos().y + DOCS_BUTTON_OFFSET_Y;
			if(y <= 0 && background != null) {
				y = (int) (background.getHeight() - (42 + docsButton.getHeight())) + DOCS_BUTTON_OFFSET_Y;
			}
			docsButton.setPos(x, y, 0);
			resetButton.setPos(x + docsButton.getWidth() + RESET_BUTTON_GAP_X, y, 0);
		}

		private void activateConsoleFocusIfPending() {
			if(!focusConsoleOnOpen || consolePane == null) {
				return;
			}

			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) {
				return;
			}

			consolePane.activateBar();
			int caretPosition = textArea.getCache() == null ? 0 : textArea.getCache().length();
			if(promptStartPosition >= 0) {
				caretPosition = Math.max(caretPosition, promptStartPosition);
			}
			textArea.setChatCarrier(caretPosition);
			textArea.setBufferChanged();
			textArea.update();
			clampCaretToEditableRegion();
			scrollPaneToCursor();

			focusConsoleOnOpen = getState().getController().getInputController().getCurrentActiveField() != textArea;
		}

		private void updateGfxOverlayBounds() {
			if(consolePane == null || terminalGfxOverlay == null) {
				return;
			}

			float x = consolePane.getPos().x;
			float y = consolePane.getPos().y;
			int width = Math.max(1, Math.round(consolePane.getWidth()));
			int height = Math.max(1, Math.round(consolePane.getHeight()));

			GUIScrollablePanel scrollPanel = resolveTextBarScrollPanel();
			if(scrollPanel != null) {
				x += scrollPanel.getPos().x;
				y += scrollPanel.getPos().y;
				width = Math.max(1, Math.round(scrollPanel.getWidth()));
				height = Math.max(1, Math.round(scrollPanel.getHeight()));
			}

			terminalGfxOverlay.setCanvasBounds(x, y, width, height);
			terminalGfxOverlay.setCanvasEnabled(!isFileEditMode());
		}

		private void refreshPromptStartPositionFromCurrentText() {
			if(consolePane == null) {
				promptStartPosition = -1;
				return;
			}

			String fullText = consolePane.getText();
			if(fullText == null || fullText.isEmpty()) {
				promptStartPosition = -1;
				return;
			}

			int lastLineStart = fullText.lastIndexOf('\n') + 1;
			String lastLine = fullText.substring(lastLineStart);
			int promptIndex = lastLine.lastIndexOf(PROMPT_MARKER);
			if(promptIndex == -1) {
				promptStartPosition = -1;
				return;
			}

			promptStartPosition = lastLineStart + promptIndex + PROMPT_MARKER.length();
		}

		public void clampCaretToEditableRegion() {
			if(consolePane == null) {
				return;
			}

			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null || promptStartPosition < 0) {
				return;
			}

			int currentCarrier = textArea.getChatCarrier();
			if(currentCarrier < promptStartPosition) {
				textArea.setChatCarrier(promptStartPosition);
				textArea.setBufferChanged();
			}
		}

		private GUIScrollablePanel resolveTextBarScrollPanel() {
			if(textBarScrollPanelResolved) {
				return textBarScrollPanel;
			}
			textBarScrollPanelResolved = true;

			if(consolePane == null) {
				return null;
			}

			try {
				Field backgroundField = GUIActivatableTextBar.class.getDeclaredField("background");
				backgroundField.setAccessible(true);
				Object bg = backgroundField.get(consolePane);
				if(bg instanceof GUIScrollablePanel) {
					textBarScrollPanel = (GUIScrollablePanel) bg;
				}
			} catch(Exception ignored) {
				textBarScrollPanel = null;
			}

			return textBarScrollPanel;
		}

		private void scrollPaneToCursor() {
			if(consolePane == null) {
				return;
			}

			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) {
				return;
			}

			GUIScrollablePanel scrollPanel = resolveTextBarScrollPanel();
			if(scrollPanel == null) {
				return;
			}

			int totalLines = Math.max(1, textArea.getLineIndex() + 1);
			int cursorLine = Math.max(0, textArea.getCarrierLineIndex());

			if(cursorLine == lastAutoScrollCursorLine && totalLines == lastAutoScrollTotalLines) {
				return;
			}
			lastAutoScrollCursorLine = cursorLine;
			lastAutoScrollTotalLines = totalLines;

			if(totalLines <= 1) {
				scrollPanel.scrollVerticalPercent(0.0F);
				return;
			}

			float cursorPercent = Math.min(1.0F, Math.max(0.0F, cursorLine / (float) (totalLines - 1)));
			scrollPanel.scrollVerticalPercent(cursorPercent);
		}

		private void scrollPaneToBottom() {
			GUIScrollablePanel scrollPanel = resolveTextBarScrollPanel();
			if(scrollPanel == null) {
				return;
			}

			scrollPanel.scrollVerticalPercent(1.0F);
		}

		private void followOutputIfChanged(String content) {
			String safeContent = content == null ? "" : content;
			int length = safeContent.length();
			if(length == lastAutoFollowContentLength) {
				return;
			}

			lastAutoFollowContentLength = length;
			scrollPaneToBottom();
			lastAutoScrollCursorLine = -1;
			lastAutoScrollTotalLines = -1;
		}

		private void handleHistoryUp() {
			if(computerModule == null || computerModule.getTerminal() == null) return;
			computerModule.getTerminal().setCurrentInput(currentInputLine);
			String command = computerModule.getTerminal().getPreviousCommand();
			setHistoryCommand(command);
		}

		private void handleHistoryDown() {
			if(computerModule == null || computerModule.getTerminal() == null) return;
			String command = computerModule.getTerminal().getNextCommand();
			setHistoryCommand(command);
		}

		private void setHistoryCommand(String command) {
			if(command == null) {
				command = "";
			}
			setTerminalInputLine(command, false);
		}

		private void setTerminalInputLine(String command, boolean keepSuggestions) {
			if(consolePane == null || isFileEditMode()) {
				return;
			}

			String normalized = command == null ? "" : command;
			currentInputLine = normalized;
			userIsTyping = !normalized.isEmpty();
			consolePane.setTextWithoutCallback(lastModuleContent + normalized);
			refreshPromptStartPositionFromCurrentText();
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea != null) {
				textArea.setChatCarrier(textArea.getCache().length());
				textArea.setBufferChanged();
				textArea.update();
			}
			scrollPaneToCursor();

			if(keepSuggestions) {
				suggestionSeedInput = normalized;
			} else {
				clearCommandSuggestions();
				lastInputEditAtMs = System.currentTimeMillis();
			}
		}

		private void clearCommandSuggestions() {
			commandSuggestions.clear();
			selectedSuggestionIndex = -1;
			suggestionSeedInput = "";
			pathCompletionMode = false;
			pathCompletionPrefix = "";
		}

		private boolean refreshCommandSuggestions(boolean force) {
			if(isFileEditMode() || computerModule == null || computerModule.getTerminal() == null) {
				clearCommandSuggestions();
				return false;
			}

			String normalizedInput = currentInputLine == null ? "" : currentInputLine.trim();
			if(normalizedInput.isEmpty()) {
				clearCommandSuggestions();
				return false;
			}

			if(!force && normalizedInput.equals(suggestionSeedInput) && !commandSuggestions.isEmpty()) {
				return true;
			}

			List<String> suggestions = computerModule.getTerminal().getCommandSuggestions(normalizedInput);
			if(suggestions == null || suggestions.isEmpty()) {
				clearCommandSuggestions();
				return false;
			}

			commandSuggestions.clear();
			commandSuggestions.addAll(suggestions);
			suggestionSeedInput = normalizedInput;

			selectedSuggestionIndex = 0;
			for(int i = 0; i < commandSuggestions.size(); i++) {
				if(commandSuggestions.get(i).equalsIgnoreCase(normalizedInput)) {
					selectedSuggestionIndex = i;
					break;
				}
			}

			return true;
		}

		private boolean cycleCommandSuggestion(int direction) {
			if(commandSuggestions.isEmpty()) {
				return false;
			}

			int count = commandSuggestions.size();
			selectedSuggestionIndex = (selectedSuggestionIndex + direction) % count;
			if(selectedSuggestionIndex < 0) {
				selectedSuggestionIndex += count;
			}

			setTerminalInputLine(commandSuggestions.get(selectedSuggestionIndex), true);
			return true;
		}

		public void handleTabAutocomplete() {
			if(isFileEditMode()) {
				return;
			}

			String input = currentInputLine == null ? "" : currentInputLine;
			boolean hasArgs = input.contains(" ");

			if(hasArgs) {
				// Path/file completion mode: complete the last argument token.
				if(!commandSuggestions.isEmpty() && pathCompletionMode) {
					cycleCommandSuggestion(1);
					return;
				}

				if(computerModule == null || computerModule.getTerminal() == null) {
					return;
				}

				List<String> paths = computerModule.getTerminal().getPathSuggestions(input);
				if(paths == null || paths.isEmpty()) {
					return;
				}

				// Build full input-line replacements so cycling replaces only the last token.
				pathCompletionPrefix = getInputPrefixBeforeLastToken(input);
				pathCompletionMode = true;
				commandSuggestions.clear();
				for(String path : paths) {
					commandSuggestions.add(pathCompletionPrefix + path);
				}
				suggestionSeedInput = input.trim();
				selectedSuggestionIndex = 0;
				setTerminalInputLine(commandSuggestions.get(0), true);
				return;
			}

			// Command completion mode (first token only).
			if(!commandSuggestions.isEmpty()) {
				cycleCommandSuggestion(1);
				return;
			}

			if(!refreshCommandSuggestions(true)) {
				return;
			}

			if(selectedSuggestionIndex < 0 || selectedSuggestionIndex >= commandSuggestions.size()) {
				selectedSuggestionIndex = 0;
			}

			setTerminalInputLine(commandSuggestions.get(selectedSuggestionIndex), true);
		}

		private String getInputPrefixBeforeLastToken(String input) {
			if(input == null) return "";
			int lastSpace = input.lastIndexOf(' ');
			if(lastSpace < 0) return "";
			return input.substring(0, lastSpace + 1);
		}

		private void handleCaretLeft() {
			if(consolePane == null) return;
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) return;
			if(textArea.getChatCarrier() > promptStartPosition) {
				textArea.chatKeyLeft();
			}
		}

		private void handleCaretRight() {
			if(consolePane == null) return;
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) return;
			textArea.chatKeyRight();
		}

		private void handleCaretHome() {
			if(consolePane == null) return;
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null || promptStartPosition < 0) return;
			textArea.setChatCarrier(promptStartPosition);
			textArea.setBufferChanged();
			textArea.update();
		}

		private void handleCaretEnd() {
			if(consolePane == null) return;
			consolePane.getTextArea().chatKeyEnd();
			scrollPaneToCursor();
		}

		/**
		 * Handles navigation key events intercepted before they reach TextAreaInput.
		 * Called from EventManager's KeyPressEvent listener.
		 */
		public void handleNavigationKey(int glfwKey) {
			switch(glfwKey) {
				case GLFW.GLFW_KEY_UP:
					handleHistoryUp();
					break;
				case GLFW.GLFW_KEY_DOWN:
					handleHistoryDown();
					break;
				case GLFW.GLFW_KEY_LEFT:
					handleCaretLeft();
					break;
				case GLFW.GLFW_KEY_RIGHT:
					handleCaretRight();
					break;
				case GLFW.GLFW_KEY_HOME:
					handleCaretHome();
					break;
				case GLFW.GLFW_KEY_END:
					handleCaretEnd();
					break;
			}
		}

		/**
		 * Executes the current input line as a terminal command
		 */
		private void executeCurrentInput() {
			if(isFileEditMode()) {
				return;
			}
			clearCommandSuggestions();

			if(computerModule != null && computerModule.getTerminal() != null) {
				String inputToExecute = currentInputLine;
				currentInputLine = "";

				computerModule.getTerminal().handleInput(inputToExecute);
				computerModule.setSavedTerminalInput("");

				userIsTyping = false;

				String newContent = computerModule.getLastTextContent();
				lastModuleContent = newContent;
				consolePane.setTextWithoutCallback(newContent);
				followOutputIfChanged(newContent);
				currentInputLine = "";
				userIsTyping = false;
				refreshPromptStartPositionFromCurrentText();
				scrollPaneToCursor();
			}
		}

		@Override
		public void onInit() {
			super.onInit();
			if(computerModule == null) {
				return;
			}
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(TEXT_BOX_HEIGHT);
			mainContentPane = contentPane;

			consolePanel = new GUIScrollablePanel(850, 650, contentPane.getContent(0), getState());
			consolePane = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, ConfigManager.getConsoleCharacterLimit(), ConfigManager.getConsoleLineLimit(), "", contentPane.getContent(0), new TextCallback() {
				@Override
				public String[] getCommandPrefixes() {
					return new String[0];
				}

				@Override
				public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
					return "";
				}

				@Override
				public void onFailedTextCheck(String s) {
				}

				@Override
				public void onTextEnter(String input, boolean b, boolean b1) {
					executeCurrentInput();
				}

				@Override
				public void newLine() {
					executeCurrentInput();
				}
			}, input -> {
				String previousInputLine = currentInputLine;
				if(isFileEditMode()) {
					userIsTyping = true;
					return input;
				}

				if(input != null && lastModuleContent != null && !lastModuleContent.isEmpty()) {
					if(!input.startsWith(lastModuleContent)) {
						return lastModuleContent + currentInputLine;
					}
				}

				if(input != null) {
					String[] lines = input.split("\n");
					if(lines.length > 0) {
						String lastLine = lines[lines.length - 1];
						int promptIndex = lastLine.lastIndexOf(PROMPT_MARKER);
						if(promptIndex != -1) {
							int lineStartPos = 0;
							for(int i = 0; i < lines.length - 1; i++) {
								lineStartPos += lines[i].length() + 1;
							}
							promptStartPosition = lineStartPos + promptIndex + PROMPT_MARKER.length();

							if(lastLine.length() > promptIndex + PROMPT_MARKER.length()) {
								currentInputLine = lastLine.substring(promptIndex + PROMPT_MARKER.length());
								userIsTyping = true;
							} else {
								currentInputLine = "";
								userIsTyping = false;
							}
						} else {
							currentInputLine = "";
							userIsTyping = false;
						}
					}
				}

				clampCaretToEditableRegion();
				if(!Objects.equals(previousInputLine, currentInputLine)) {
					lastInputEditAtMs = System.currentTimeMillis();
					clearCommandSuggestions();
				}

				return input;
			}) {
				@Override
				public void draw() {
					updateEditorHintOverlay();
					updateActionButtonPositions();
					activateConsoleFocusIfPending();

					ComputerModule.ComputerMode currentMode = computerModule.getLastMode();
					if(renderedMode != currentMode) {
						renderedMode = currentMode;
						lastAutoScrollCursorLine = -1;
						lastAutoScrollTotalLines = -1;
						lastAutoFollowContentLength = -1;
						String modeContent = computerModule.getLastTextContent();
						setTextWithoutCallback(modeContent == null ? "" : modeContent);
						lastModuleContent = modeContent == null ? "" : modeContent;
						followOutputIfChanged(lastModuleContent);
						if(isFileEditMode()) {
							userIsTyping = true;
							currentInputLine = "";
							clearCommandSuggestions();
							if(mainContentPane != null) {
								mainContentPane.setTextBoxHeightLast(TEXT_BOX_HEIGHT - EDITOR_HINT_RESERVE_PX);
							}
						} else {
							userIsTyping = false;
							clearCommandSuggestions();
							refreshPromptStartPositionFromCurrentText();
							requestConsoleFocus();
							if(mainContentPane != null) {
								mainContentPane.setTextBoxHeightLast(TEXT_BOX_HEIGHT);
							}
						}
					}

					if(isFileEditMode()) {
						super.draw();
						scrollPaneToCursor();
						return;
					}

					if(!currentInputLine.equals(lastSavedInput)) {
						computerModule.setSavedTerminalInput(currentInputLine);
						lastSavedInput = currentInputLine;
					}

					if(!userIsTyping) {
						String moduleContent = computerModule.getLastTextContent();
						if(!Objects.equals(lastModuleContent, moduleContent)) {
							lastModuleContent = moduleContent;
							setTextWithoutCallback(moduleContent);
							followOutputIfChanged(moduleContent);
							currentInputLine = "";
							clearCommandSuggestions();
							refreshPromptStartPositionFromCurrentText();
						}
					}

					if(currentInputLine != null && !currentInputLine.trim().isEmpty()) {
						long nowMs = System.currentTimeMillis();
						if(nowMs - lastInputEditAtMs >= SUGGESTION_IDLE_DELAY_MS) {
							refreshCommandSuggestions(false);
						}
					}

					refreshPromptStartPositionFromCurrentText();
					clampCaretToEditableRegion();
					scrollPaneToCursor();
					super.draw();
				}
			};
			consolePanel.setContent(consolePane);
			consolePane.getTextArea().getChatLog().clear();
			consolePane.getTextArea().onTabCallback = new TabCallback() {
				@Override
				public boolean catchTab(TextAreaInput textAreaInput) {
					handleTabAutocomplete();
					return true;
				}

				@Override
				public void onEnter() {
					executeCurrentInput();
				}
			};
			consolePane.onInit();
			contentPane.getContent(0).attach(consolePane);

			terminalGfxOverlay = new TerminalGfxOverlay(1, 1, GameClient.getClientState(), computerModule.getGfxApi());
			terminalGfxOverlay.onInit();
			terminalGfxOverlay.setMouseUpdateEnabled(false);
			contentPane.getContent(0).attach(terminalGfxOverlay);
			updateGfxOverlayBounds();

			editorHintsOverlay = new GUITextOverlay(830, 16, FontLibrary.FontSize.SMALL, getState());
			editorHintsOverlay.setTextSimple(EDITOR_HINT_PREFIX);
			editorHintsOverlay.onInit();
			editorHintsOverlay.setColor(0.8F, 0.8F, 0.8F, 1.0F);
			editorHintsOverlay.setTextSimple("");
			((GUIDialogWindow) background).attachSuper(editorHintsOverlay);

			docsButton = new GUITextButton(getState(), 90, 20, GUITextButton.ColorPalette.OK, "DOCS", getCallback());
			docsButton.setUserPointer("DOCS");
			docsButton.setMouseUpdateEnabled(true);
			docsButton.onInit();

			resetButton = new GUITextButton(getState(), 90, 20, GUITextButton.ColorPalette.CANCEL, "RESET", getCallback());
			resetButton.setUserPointer("RESET");
			resetButton.setMouseUpdateEnabled(true);
			resetButton.onInit();

			updateActionButtonPositions();
			((GUIDialogWindow) background).attachSuper(docsButton);
			((GUIDialogWindow) background).attachSuper(resetButton);

			consolePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			consolePane.getTextArea().setLinewrap(LINE_WRAP);
			String initialContent = computerModule.getLastTextContent();

			if(isFileEditMode()) {
				currentInputLine = "";
				userIsTyping = true;
				clearCommandSuggestions();
			} else {
				String savedInput = computerModule.getSavedTerminalInput();
				if(savedInput != null && !savedInput.isEmpty()) {
					initialContent = initialContent + savedInput;
					currentInputLine = savedInput;
					userIsTyping = true;
					lastInputEditAtMs = System.currentTimeMillis();
				}
			}

			consolePane.setTextWithoutCallback(initialContent);
			lastModuleContent = computerModule.getLastTextContent();
			lastAutoFollowContentLength = lastModuleContent == null ? 0 : lastModuleContent.length();
			renderedMode = computerModule.getLastMode();
			updateGfxOverlayBounds();
			refreshPromptStartPositionFromCurrentText();
			scrollPaneToCursor();
			updateEditorHintOverlay();
			requestConsoleFocus();
		}

		@Override
		public void draw() {
			updateGfxOverlayBounds();
			super.draw();
			clampCaretToEditableRegion();
			scrollPaneToCursor();
			updateActionButtonPositions();
		}
	}
}
