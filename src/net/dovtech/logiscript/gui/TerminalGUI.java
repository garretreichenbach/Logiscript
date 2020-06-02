package net.dovtech.logiscript.gui;

import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.GUIEnterableList;
import org.schema.schine.graphicsengine.forms.gui.GUITextButton;
import org.schema.schine.graphicsengine.forms.gui.GUITextInput;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TerminalGUI extends GUIMainWindow implements GUIWindowInterface {

    private GUIContentPane window;
    private GUITextInput inputBox;
    private GUIElementList buttons;
    private GUITextButton exitButton;
    private GUITextButton loadButton;
    private GUITextButton saveButton;
    private GUIEnterableList inputsList;

    public TerminalGUI(InputState inputState, int i, int i1, String s) {
        super(inputState, i, i1, s);
        createGUIWindow(inputState);
    }

    private void createGUIWindow(InputState inputState) {
        window = new GUIContentPane(inputState, this, "TERMINAL");
        window.addNewTextBox(280);

        //InputBox
        inputBox = new GUITextInput((int) (window.getWidth()) -5, (int) (window.getHeight()) - 5, inputState);
        window.getContent(0).attach(inputBox);

        //Buttons
        buttons = new GUIElementList(inputState);

        window.getContent(0, 1).attach(buttons);
    }

    public String[] getText() {
        return (String[]) inputBox.getInputBox().getText().toArray();
    }

    public void setText(String[] textInput) {
        inputBox.getTextInput().clear();
        inputBox.getInputBox().setText(new ArrayList<Object>(Arrays.asList(textInput)));
    }
}
