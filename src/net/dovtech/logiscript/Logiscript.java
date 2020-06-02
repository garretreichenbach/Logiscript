package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.mod.StarMod;

public class Logiscript extends StarMod {
    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }

    public static void main(String[] args) {
        //Dont put anything in here, this is just for compilation purposes
    }

    @Override
    public void onGameStart() {
        this.modName = "Logiscript";
        this.modAuthor = "DovTech";
        this.modVersion = "0.2.5";
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";
    }

    @Override
    public void onEnable() {
        super.onEnable();
        DebugFile.log("Enabled", this);
    }

    public void onBlockConfigLoad(BlockConfig config) {

    }
}