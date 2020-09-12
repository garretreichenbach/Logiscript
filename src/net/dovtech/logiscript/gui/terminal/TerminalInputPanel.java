package net.dovtech.logiscript.gui.terminal;

import org.schema.game.client.view.gui.GUIInputInterface;
import org.schema.game.client.view.gui.GUITextAreaInputPanel;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.network.client.ClientState;

public class TerminalInputPanel extends GUITextAreaInputPanel implements GUIInputInterface {


    public TerminalInputPanel(ClientState clientState, GUICallback guiCallback, TextAreaInput textAreaInput, String currentText) {
        super("TERMINAL_INPUT_PANEL", clientState, 750, 500, guiCallback, "", currentText, textAreaInput, FontLibrary.FontSize.SMALL, true);
    }

    public float getWidth() {
        return 0;
    }

    public float getHeight() {
        return 0;
    }

    public void cleanUp() {

    }

    public void draw() {

    }

    public void onInit() {

    }
}
