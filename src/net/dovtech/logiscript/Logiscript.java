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
import net.dovtech.logiscript.gui.terminal.TerminalGUI;
import org.schema.game.client.controller.PlayerTextAreaInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.input.InputState;
import java.io.File;

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
                    final InputState inputState = GameClientState.instance.getWorldDrawer().getGuiDrawer().getPlayerPanel().getInventoryPanel().inventoryPanel.getState();
                    TerminalGUI terminalGUI = new TerminalGUI(inputState);
                    terminalGUI.activate();

                    /*
                    (textInput = new PlayerTextAreaInput("EDIT_DISPLAY_BLOCK_POPUP", GameClientState.instance, 500, 450, 300, 12, Lng.ORG_SCHEMA_GAME_CLIENT_CONTROLLER_MANAGER_INGAME_PLAYERINTERACTIONCONTROLMANAGER_28, "", , FontLibrary.FontSize.SMALL) {
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
                     */
                }
            }
        });
    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        Terminal terminal = new Terminal();
        ElementInformation terminalInfo = Terminal.blockInfo;
        FactoryResource[] terminalRecipe = {
            new FactoryResource(4, Blocks.DISPLAY_MODULE.getId()),
            new FactoryResource(5, Blocks.ACTIVATION_MODULE.getId()),
            new FactoryResource(5, Blocks.NOT_SIGNAL.getId()),
            new FactoryResource(3, Blocks.SENSOR.getId()),
            new FactoryResource(1, Blocks.BOBBY_AI_MODULE.getId()),
        };
        BlockConfig.addRecipe(terminalInfo, FactoryType.ADVANCED, 10, terminalRecipe);
        config.add(terminalInfo);
    }

    public static Logiscript getInstance() {
        return inst;
    }
}