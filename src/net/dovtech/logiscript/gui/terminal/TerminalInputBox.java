package net.dovtech.logiscript.gui.terminal;

import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.common.InputHandler;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;

public class TerminalInputBox extends PlayerTextAreaInput implements InputHandler, TextCallback {

    private TextAreaInput textAreaInput;
    private TerminalInputPanel inputPanel;

    public TerminalInputBox(GameClientState gameClientState, String currentText) {
        super("EDIT_DISPLAY_BLOCK_POPUP", gameClientState, 750,500, 300, 12, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_PLAYERINTERACTIONCONTROLMANAGER_28, currentText, FontLibrary.FontSize.SMALL);
        textAreaInput = new TextAreaInput(700, 450, this);
        inputPanel = new TerminalInputPanel(gameClientState, this, textAreaInput, currentText);
    }

    public void onDeactivate() {

    }

    public boolean onInput(String s) {
        return false;
    }

    public String[] getCommandPrefixes() {
        return new String[0];
    }

    public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
        return null;
    }

    public void onFailedTextCheck(String s) {

    }
}
