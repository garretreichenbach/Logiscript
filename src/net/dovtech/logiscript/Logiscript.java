package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.element.block.Blocks;
import api.listener.Listener;
import api.listener.events.block.*;
import api.mod.StarLoader;
import api.mod.StarMod;
import net.dovtech.logiscript.block.MicroController;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import java.io.File;

public class Logiscript extends StarMod {

    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }
    public File scriptsFolder;

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    @Override
    public void onGameStart() {
        setModName("Logiscript");
        setModAuthor("CaptainSkwidz, TheDerpGamer");
        setModVersion("0.3.4");
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";

        scriptsFolder = new File("scripts");
        if(!scriptsFolder.exists()) scriptsFolder.mkdir();
    }

    @Override
    public void onEnable() {
        super.onEnable();

        registerListeners();

        DebugFile.log("Enabled", this);

    }

    @Override
    public void onBlockConfigLoad(BlockConfig config) {
        /* Factory Types:
        0 = NONE
        1 = CAPSULE REFINERY
        2 = MICRO ASSEMBLER
        3 = BASIC FACTORY
        4 = STANDARD FACTORY
        5 = ADVANCED FACTORY
         */

        //Create Blocks
        //Texture ID Places: { FRONT, BACK, TOP, BOTTOM, LEFT, RIGHT }
        MicroController.blockInfo = config.newElement("Micro Controller", new short[] { 0, 0, 0, 0, 0, 0 });

        //Initialize Blocks
        new MicroController();

        //Add Recipes
        FactoryResource[] microControllerRecipe = {
                new FactoryResource(1000, Blocks.ALLOYED_METAL_MESH.getId()),
                new FactoryResource(1000, Blocks.CRYSTAL_COMPOSITE.getId()),
                new FactoryResource(5, Blocks.ACTIVATION_MODULE.getId()),
                new FactoryResource(3, Blocks.BUTTON.getId()),
                new FactoryResource(3, Blocks.NOT_SIGNAL.getId()),
                new FactoryResource(3, Blocks.AND_SIGNAL.getId()),
                new FactoryResource(3, Blocks.OR_SIGNAL.getId())
        };
        BlockConfig.addRecipe(MicroController.blockInfo, 4, 650, microControllerRecipe);

        //Register Blocks
        config.add(MicroController.blockInfo);

        DebugFile.log("[DEBUG]: Registered block");
    }

    private void registerListeners() {
        //Placed Block
        StarLoader.registerListener(SegmentPieceAddEvent.class, new Listener<SegmentPieceAddEvent>() {
            @Override
            public void onEvent(SegmentPieceAddEvent event) {

            }
        });

        //Removed Block
        StarLoader.registerListener(SegmentPieceRemoveEvent.class, new Listener<SegmentPieceRemoveEvent>() {
            @Override
            public void onEvent(SegmentPieceRemoveEvent event) {
                if(event.getType() == MicroController.blockInfo.getId()) removeDataBlock(event.getType(), new int[] {event.getX(), event.getY(), event.getZ()});
            }
        });
        StarLoader.registerListener(SegmentPieceKillEvent.class, new Listener<SegmentPieceKillEvent>() {
            @Override
            public void onEvent(SegmentPieceKillEvent event) {
                if(event.getPiece().getType() == MicroController.blockInfo.getId()) removeDataBlock(event.getPiece().getType(), new int[] {event.getPiece().getType(), event.getPiece().getType(), event.getPiece().getType()});
            }
        });
        StarLoader.registerListener(SegmentPieceSalvageEvent.class, new Listener<SegmentPieceSalvageEvent>() {
            @Override
            public void onEvent(SegmentPieceSalvageEvent event) {
                if(event.getBlockInternal().getType() == MicroController.blockInfo.getId()) removeDataBlock(event.getBlockInternal().getType(), new int[] {event.getBlockInternal().x, event.getBlockInternal().y, event.getBlockInternal().z});
            }
        });

        //Player Activated Block
        StarLoader.registerListener(SegmentPieceActivateByPlayer.class, new Listener<SegmentPieceActivateByPlayer>() {
            @Override
            public void onEvent(SegmentPieceActivateByPlayer event) {
                short id = event.getSegmentPiece().getType();
                PlayerState player = event.getPlayer();
                if(id == MicroController.blockInfo.getId()) {
                    //Todo
                }
            }
        });

        //Logic Activated Block
        StarLoader.registerListener(SegmentPieceActivateEvent.class, new Listener<SegmentPieceActivateEvent>() {
            @Override
            public void onEvent(SegmentPieceActivateEvent event) {
                short id = event.getSegmentPiece().getType();
                if(id == MicroController.blockInfo.getId()) {
                    //Todo
                }
            }
        });
    }

    private void removeDataBlock(short id, int[] position) {
        //Todo
    }

    public static Logiscript getInstance() {
        return inst;
    }
}