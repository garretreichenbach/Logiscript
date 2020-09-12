package net.dovtech.logiscript.gui.terminal;

import api.DebugFile;
import net.dovtech.logiscript.Logiscript;
import net.dovtech.logiscript.interp.Assembler;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUITextInputBar;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class TerminalGUI {

    private File scriptsFolder = new File("/scripts/");

    private TerminalInputBox terminalInputBox;

    public TerminalGUI(InputState inputState) {
        terminalInputBox = new TerminalInputBox(GameClientState.instance);
    }

    public void activate() {


        DebugFile.log("Activated TerminalGUI", Logiscript.getInstance());
    }

    private void createGUIWindow(final InputState inputState) {
        //InputsButton
        inputsButton = new GUITextButton(inputState, 30, 10, "INPUT SETTINGS", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    //inputsList.draw();
                    GameClientState.instance.getController().queueUIAudio("0022_action - buttons push medium");
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }) {
            public void draw() {
                this.setPos(textInput.getInputPanel().getButtonCancel().getPos().x + textInput.getInputPanel().getButtonCancel().getWidth() + 10.0F, textInput.getInputPanel().getButtonCancel().getPos().y, 0.0F);
                super.draw();
            }
        };

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
                                        GameClientState.instance.getController().queueUIAudio("0022_action - buttons push medium");
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
                    GameClientState.instance.getController().queueUIAudio("0022_action - buttons push medium");
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }) {
            public void draw() {
                this.setPos(textInput.getInputPanel().getButtonCancel().getPos().x + textInput.getInputPanel().getButtonCancel().getWidth() + 10.0F, textInput.getInputPanel().getButtonCancel().getPos().y, 0.0F);
                super.draw();
            }
        };

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
                    GameClientState.instance.getController().queueUIAudio("0022_action - buttons push medium");
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }) {
            public void draw() {
                this.setPos(textInput.getInputPanel().getButtonCancel().getPos().x + textInput.getInputPanel().getButtonCancel().getWidth() + 10.0F, textInput.getInputPanel().getButtonCancel().getPos().y, 0.0F);
                super.draw();
            }
        };

        //RunButton
        runButton = new GUITextButton(inputState, 30, 10, "RUN SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    //runScript(getText());
                    GameClientState.instance.getController().queueUIAudio("0022_action - buttons push medium");
                }
            }

            @Override
            public boolean isOccluded() {
                return false;
            }
        }) {
            public void draw() {
                this.setPos(textInput.getInputPanel().getButtonCancel().getPos().x + textInput.getInputPanel().getButtonCancel().getWidth() + 10.0F, textInput.getInputPanel().getButtonCancel().getPos().y, 0.0F);
                super.draw();
            }
        };

        textInput.getInputPanel().onInit();
        saveButton.setPos(300.0F, -40.0F, 0.0F);
        ((GUIDialogWindow)textInput.getInputPanel().background).getMainContentPane().getContent(0).attach(saveButton);

        loadButton.setPos(320.0F, -40.0F, 0.0F);
        ((GUIDialogWindow)textInput.getInputPanel().background).getMainContentPane().getContent(0).attach(loadButton);

        runButton.setPos(340.0F, -40.0F, 0.0F);
        ((GUIDialogWindow)textInput.getInputPanel().background).getMainContentPane().getContent(0).attach(runButton);

        inputsButton.setPos(360.0F, -40.0F, 0.0F);
        ((GUIDialogWindow)textInput.getInputPanel().background).getMainContentPane().getContent(0).attach(inputsButton);

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
