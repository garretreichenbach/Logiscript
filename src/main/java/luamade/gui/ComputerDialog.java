package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import luamade.lua.Console;
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

import javax.vecmath.Vector3f;
import java.lang.reflect.Field;
import java.util.Objects;

public class ComputerDialog extends PlayerInput {

	protected final ComputerModule computerModule;
	private final ComputerPanel computerPanel;

	/** Tracks the currently open ComputerPanel so event listeners can access it. */
	private static ComputerPanel activePanel;

	public static ComputerPanel getActivePanel() {
		return activePanel;
	}

	public ComputerDialog(ComputerModule computerModule) {
		super(GameClient.getClientState());
		this.computerModule = computerModule;
		computerPanel = new ComputerPanel(getState(), this, computerModule);
	}

	@Override
	public void activate() {
		super.activate();
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

	@Override
	public ComputerPanel getInputPanel() {
		return computerPanel;
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
		if(computerModule == null) return;
		int cellX = -1;
		int cellY = -1;
		if(computerPanel != null) {
			int[] mapped = computerPanel.mapMouseToCanvasCell(mouseEvent.x, mouseEvent.y);
			if(mapped != null) {
				cellX = mapped[0];
				cellY = mapped[1];
			}
		}
		// Determine which button triggered this event (-1 = move/scroll only)
		int button = mouseEvent.button;
		boolean pressed = mouseEvent.state;
		// Only report actual button events (button >= 0) or scroll (dWheel != 0)
		if(button >= 0 || mouseEvent.dWheel != 0) {
			computerModule.getInputApi().pushMouseEvent(
					button,
					pressed,
					mouseEvent.x,
					mouseEvent.y,
					mouseEvent.dx,
					mouseEvent.dy,
					mouseEvent.dWheel,
					cellX,
					cellY
			);
		}
	}

	@Override
	public void onDeactivate() {
		// Clear the active panel reference so the event listener stops intercepting
		if(activePanel == computerPanel) {
			activePanel = null;
		}
		// Save current input when closing the dialog
		if(computerPanel != null) {
			computerPanel.saveCurrentInput();
		}
		// Discard any pending input events so they don't bleed into the next session
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
		/** Pixel height of the console text-box in terminal mode. */
		private static final int TEXT_BOX_HEIGHT = 500;
		/** Pixels reserved at the bottom for the editor hint bar overlay. */
		private static final int EDITOR_HINT_RESERVE_PX = 36;
		private static final float CANVAS_PADDING_X = 8.0F;
		private static final float CANVAS_PADDING_Y = 8.0F;

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
		private GUITextOverlay graphicsFrameOverlay;
		private GUITextButton docsButton;
		private String lastEditorHintText = "";
		private long lastGraphicsFrameRevision = -1L;
		/** Reference to the content pane so we can adjust text-box height dynamically. */
		private GUIContentPane mainContentPane;
		/**
		 * Last cursor line at which we forced auto-scroll. In editor mode we skip
		 * the auto-scroll when the cursor hasn't moved so the user can scroll freely.
		 */
		private int lastAutoScrollCursorLine = -1;

		public ComputerPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "COMPUTER_PANEL", "", "", 850, 650, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(true);
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

		public boolean isFileEditMode() {
			return computerModule != null && computerModule.getLastMode() == ComputerModule.ComputerMode.FILE_EDIT;
		}

		/** Returns the {@link ComputerModule} associated with this panel (may be null). */
		public ComputerModule getComputerModule() {
			return computerModule;
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
			lastAutoScrollCursorLine = -1;
			// Restore full text-box height when returning to terminal mode.
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
				if(!lastEditorHintText.isEmpty()) {
					editorHintsOverlay.setTextSimple("");
					lastEditorHintText = "";
				}
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

		private void updateDocsButtonPosition() {
			if(docsButton == null || getButtonOK() == null) {
				return;
			}

			int x = (int) (getButtonOK().getPos().x + getButtonOK().getWidth() + 5) + DOCS_BUTTON_OFFSET_X;
			int y = (int) getButtonOK().getPos().y + DOCS_BUTTON_OFFSET_Y;
			if(y <= 0 && background != null) {
				y = (int) (background.getHeight() - (42 + docsButton.getHeight())) + DOCS_BUTTON_OFFSET_Y;
			}
			docsButton.setPos(x, y, 0);
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
				// Force bufferChanged so update() actually refreshes cacheCarrier
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
				Object background = backgroundField.get(consolePane);
				if(background instanceof GUIScrollablePanel) {
					textBarScrollPanel = (GUIScrollablePanel) background;
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

			// In editor mode, only force-scroll when the cursor line has actually moved.
			// Without this guard the scroll is overridden every frame, making the
			// scroll bar impossible to use while editing.
			if(isFileEditMode() && cursorLine == lastAutoScrollCursorLine) {
				return;
			}
			lastAutoScrollCursorLine = cursorLine;

			if(totalLines <= 1) {
				scrollPanel.scrollVerticalPercent(0.0F);
				return;
			}

			float cursorPercent = Math.min(1.0F, Math.max(0.0F, cursorLine / (float) (totalLines - 1)));
			scrollPanel.scrollVerticalPercent(cursorPercent);
		}

		private static String stripAnsi(String text) {
			if(text == null || text.isEmpty()) {
				return "";
			}
			return text.replaceAll("\\u001B\\[[0-9;]*m", "");
		}

		private void handleHistoryUp() {
			if(computerModule == null || computerModule.getTerminal() == null) return;
			// Save current input before navigating away for the first time
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
			if(command == null) command = "";
			currentInputLine = command;
			userIsTyping = !command.isEmpty();
			// Use setTextWithoutCallback to avoid the guard restoring old content
			consolePane.setTextWithoutCallback(lastModuleContent + command);
			refreshPromptStartPositionFromCurrentText();
			// Move caret to end of the newly set text
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea != null) {
				textArea.setChatCarrier(textArea.getCache().length());
				textArea.setBufferChanged();
				textArea.update();
			}
			scrollPaneToCursor();
		}

		private void handleCaretLeft() {
			if(consolePane == null) return;
			TextAreaInput textArea = consolePane.getTextArea();
			if(textArea == null) return;
			// Only allow moving left if we stay within the editable region
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
			if(hasGraphicsFrame()) {
				return;
			}
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
			if(hasGraphicsFrame()) {
				return;
			}

			if(computerModule != null && computerModule.getTerminal() != null) {
				// Save the input before clearing it
				String inputToExecute = currentInputLine;
				currentInputLine = "";

				// Execute the command
				computerModule.getTerminal().handleInput(inputToExecute);

				// Save empty input since command is executed
				computerModule.setSavedTerminalInput("");

				// Reset typing flag and force UI update to show command output
				userIsTyping = false;

				// Update the display immediately with the new terminal content.
				// Use setTextWithoutCallback so the guard in onInputChangedCallback
				// does not fire during the intermediate area.clear() step inside
				// setText(), which would cause a double-prompt by re-injecting the
				// old content before the new content is appended.
				String newContent = computerModule.getLastTextContent();
				lastModuleContent = newContent;
				consolePane.setTextWithoutCallback(newContent);
				// Manually sync state since the callback won't fire.
				currentInputLine = "";
				userIsTyping = false;
				refreshPromptStartPositionFromCurrentText();
				scrollPaneToCursor();
			}
		}

		private Console.GraphicsFrame getActiveCanvasFrame() {
			if(computerModule == null || computerModule.getConsole() == null) {
				return null;
			}
			Console.GraphicsFrame frame = computerModule.getConsole().getGraphicsFrame();
			if(frame == null || frame.getBackend() != Console.GraphicsFrame.RenderBackend.CANVAS) {
				return null;
			}
			return frame;
		}

		private boolean hasGraphicsFrame() {
			return getActiveCanvasFrame() != null;
		}

		public int[] mapMouseToCanvasCell(int mouseX, int mouseY) {
			Console.GraphicsFrame frame = getActiveCanvasFrame();
			if(frame == null || graphicsFrameOverlay == null) {
				return null;
			}

			float originX = graphicsFrameOverlay.getPos().x;
			float originY = graphicsFrameOverlay.getPos().y;
			int baseCharWidth = Math.max(1, FontLibrary.getMetrics(FontLibrary.FontSize.MEDIUM.getFont()).stringWidth("W"));
			int baseCharHeight = Math.max(1, FontLibrary.FontSize.MEDIUM.getFont().getLineHeight());
			float cellWidthPx = Math.max(1.0F, baseCharWidth * frame.getCellScaleX());
			float cellHeightPx = Math.max(1.0F, baseCharHeight * frame.getCellScaleY());

			float localX = mouseX - originX;
			float localY = mouseY - originY;
			if(localX < 0 || localY < 0) {
				return null;
			}

			int cellX = (int) Math.floor(localX / cellWidthPx) + 1;
			int cellY = (int) Math.floor(localY / cellHeightPx) + 1;
			if(cellX < 1 || cellY < 1 || cellX > frame.getWidth() || cellY > frame.getHeight()) {
				return null;
			}

			return new int[]{cellX, cellY};
		}

		private void hideGraphicsOverlay() {
			if(graphicsFrameOverlay == null) {
				return;
			}
			graphicsFrameOverlay.setTextSimple("");
			graphicsFrameOverlay.setScale(new Vector3f(1.0F, 1.0F, 1.0F));
			lastGraphicsFrameRevision = -1L;
		}

		private boolean updateGraphicsFrameOverlay() {
			if(isFileEditMode() || graphicsFrameOverlay == null || computerModule == null || computerModule.getConsole() == null) {
				hideGraphicsOverlay();
				return false;
			}

			Console.GraphicsFrame frame = getActiveCanvasFrame();
			if(frame == null) {
				hideGraphicsOverlay();
				return false;
			}

			long revision = computerModule.getConsole().getGraphicsFrameRevision();
			if(revision != lastGraphicsFrameRevision) {
				String frameText = frame.isAnsiEnabled() ? stripAnsi(frame.getText()) : frame.getText();
				graphicsFrameOverlay.setTextSimple(frameText == null ? "" : frameText);
				lastGraphicsFrameRevision = revision;
			}

			if(consolePane != null) {
				graphicsFrameOverlay.setPos(consolePane.getPos().x + CANVAS_PADDING_X, consolePane.getPos().y + CANVAS_PADDING_Y, 0.0F);
			}
			graphicsFrameOverlay.setScale(new Vector3f(frame.getCellScaleX(), frame.getCellScaleY(), 1.0F));
			return true;
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
					// This is called when Enter is pressed
					// Send the current input line to the terminal
					executeCurrentInput();
				}

				@Override
				public void newLine() {
					// Called when a new line is created
					executeCurrentInput();
				}
			}, input -> {
				if(isFileEditMode()) {
					userIsTyping = true;
					return input;
				}

				// Guard: prevent deletion of protected console output / prompt
				if(input != null && lastModuleContent != null && !lastModuleContent.isEmpty()) {
					if(!input.startsWith(lastModuleContent)) {
						// User deleted into protected territory – restore it
						return lastModuleContent + currentInputLine;
					}
				}

				// The input parameter contains the new text content
				// We need to extract just the current line being typed
				if(input != null) {
					String[] lines = input.split("\n");
					if(lines.length > 0) {
						String lastLine = lines[lines.length - 1];
						// Extract input after the final prompt marker (supports name:path $ prompt)
						int promptIndex = lastLine.lastIndexOf(PROMPT_MARKER);
						if(promptIndex != -1) {
							// Calculate the absolute position of the prompt in the full text
							int lineStartPos = 0;
							for(int i = 0; i < lines.length - 1; i++) {
								lineStartPos += lines[i].length() + 1; // +1 for newline
							}
							promptStartPosition = lineStartPos + promptIndex + PROMPT_MARKER.length();

							if(lastLine.length() > promptIndex + PROMPT_MARKER.length()) {
								currentInputLine = lastLine.substring(promptIndex + PROMPT_MARKER.length());
								// Mark that user is actively typing only when there's actual input
								userIsTyping = true;
							} else {
								currentInputLine = "";
								// No user input, allow syncing from module
								userIsTyping = false;
							}
						} else {
							currentInputLine = "";
							userIsTyping = false;
						}
					}
				}

				// Clamp caret immediately after input parsing to prevent movement into prompt
				clampCaretToEditableRegion();

				return input;
			}) {
				@Override
				public void draw() {
					updateEditorHintOverlay();
					updateDocsButtonPosition();
					if(updateGraphicsFrameOverlay()) {
						return;
					}
					activateConsoleFocusIfPending();

					ComputerModule.ComputerMode currentMode = computerModule.getLastMode();
					if(renderedMode != currentMode) {
						renderedMode = currentMode;
						// Reset cursor tracker so initial scroll-to-cursor fires on mode entry.
						lastAutoScrollCursorLine = -1;
						String modeContent = computerModule.getLastTextContent();
						setTextWithoutCallback(modeContent == null ? "" : modeContent);
						lastModuleContent = modeContent == null ? "" : modeContent;
						if(isFileEditMode()) {
							userIsTyping = true;
							currentInputLine = "";
							// Shrink text area so bottom lines don't render behind the hint overlay.
							if(mainContentPane != null) {
								mainContentPane.setTextBoxHeightLast(TEXT_BOX_HEIGHT - EDITOR_HINT_RESERVE_PX);
							}
						} else {
							userIsTyping = false;
							refreshPromptStartPositionFromCurrentText();
							requestConsoleFocus();
							// Restore full height in terminal mode (no hint overlay).
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

					// Save current input line to module for persistence (only when it changes)
					if(!currentInputLine.equals(lastSavedInput)) {
						computerModule.setSavedTerminalInput(currentInputLine);
						lastSavedInput = currentInputLine;
					}

					// Only update text from module when user is not typing and module content has changed
					// This prevents user input from being overwritten while typing
					if(!userIsTyping) {
						String moduleContent = computerModule.getLastTextContent();
						if(!Objects.equals(lastModuleContent, moduleContent)) {
							// Use setTextWithoutCallback – setText internally calls area.clear()
							// which fires onInputChanged(""), causing the guard to re-inject the
							// old content before append() adds the new content (double prompt).
							lastModuleContent = moduleContent;
							setTextWithoutCallback(moduleContent);
							currentInputLine = "";
							refreshPromptStartPositionFromCurrentText();
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
					return true;
				}

				@Override
				public void onEnter() {
					// Also handle Enter key press here
					executeCurrentInput();
				}
			};
			consolePane.onInit();
			contentPane.getContent(0).attach(consolePane);

			editorHintsOverlay = new GUITextOverlay(830, 16, FontLibrary.FontSize.SMALL, getState());
			editorHintsOverlay.setTextSimple(EDITOR_HINT_PREFIX);
			editorHintsOverlay.onInit();
			editorHintsOverlay.setColor(0.8F, 0.8F, 0.8F, 1.0F);
			editorHintsOverlay.setTextSimple("");
			((GUIDialogWindow) background).attachSuper(editorHintsOverlay);

			graphicsFrameOverlay = new GUITextOverlay(830, TEXT_BOX_HEIGHT, FontLibrary.FontSize.MEDIUM, getState());
			graphicsFrameOverlay.onInit();
			graphicsFrameOverlay.setColor(1.0F, 1.0F, 1.0F, 1.0F);
			graphicsFrameOverlay.setTextSimple("");
			((GUIDialogWindow) background).attachSuper(graphicsFrameOverlay);

			docsButton = new GUITextButton(getState(), 90, 20, GUITextButton.ColorPalette.OK, "DOCS", getCallback());
			docsButton.setUserPointer("DOCS");
			docsButton.setMouseUpdateEnabled(true);
			docsButton.onInit();
			updateDocsButtonPosition();
			((GUIDialogWindow) background).attachSuper(docsButton);

			consolePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			consolePane.getTextArea().setLinewrap(LINE_WRAP);
			String initialContent = computerModule.getLastTextContent();

			if(isFileEditMode()) {
				currentInputLine = "";
				userIsTyping = true;
			} else {
				// Restore saved terminal input if available
				String savedInput = computerModule.getSavedTerminalInput();
				if(savedInput != null && !savedInput.isEmpty()) {
					initialContent = initialContent + savedInput;
					currentInputLine = savedInput;
					userIsTyping = true;
				}
			}

			consolePane.setTextWithoutCallback(initialContent);
			lastModuleContent = computerModule.getLastTextContent();
			renderedMode = computerModule.getLastMode();
			refreshPromptStartPositionFromCurrentText();
			scrollPaneToCursor();
			updateEditorHintOverlay();
			requestConsoleFocus();
		}


		@Override
		public void draw() {
			super.draw();
			if(!hasGraphicsFrame()) {
				clampCaretToEditableRegion();
				scrollPaneToCursor();
			}
			updateDocsButtonPosition();
		}
	}
}
