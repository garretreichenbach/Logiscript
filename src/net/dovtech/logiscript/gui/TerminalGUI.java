package net.dovtech.logiscript.gui;

import api.DebugFile;
import net.dovtech.logiscript.Logiscript;
import net.dovtech.logiscript.interp.Assembler;
import org.schema.game.client.view.gui.GUITextInputBar;
import org.schema.game.client.view.mainmenu.FileChooserDialog;
import org.schema.game.client.view.mainmenu.gui.FileChooserStats;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIPlainWindow;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIWindowInterface;
import org.schema.schine.input.InputState;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class TerminalGUI extends GUIPlainWindow implements GUIWindowInterface {

    private File scriptsFolder = new File("/scripts/");
    private SegmentPiece terminalBlock;
    private GUIContentPane window;
    private GUITextInput inputBox;
    private GUIElementList buttons;
    private GUITextButton exitButton;
    private GUITextButton inputsButton;
    private GUITextButton loadButton;
    private GUITextButton saveButton;
    private GUITextButton runButton;
    private GUIEnterableList inputsList;


    public TerminalGUI(SegmentPiece terminalBlock, InputState inputState, int i, int i1, String s) {
        super(inputState, i, i1, s);
        this.terminalBlock = terminalBlock;
        createGUIWindow(inputState);
        draw();
    }

    @Override
    public void draw() {
        super.draw();
        DebugFile.log("[DEBUG]: Drew Terminal GUI", Logiscript.getInstance());
    }

    private void createGUIWindow(final InputState inputState) {
        window = new GUIContentPane(inputState, this, "TERMINAL");
        window.addNewTextBox(0, 0);
        window.addNewTextBox(0, 280);

        //InputBox
        inputBox = new GUITextInput((int) (window.getWidth()) -5, (int) (window.getHeight()) - 5, inputState);
        window.getContent(0).attach(inputBox);

        //ExitButton
        exitButton = new GUITextButton(inputState, 30, 10, "EXIT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) cleanUp();
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });

        //InputsButton
        inputsButton = new GUITextButton(inputState, 30, 10, "INPUT SETTINGS", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                //if(mouseEvent.pressedLeftMouse()) inputsList.draw();
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });

        //Loadbutton
        loadButton = new GUITextButton(inputState, 30, 10, "LOAD SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    FileFilter fileFilter = new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            return f.getName().endsWith(".lscript");
                        }

                        @Override
                        public String getDescription() {
                            return null;
                        }
                    };
                    FileChooserDialog fileChooser = new FileChooserDialog(inputState, new FileChooserStats(inputState, scriptsFolder, "", fileFilter) {
                        @Override
                        public void onSelectedFile(File file, String s) {
                            if (file.getName().endsWith(".lscript")) {
                                try {
                                    Scanner scan = new Scanner(file);
                                    ArrayList<String> list = new ArrayList<String>();
                                    while (scan.hasNext()) {
                                        list.add(scan.nextLine());
                                    }
                                    setText((String[]) list.toArray());
                                } catch (FileNotFoundException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });

        //SaveButton
        saveButton = new GUITextButton(inputState, 30, 10, "SAVE SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    GUITextInputBar guiTextInputBar = new GUITextInputBar(inputState, new GUICallback() {
                        @Override
                        public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                            saveScript(((GUITextInputBar) guiElement).getText());
                            guiElement.cleanUp();
                        }

                        @Override
                        public boolean isOccluded() {
                            return !isActive();
                        }
                    }, 30);
                }
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });

        //RunButton
        runButton = new GUITextButton(inputState, 30, 10, "RUN SCRIPT", new GUICallback() {
            @Override
            public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                if(mouseEvent.pressedLeftMouse()) {
                    runScript(getText());
                }
            }

            @Override
            public boolean isOccluded() {
                return !isActive();
            }
        });

        //Buttons
        buttons = new GUIElementList(inputState);
        GUIListElement exitButtonElement = new GUIListElement(exitButton, inputState);
        GUIListElement inputsButtonElement = new GUIListElement(inputsButton, inputState);
        GUIListElement loadButtonElement = new GUIListElement(loadButton, inputState);
        GUIListElement saveButtonElement = new GUIListElement(saveButton, inputState);
        GUIListElement runButtonElement = new GUIListElement(runButton, inputState);
        buttons.add(exitButtonElement);
        buttons.add(inputsButtonElement);
        buttons.add(loadButtonElement);
        buttons.add(saveButtonElement);
        buttons.add(runButtonElement);
        window.getContent(1).attach(buttons);
        this.attach(window);
        this.setLoaded(true);
    }

    private void saveScript(String scriptName) {
        if(scriptName.equals("")) scriptName = "Unnamed Script";
        try {
            PrintWriter printWriter = new PrintWriter(scriptsFolder + scriptName + ".lscript");
            for(String s : getText()) {
                printWriter.println(s);
            }
            printWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void runScript(String[] script) {
        byte[] assembledData = Assembler.assemble(script);
    }

    public String[] getText() {
        return (String[]) inputBox.getInputBox().getText().toArray();
    }

    public void setText(String[] textInput) {
        inputBox.getTextInput().clear();
        inputBox.getInputBox().setText(new ArrayList<Object>(Arrays.asList(textInput)));
    }
}
