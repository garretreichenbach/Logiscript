package net.dovtech.logiscript.scripts;

import api.common.GameClient;
import org.schema.game.common.data.player.PlayerState;
import java.util.HashMap;

public class Script {

    private String name;
    private PlayerState uploader;
    private HashMap<String, ExternalConnection> externalConnections;
    private String[] rawScript;

    public Script() {
        this("New Script", GameClient.getClientPlayerState());
    }

    public Script(String name, PlayerState uploader) {
        this.name = name;
        this.uploader = uploader;
        this.externalConnections = new HashMap<>();
        this.rawScript = new String[] {

        };
    }

    public String[] getRawScript() {
        return rawScript;
    }
}
