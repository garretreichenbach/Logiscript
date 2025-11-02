package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.schema.game.client.controller.PlayerInput;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TabCallback;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import org.schema.schine.input.KeyEventInterface;

import java.util.Objects;

public class ComputerDialog extends PlayerInput {

	protected final ComputerModule computerModule;
	private final ComputerPanel computerPanel;

	public ComputerDialog(ComputerModule computerModule) {
		super(GameClient.getClientState());
		this.computerModule = computerModule;
		computerPanel = new ComputerPanel(getState(), this, computerModule);
	}

	@Override
	public void activate() {
		super.activate();
		computerModule.resumeFromLastMode();
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
	public void handleKeyEvent(KeyEventInterface keyEvent) {
		super.handleKeyEvent(keyEvent);
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {

	}

	@Override
	public void onDeactivate() {
		// Save current input when closing the dialog
		if(computerPanel != null) {
			computerPanel.saveCurrentInput();
		}
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private static final String PROMPT_MARKER = " $ ";

		private ComputerModule computerModule;
		private GUIScrollablePanel consolePanel;
		private GUIActivatableTextBar consolePane;
		private String currentInputLine = "";
		private String lastModuleContent = "";
		private boolean userIsTyping = false;
		private int promptStartPosition = -1;
		private String lastSavedInput = "";

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
				
				// Update the display immediately with the new terminal content
				String newContent = computerModule.getLastTextContent();
				consolePane.setText(newContent);
				lastModuleContent = newContent;
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
			consolePane = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, ConfigManager.getMainConfig().getConfigurableInt("console-character-limit", 30000), ConfigManager.getMainConfig().getConfigurableInt("console-line-limit", 1000), "", contentPane.getContent(0), new TextCallback() {
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
			}, new OnInputChangedCallback() {
				@Override
				public String onInputChanged(String input) {
					// The input parameter contains the new text content
					// We need to extract just the current line being typed
					if(input != null) {
						String[] lines = input.split("\n");
						if(lines.length > 0) {
							String lastLine = lines[lines.length - 1];
							// Extract input after prompt (format: "path $ ")
							int promptIndex = lastLine.indexOf(PROMPT_MARKER);
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
								// Prompt was deleted or modified - restore the last valid content
								// This prevents users from deleting the prompt
								if(lastModuleContent != null && !lastModuleContent.isEmpty()) {
									// Restore the text to the last valid module content with the saved input
									String restoredText = lastModuleContent;
									if(!currentInputLine.isEmpty()) {
										restoredText = lastModuleContent + currentInputLine;
									}
									// Schedule text restoration on next frame to avoid recursion
									final String textToRestore = restoredText;
									consolePane.getState().getController().queueUIAudio("0022_menu_back");
									new Thread(() -> {
										try {
											Thread.sleep(10);
											consolePane.setText(textToRestore);
										} catch(InterruptedException e) {
											e.printStackTrace();
										}
									}).start();
									return input; // Return current input for now, will be fixed on next frame
								}
								currentInputLine = "";
								userIsTyping = false;
							}
						}
					}
					return input;
				}
			}) {
				@Override
				public void draw() {
					// Save current input line to module for persistence (only when it changes)
					if(computerModule != null && !currentInputLine.equals(lastSavedInput)) {
						computerModule.setSavedTerminalInput(currentInputLine);
						lastSavedInput = currentInputLine;
					}
					
					// Only update text from module when user is not typing and module content has changed
					// This prevents user input from being overwritten while typing
					if(computerModule != null && !userIsTyping) {
						String moduleContent = computerModule.getLastTextContent();
						if(!Objects.equals(lastModuleContent, moduleContent)) {
							setText(moduleContent);
							lastModuleContent = moduleContent;
						}
					}
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
			consolePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL | GUIScrollablePanel.SCROLLABLE_HORIZONTAL);
			String initialContent = computerModule.getLastTextContent();
			
			// Restore saved terminal input if available
			String savedInput = computerModule.getSavedTerminalInput();
			if(savedInput != null && !savedInput.isEmpty()) {
				initialContent = initialContent + savedInput;
				currentInputLine = savedInput;
				userIsTyping = true;
			}
			
			consolePane.setText(initialContent);
			lastModuleContent = computerModule.getLastTextContent();
		}
	}
}
