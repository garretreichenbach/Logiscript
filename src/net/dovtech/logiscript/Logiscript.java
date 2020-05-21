package net.dovtech.logiscript;

import api.DebugFile;
import api.config.BlockConfig;
import api.mod.StarMod;

public class Logiscript extends StarMod {
    static Logiscript inst;
    public Logiscript() {
        inst = this;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.modName = "Logiscript";
        this.modAuthor = "DovTech";
        this.modVersion = "0.2.1";
        this.modDescription = "Adds an assembly-based logic scripting language to StarMade.";
        DebugFile.log("Enabled", this);
    }

    public void onBlockConfigLoad(BlockConfig config) {

    }
}