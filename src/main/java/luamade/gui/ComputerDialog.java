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
		GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().suspend(false);
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private ComputerModule computerModule;
		private GUIScrollablePanel consolePanel;
		private GUIActivatableTextBar consolePane;
		private String currentInputLine = "";

		public ComputerPanel(InputState inputState, GUICallback guiCallback, ComputerModule computerModule) {
			super(inputState, "COMPUTER_PANEL", "", "", 850, 650, guiCallback);
			this.computerModule = computerModule;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			if(computerModule == null) return;
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
					if(computerModule != null && computerModule.getTerminal() != null) {
						computerModule.getTerminal().handleInput(currentInputLine);
						currentInputLine = "";
					}
				}

				@Override
				public void newLine() {
					// Called when a new line is created
					if(computerModule != null && computerModule.getTerminal() != null) {
						computerModule.getTerminal().handleInput(currentInputLine);
						currentInputLine = "";
					}
				}
			}, new OnInputChangedCallback() {
				@Override
				public String onInputChanged(String input) {
					// Track what the user is typing
					// Extract the current line being edited
					String currentText = consolePane.getText();
					String[] lines = currentText.split("\n");
					if(lines.length > 0) {
						String lastLine = lines[lines.length - 1];
						// Extract input after prompt (format: "path $ ")
						int promptIndex = lastLine.indexOf(" $ ");
						if(promptIndex != -1) {
							currentInputLine = lastLine.substring(promptIndex + 3);
						} else {
							currentInputLine = lastLine;
						}
					}
					return input;
				}
			}) {
				public String getLastTextContent() {
					return consolePane.getText();
				}

				public void setTextContent(String text) {
					consolePane.setText(text);
				}

				@Override
				public void draw() {
					if(computerModule != null && !Objects.equals(computerModule.getLastTextContent(), getLastTextContent())) {
						setTextContent(computerModule.getLastTextContent());
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
					if(computerModule != null && computerModule.getTerminal() != null) {
						computerModule.getTerminal().handleInput(currentInputLine);
						currentInputLine = "";
					}
				}
			};
			consolePane.onInit();
			contentPane.getContent(0).attach(consolePane);
			consolePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL | GUIScrollablePanel.SCROLLABLE_HORIZONTAL);
		}
	}
}
