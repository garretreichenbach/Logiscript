package luamade.gui;

import api.common.GameClient;
import api.network.packets.PacketUtil;
import api.utils.gui.GUIInputDialog;
import api.utils.gui.GUIInputDialogPanel;
import api.utils.gui.SimplePlayerTextInput;
import luamade.manager.ConfigManager;
import luamade.network.client.RunScriptPacket;
import luamade.network.client.SaveScriptPacket;
import luamade.network.client.SetAutoRunPacket;
import luamade.network.client.TerminateScriptPacket;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.common.OnInputChangedCallback;
import org.schema.schine.common.TabCallback;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
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

		private SegmentPiece segmentPiece;
		private ComputerModule computerModule;
		private ComputerModule.ComputerData computerData;
		private GUIActivatableTextBar codeBar;
		private GUIActivatableTextBar outputBar;
		private String output = "";

		public ComputerPanel(InputState inputState, GUICallback guiCallback) {
			super(inputState, "COMPUTER_PANEL", "", "", 1000, 630, guiCallback);
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			if(computerModule == null) return;
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			contentPane.setTextBoxHeightLast(500);

			GUIScrollablePanel codePanel = new GUIScrollablePanel(getWidth(), 500, contentPane.getContent(0), getState());
			codeBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, ConfigManager.getMainConfig().getConfigurableInt("script-character-limit", 30000), ConfigManager.getMainConfig().getConfigurableInt("script-line-limit", 1000), "", contentPane.getContent(0), new TextCallback() {
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
			codePanel.setContent(codeBar);
			codeBar.getTextArea().getChatLog().clear();
			if(computerData != null && computerData.script != null) codeBar.setText(computerData.script);
			else codeBar.setText("");
			codeBar.getTextArea().onTabCallback = new TabCallback() {
				@Override
				public boolean catchTab(TextAreaInput textAreaInput) {
					return true;
				}

				@Override
				public void onEnter() {

				}
			};
			codePanel.onInit();
			contentPane.getContent(0).attach(codePanel);
			codePanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);

			contentPane.addNewTextBox(100);
			GUIScrollablePanel outputPanel = new GUIScrollablePanel(getWidth(), 100, contentPane.getContent(1), getState());
			outputBar = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10000, 100, "", contentPane.getContent(0), new TextCallback() {
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
					if(computerData != null && computerData.lastOutput != null) return computerData.lastOutput;
					else return "";
				}
			});
			outputPanel.setContent(outputBar);
			outputBar.getTextArea().getChatLog().clear();
			if(computerData != null && computerData.lastOutput != null) outputBar.setText(computerData.lastOutput);
			else outputBar.setText("");
			outputPanel.onInit();
			contentPane.getContent(1).attach(outputPanel);
			outputPanel.setScrollable(GUIScrollablePanel.SCROLLABLE_VERTICAL);

			contentPane.addNewTextBox(30);
			GUIHorizontalButtonTablePane buttonPane = new GUIHorizontalButtonTablePane(getState(), 6, 1, contentPane.getContent(2));
			buttonPane.onInit();

			buttonPane.addButton(0, 0, "CLEAR", GUIHorizontalArea.HButtonColor.RED, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						computerData.script = "";
						codeBar.setText(computerData.script);
					}
				}

				@Override
				public boolean isOccluded() {
					return computerData == null || computerData.script == null || computerData.script.isEmpty();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return computerData != null && computerData.script != null && !computerData.script.isEmpty();
				}
			});

			buttonPane.addButton(1, 0, "SAVE", GUIHorizontalArea.HButtonColor.GREEN, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						computerData.script = codeBar.getText();
						computerModule.setData(segmentPiece, computerData);
						PacketUtil.sendPacketToServer(new SaveScriptPacket(segmentPiece.getSegmentController(), segmentPiece.getAbsoluteIndex(), computerData.script));
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
					if(mouseEvent.pressedLeftMouse() && segmentPiece != null) {
						computerData.script = codeBar.getText();
						computerModule.setData(segmentPiece, computerData);
						PacketUtil.sendPacketToServer(new RunScriptPacket(segmentPiece.getSegmentController(), segmentPiece.getAbsoluteIndex(), computerData.script));
					}
				}

				@Override
				public boolean isOccluded() {
					return computerData == null || computerData.script == null || computerData.script.isEmpty();
				}
			}, new GUIActivationCallback() {
				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return computerData != null && computerData.script != null && !computerData.script.isEmpty();
				}
			});

			buttonPane.addButton(3, 0, "TERMINATE", GUIHorizontalArea.HButtonColor.ORANGE, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse() && segmentPiece != null) {
						PacketUtil.sendPacketToServer(new TerminateScriptPacket(segmentPiece.getSegmentController(), segmentPiece.getAbsoluteIndex()));
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

			buttonPane.addButton(4, 0, "GET FROM WEB", GUIHorizontalArea.HButtonColor.YELLOW, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						new SimplePlayerTextInput("ENTER URL", "Enter the URL of the script you want to download") {
							@Override
							public boolean onInput(String s) {
								if(s == null || s.isEmpty()) return false;
								else {
									computerData.script = computerModule.getScriptFromWeb(segmentPiece, s);
									codeBar.setText(computerData.script);
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

			buttonPane.addButton(5, 0, "AUTO-RUN", GUIHorizontalArea.HButtonColor.PINK, new GUICallback() {
				@Override
				public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
					if(mouseEvent.pressedLeftMouse()) {
						computerData.autoRun = !computerData.autoRun;
						PacketUtil.sendPacketToServer(new SetAutoRunPacket(segmentPiece.getSegmentController(), segmentPiece.getAbsoluteIndex(), computerData.autoRun));
					}
				}

				@Override
				public boolean isOccluded() {
					return false;
				}
			}, new GUIActivationHighlightCallback() {
				@Override
				public boolean isHighlighted(InputState inputState) {
					return computerData != null && computerData.autoRun;
				}

				@Override
				public boolean isVisible(InputState inputState) {
					return true;
				}

				@Override
				public boolean isActive(InputState inputState) {
					return true;
				}
			});

			contentPane.getContent(2).attach(buttonPane);
		}

		public void setValues(SegmentPiece segmentPiece, ComputerModule computerModule, ComputerModule.ComputerData computerData) {
			this.segmentPiece = segmentPiece;
			this.computerModule = computerModule;
			this.computerData = computerData;
		}
	}
}
