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
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

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

	@Override
	public ComputerPanel getInputPanel() {
		return computerPanel;
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

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
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private static final String PROMPT_MARKER = " $ ";
		private static final int LINE_WRAP = 100;

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

		public ComputerPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "COMPUTER_PANEL", "", "", 850, 650, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(false);
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
			if(totalLines <= 1) {
				scrollPanel.scrollVerticalPercent(0.0F);
				return;
			}

			float cursorPercent = Math.min(1.0F, Math.max(0.0F, cursorLine / (float) (totalLines - 1)));
			scrollPanel.scrollVerticalPercent(cursorPercent);
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
		 * Executes the current input line as a terminal command
		 */
		private void executeCurrentInput() {
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

		@Override
		public void onInit() {
			super.onInit();
			if(computerModule == null) {
				return;
			}
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(500);

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
					activateConsoleFocusIfPending();

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
			consolePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			consolePane.getTextArea().setLinewrap(LINE_WRAP);
			String initialContent = computerModule.getLastTextContent();

			// Restore saved terminal input if available
			String savedInput = computerModule.getSavedTerminalInput();
			if(savedInput != null && !savedInput.isEmpty()) {
				initialContent = initialContent + savedInput;
				currentInputLine = savedInput;
				userIsTyping = true;
			}

			consolePane.setTextWithoutCallback(initialContent);
			lastModuleContent = computerModule.getLastTextContent();
			refreshPromptStartPositionFromCurrentText();
			scrollPaneToCursor();
			requestConsoleFocus();
		}


		@Override
		public void draw() {
			super.draw();
			clampCaretToEditableRegion();
			scrollPaneToCursor();
		}
	}
}
