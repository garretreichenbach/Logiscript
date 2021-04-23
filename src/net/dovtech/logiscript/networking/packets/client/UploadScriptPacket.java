/*
 * Packet [Client -> Server]
 */

package net.dovtech.logiscript.networking.packets.client;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import net.dovtech.logiscript.scripts.Script;
import org.schema.game.common.data.player.PlayerState;
import java.io.IOException;

public class UploadScriptPacket extends Packet {

    public UploadScriptPacket() {

    }

    public UploadScriptPacket(Script script) {

    }

    @Override
    public void readPacketData(PacketReadBuffer packetReadBuffer) throws IOException {

    }

    @Override
    public void writePacketData(PacketWriteBuffer packetWriteBuffer) throws IOException {

    }

    @Override
    public void processPacketOnClient() {

    }

    @Override
    public void processPacketOnServer(PlayerState playerState) {

    }
}