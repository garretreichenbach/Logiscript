package net.dovtech.logiscript.util;

import net.dovtech.logiscript.system.ComputerSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

public class DataUtil {

    public static ArrayList<ComputerSystem> getAllSystems() {
        ArrayList<ComputerSystem> computerSystems = new ArrayList();

        try {
            File systemDataFolder = new File("moddata/Logiscript/systems");
            if(!(systemDataFolder.exists())) {
                systemDataFolder.mkdir();
            }

            for(File dataFile : systemDataFolder.listFiles()) {
                if(dataFile.getName().endsWith(".smdat")) {
                    FileInputStream systemDataFile = new FileInputStream(dataFile.getPath());
                    ObjectInputStream systemDataInput = new ObjectInputStream(systemDataFile);
                    ComputerSystem system = (ComputerSystem) systemDataInput.readObject();
                    if(system.)
                }
            }

            /*
            FileInputStream playerDataFile = new FileInputStream(pData.getPath());
            ObjectInputStream playerDataInput = new ObjectInputStream(playerDataFile);
            PlayerData playerData = (PlayerData) playerDataInput.readObject();
            for(String uid : playerData.contracts) {
                Contract contract = getContractFromUUID(uid);
                computerSystems.add(contract);
                if(BetterFactions.getInstance().debugMode) DebugFile.log("[DEBUG]: Added Contract " + contract.getName() + " to player " + internalPlayer.getName(), BetterFactions.getInstance());
            }
            playerDataInput.close();
            playerDataFile.close();
            */

        } catch(IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return computerSystems;
    }
}
