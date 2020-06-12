package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.element.block.FactoryType;
import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.block.ClientActivateSegmentPieceEvent;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.logiscript.blocks.Terminal;
import net.dovtech.logiscript.interp.Assembler;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.controller.element.world.ClientSegmentProvider;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUITextInputBar;
import org.schema.game.common.controller.SendableSegmentProvider;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.network.objects.remote.RemoteTextBlockPair;
import org.schema.game.network.objects.remote.TextBlockPair;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Logiscript extends StarMod {
    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }
    private File scriptsFolder;
    private File scriptsLocation;
    private PlayerTextAreaInput textInput;
    private GUITextButton inputsButton;
    private GUITextButton loadButton;
    private GUITextButton saveButton;
    private GUITextButton runButton;
    private GUIEnterableList inputsList;

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    @Override
    public void onGameStart() {
        this.modName = "Logiscript";
        this.modAuthor = "DovTech";
        this.modVersion = "0.2.11";
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";
        scriptsFolder = new File("scripts");
        if(!scriptsFolder.exists()) scriptsFolder.mkdir();
        scriptsLocation = new File("/scripts/");
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);

        //Terminal Activate Event
        StarLoader.registerListener(ClientActivateSegmentPieceEvent.class, new Listener() {
            @Override
            public void onEvent(Event e) {
                final ClientActivateSegmentPieceEvent event = (ClientActivateSegmentPieceEvent) e;
                if(event.getPiece().getInfo().getId() == Terminal.blockInfo.getId()) {
                    final InputState inputState = GameClientState.instance.getPlayerInputs().get(0).getInputPanel().getState();
                    (textInput = new PlayerTextAreaInput("EDIT_DISPLAY_BLOCK_POPUP", GameClientState.instance, 500, 450, 300, 12, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_PLAYERINTERACTIONCONTROLMANAGER_28, "", "TERMINAL", FontLibrary.FontSize.SMALL) {
                        public void onDeactivate() {
                            event.getPicm().suspend(false);
                        }

                        public String[] getCommandPrefixes() {
                            return null;
                        }

                        public boolean onInput(String input) {
                            SendableSegmentProvider segmentProvider = ((ClientSegmentProvider)event.getPiece().getSegment().getSegmentController().getSegmentProvider()).getSendableSegmentProvider();
                            TextBlockPair textBlock;
                            (textBlock = new TextBlockPair()).block = ElementCollection.getIndex4(event.getPiece().getAbsoluteIndex(), (short)event.getPiece().getOrientation());
                            textBlock.text = input;
                            System.err.println("[CLIENT]Text entry:\n\"" + textBlock.text + "\"");
                            segmentProvider.getNetworkObject().textBlockResponsesAndChangeRequests.add(new RemoteTextBlockPair(textBlock, false));
                            return true;
                        }

                        public String handleAutoComplete(String input, TextCallback textCallback, String var3) throws PrefixNotFoundException {
                            return null;
                        }

                        public boolean isOccluded() {
                            return false;
                        }

                        public void onFailedTextCheck(String var1x) {
                        }
                    }).getTextInput().setAllowEmptyEntry(true);
                    createGUIWindow(inputState);
                    textInput.activate();
                }
            }
        });
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        Terminal terminal = new Terminal();
        ElementInformation terminalInfo = Terminal.blockInfo;
        FactoryResource[] terminalRecipe = {
            new FactoryResource(1, Blocks.DISPLAY_MODULE.getId()),
            new FactoryResource(5, Blocks.ACTIVATION_MODULE.getId()),
            new FactoryResource(2, Blocks.SENSOR.getId()),
            new FactoryResource(1, Blocks.BOBBY_AI_MODULE.getId()),
        };
        BlockConfig.addRecipe(terminalInfo, FactoryType.ADVANCED, 10, terminalRecipe);
        config.add(terminalInfo);
    }

    public static Logiscript getInstance() {
        return inst;
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
        return textInput.getTextInput().toString().split("\n");
    }

    public void setText(String[] text) {
        textInput.getTextInput().setClipboardContents(Arrays.toString(text));
        textInput.getTextInput().paste();
    }
}