package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialog;
import api.utils.gui.GUIInputDialogPanel;
import api.utils.gui.SimplePlayerTextInput;
import luamade.manager.ConfigManager;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIScrollablePanel;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ComputerDialog extends GUIInputDialog {

	@Override
	public ComputerPanel createPanel() {
		return new ComputerPanel(getState(), this);
	}

	@Override
	public ComputerPanel getInputPanel() {
		return (ComputerPanel) super.getInputPanel();
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
	public void onDeactivate() {
		super.onDeactivate();
		GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().suspend(false);
	}

	public static class ComputerPanel extends GUIInputDialogPanel {

		private ComputerModule computerModule;
		private SegmentPiece segmentPiece;
		private String script;
		private GUIActivatableTextBar textBar;

		public ComputerPanel(InputState inputState, GUICallback guiCallback) {
			super(inputState, "COMPUTER_PANEL", "", "", 1000, 700, guiCallback);
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(670);

			GUIScrollablePanel scrollablePanel = new GUIScrollablePanel(getWidth(), 670, contentPane.getContent(0), getState());
			textBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.SMALL, ConfigManager.getMainConfig().getConfigurableInt("script-character-limit", 30000), ConfigManager.getMainConfig().getConfigurableInt("script-line-limit", 1000), "", contentPane.getContent(0), new TextCallback() {
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
				public void onTextEnter(String s, boolean b, boolean b1) {

				}

				@Override
				public void newLine() {

				}
			}, new OnInputChangedCallback() {
				@Override
				public String onInputChanged(String s) {
					return s;
				}
			});
			scrollablePanel.setContent(textBar);
			textBar.getTextArea().getChatLog().clear();
			textBar.setText("");
			scrollablePanel.onInit();
			contentPane.getContent(0).attach(scrollablePanel);
			scrollablePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);
			contentPane.addNewTextBox(30);
			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 4, 1, contentPane.getContent(1));
			buttonPane.onInit();

			buttonPane.addButton(0, 0, "CLEAR", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						script = "";
						textBar.setText(script);
					}
				}

				@Override
				public boolean isOccluded() {
					return script == null || script.isEmpty();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return script != null && !script.isEmpty();
				}
			});

			buttonPane.addButton(1, 0, "SAVE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						script = textBar.getText();
						computerModule.setScript(segmentPiece, script);
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});

			buttonPane.addButton(2, 0, "RUN", GUIHorizontalArea.HButtonColor.BLUE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						script = textBar.getText();
						computerModule.setScript(segmentPiece, script);
						computerModule.runScript(segmentPiece);
					}
				}

				@Override
				public boolean isOccluded() {
					return script == null || script.isEmpty();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return script != null && !script.isEmpty();
				}
			});

			buttonPane.addButton(3, 0, "GET FROM WEB", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						new SimplePlayerTextInput("ENTER URL", "Enter the URL of the script you want to download") {
							@Override
							public boolean onInput(String s) {
								if(s == null || s.isEmpty()) return false;
								else {
									script = computerModule.getScriptFromWeb(segmentPiece, s);
									textBar.setText(script);
									deactivate();
									return true;
								}
							}
						};
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});
			contentPane.getContent(1).attach(buttonPane);
			contentPane.setTextBoxHeightLast(30);
			if(computerModule != null && script != null) textBar.setText(script);
		}

		public void setValues(SegmentPiece segmentPiece, String script, ComputerModule computerModule) {
			this.segmentPiece = segmentPiece;
			this.script = script;
			this.computerModule = computerModule;
		}
	}
}
