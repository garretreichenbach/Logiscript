package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import luamade.gui.DataStoreDialog;
import luamade.gui.NetworkedDataStoreDialog;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Server → Client: delivers data store contents requested via
 * {@link PacketCSRequestDataStoreContents}.
 *
 * <p>On arrival the client caches the data in {@link ClientDataStoreCache}
 * and opens the appropriate dialog ({@link DataStoreDialog} or
 * {@link NetworkedDataStoreDialog}) depending on whether a store name is present.
 */
public class PacketSCDataStoreContents extends Packet {

	private String storeUuid;
	private String storeName;
	private Map<String, String> data;

	/** Required no-arg constructor for packet instantiation. */
	public PacketSCDataStoreContents() {
	}

	public PacketSCDataStoreContents(String storeUuid, String storeName, Map<String, String> data) {
		this.storeUuid = storeUuid;
		this.storeName = storeName != null ? storeName : "";
		this.data = data;
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		storeUuid = buffer.readString();
		storeName = buffer.readString();
		int count = buffer.readInt();
		data = new HashMap<>(count);
		for(int i = 0; i < count; i++) {
			data.put(buffer.readString(), buffer.readString());
		}
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeString(storeUuid);
		buffer.writeString(storeName);
		buffer.writeInt(data.size());
		for(Map.Entry<String, String> entry : data.entrySet()) {
			buffer.writeString(entry.getKey());
			buffer.writeString(entry.getValue());
		}
	}

	@Override
	public void processPacketOnClient() {
		ClientDataStoreCache.update(storeUuid, data);
		if(!storeName.isEmpty()) {
			new NetworkedDataStoreDialog(storeUuid, storeName).activate();
		} else {
			new DataStoreDialog(storeUuid).activate();
		}
	}

	@Override
	public void processPacketOnServer(PlayerState playerState) {
		// Not used — this packet travels server → client only.
	}
}
