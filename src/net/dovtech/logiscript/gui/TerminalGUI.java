package net.dovtech.logiscript.gui;

import api.DebugFile;
import api.main.GameClient;
import net.dovtech.logiscript.Logiscript;
import net.dovtech.logiscript.interp.Assembler;
import org.schema.game.client.view.gui.GUITextAreaInputPanel;
import org.schema.game.client.view.gui.GUITextInputBar;
import org.schema.schine.common.TextAreaInput;
import org.schema.schine.common.TextCallback;
import org.schema.schine.graphicsengine.core.Drawable;
import org.schema.schine.graphicsengine.core.GlUtil;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActiveInterface;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIPlainWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class TerminalGUI extends GUIPlainWindow implements GUIWindowInterface, GUIActiveInterface, Drawable {

    private File scriptsFolder = new File("/scripts/");
    private GUIContentPane window;
    private GUITextAreaInputPanel inputBox;
    private GUIElementList buttons;
    private GUITextButton inputsButton;
    private GUITextButton loadButton;
    private GUITextButton saveButton;
    private GUITextButton runButton;
    private GUIEnterableList inputsList;

    public TerminalGUI(InputState inputState, int i, int i1, String s) {
        super(inputState, i, i1, s);
        onInit();
        createGUIWindow(inputState);
        DebugFile.log("Drew TerminalGUI", Logiscript.getInstance());
    }

    @Override
    public void draw() {
        GlUtil.glPushMatrix();

        window.draw();
        window.drawAttached();
        inputBox.draw();
        buttons.draw();
        buttons.drawAttached();
        super.draw();

        GlUtil.glPopMatrix();
    }

    private void createGUIWindow(final InputState inputState) {
        //GUIWindow
        window = new GUIContentPane(inputState, this, "TERMINAL");
        window.addNewTextBox(0, 0);
        window.addNewTextBox(0, 450);

        //InputBox
        GUICallback inputBoxCallback = new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {

            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        };

        TextAreaInput inputBoxInput = new TextAreaInput(500, 500, new TextCallback() {
            @Override
            public String[] getCommandPrefixes() {
                return new String[0];
            }

            @Override
            public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
                return null;
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
        });

        inputBox = new GUITextAreaInputPanel("TERMINAL", GameClient.getClientState(), 500, 500, inputBoxCallback, 500, 500, inputBoxInput, FontLibrary.FontSize.SMALL, true);
        inputBox.onInit();
        //InputsButton
        inputsButton = new GUITextButton(inputState, 30, 10, "INPUT SETTINGS", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                //if(mouseEvent.pressedLeftMouse()) inputsList.draw();
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });

        //Loadbutton
        loadButton = new GUITextButton(inputState, 30, 10, "LOAD SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {

                    GUIElementList scriptsListElement = new GUIElementList(inputState);
                    scriptsListElement.onInit();

                    for(int i = 0; i < scriptsFolder.listFiles().length; i ++) {
                        if(scriptsFolder.listFiles()[i].getName().endsWith(".lscript")) {
                            GUIListElement scriptElement = new GUIListElement(inputState);
                            scriptElement.onInit();
                            GUITextOverlay textOverlay = new GUITextOverlay((int) scriptElement.getWidth(), (int) scriptElement.getHeight(), inputState);
                            textOverlay.onInit();
                            String fileName = scriptsFolder.listFiles()[i].getName().substring(0, scriptsFolder.listFiles()[i].getName().indexOf(".") - 1);
                            textOverlay.setTextSimple(fileName);
                            scriptElement.setContent(textOverlay);
                            final int finalI = i;
                            scriptElement.setCallback(new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                                    if(mouseEvent.pressedLeftMouse()) {
                                        try {
                                            Scanner scan = new Scanner(scriptsFolder.listFiles()[finalI]);
                                            ArrayList<String> list = new ArrayList<String>();
                                            while (scan.hasNext()) {
                                                list.add(scan.nextLine());
                                            }
                                            setText((String[]) list.toArray());
                                            guiElement.cleanUp();
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return false;
                                }
                            });
                            scriptsListElement.add(scriptElement);
                        }
                    }

                    scriptsListElement.draw();
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });

        //SaveButton
        saveButton = new GUITextButton(inputState, 30, 10, "SAVE SCRIPT", new GUICallback() {
            @Override
            public void callback(final GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    final GUITextInputBar guiTextInputBar = new GUITextInputBar(inputState, new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            saveScript(((GUITextInputBar) guiElement).getText());
                            guiElement.cleanUp();
                        }

                        @Override
                        public boolean isOccluded() {
                            return !guiElement.isActive();
                        }
                    }, 30);
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });

        //RunButton
        runButton = new GUITextButton(inputState, 30, 10, "RUN SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    //runScript(getText());
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        });

        //Buttons
        buttons = new GUIElementList(inputState);
        GUIListElement inputsButtonElement = new GUIListElement(inputsButton, inputState);
        GUIListElement loadButtonElement = new GUIListElement(loadButton, inputState);
        GUIListElement saveButtonElement = new GUIListElement(saveButton, inputState);
        GUIListElement runButtonElement = new GUIListElement(runButton, inputState);
        buttons.add(inputsButtonElement);
        buttons.add(loadButtonElement);
        buttons.add(saveButtonElement);
        buttons.add(runButtonElement);
        window.getContent(0).attach(inputBox);
        window.getContent(1).attach(buttons);
        attach(window);
        DebugFile.log("Created TerminalGUIWindow", Logiscript.getInstance());
    }

    private void saveScript(String scriptName) {
        if(scriptName.equals("")) scriptName = "Unnamed Script";
        try {
            PrintWriter printWriter = new PrintWriter(scriptsFolder + scriptName + ".lscript");
            for(String s : getText()) {
                printWriter.println(s);
            }
            printWriter.close();
            DebugFile.log("Successfully saved script " + scriptName + " to local", Logiscript.getInstance());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            DebugFile.log("[ERROR]: Something went wrong trying to save script " + scriptName + " to local", Logiscript.getInstance());
        }
    }

    private void runScript(String[] script) {
        StringBuilder scriptString = new StringBuilder();
        for(String s : script) {
            scriptString.append(s).append("\n");
        }
        byte[] assembledData = Assembler.assemble(scriptString.toString());
    }

    public String[] getText() {
        return inputBox.getGuiTextInput().getTextInput().toString().split("\n");
    }

    public void setText(String[] textInput) {
        inputBox.getGuiTextInput().getTextInput().setClipboardContents(Arrays.toString(getText()));
        inputBox.getGuiTextInput().getTextInput().paste();
    }
}
