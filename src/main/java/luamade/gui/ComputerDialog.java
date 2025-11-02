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
		
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private static final String PROMPT_MARKER = " $ ";

		private ComputerModule computerModule;
		private GUIScrollablePanel consolePanel;
		private GUIActivatableTextBar consolePane;
		private String currentInputLine = "";
		private String lastModuleContent = "";
		private boolean userIsTyping;

		public ComputerPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "COMPUTER_PANEL", "", "", 850, 650, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(false);
		}

		/**
		 * Executes the current input line as a terminal command
		 */
		private void executeCurrentInput() {
			if(computerModule != null && computerModule.getTerminal() != null) {
				computerModule.getTerminal().handleInput(currentInputLine);
				currentInputLine = "";
				// Reset typing flag to allow module content to sync back to the text field
				userIsTyping = false;
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
					// For now, let's assume the user types at the end
					if(input != null) {
						String[] lines = input.split("\n");
						if(lines.length > 0) {
							String lastLine = lines[lines.length - 1];
							// Extract input after prompt (format: "path $ ")
							int promptIndex = lastLine.indexOf(PROMPT_MARKER);
							if(promptIndex != -1 && lastLine.length() > promptIndex + PROMPT_MARKER.length()) {
								currentInputLine = lastLine.substring(promptIndex + PROMPT_MARKER.length());
								// Mark that user is actively typing only when there's actual input
								userIsTyping = true;
							} else {
								currentInputLine = "";
								// No user input, allow syncing from module
								userIsTyping = false;
							}
						}
					}
					return input;
				}
			}) {
				@Override
				public void draw() {
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
			consolePane.setText(initialContent);
			lastModuleContent = initialContent;
		}
	}
}
