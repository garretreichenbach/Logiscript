package luamade.network;

import api.network.Packet;
import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.network.packets.PacketUtil;
import luamade.lua.datastore.SharedDataStore;
import org.schema.game.common.data.player.PlayerState;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client → Server: request the contents of a data store.
 *
 * <p>{@code storeName} is empty for a regular DataStore and contains the
 * registered network name for a NetworkedDataStore. The server echoes it
 * back in {@link PacketSCDataStoreContents} so the client can open the
 * correct dialog.
 */
public class PacketCSRequestDataStoreContents extends Packet {

	/** Max characters sent per value to keep packet size reasonable. */
	private static final int MAX_DISPLAY_VALUE_LENGTH = 512;

	private String storeUuid;
	private String storeName;

	/** Required no-arg constructor for packet instantiation. */
	public PacketCSRequestDataStoreContents() {
	}

	public PacketCSRequestDataStoreContents(String storeUuid, String storeName) {
		this.storeUuid = storeUuid;
		this.storeName = storeName != null ? storeName : "";
	}

	@Override
	public void readPacketData(PacketReadBuffer buffer) throws IOException {
		storeUuid = buffer.readString();
		storeName = buffer.readString();
	}

	@Override
	public void writePacketData(PacketWriteBuffer buffer) throws IOException {
		buffer.writeString(storeUuid);
		buffer.writeString(storeName);
	}

	@Override
	public void processPacketOnClient() {
		// Not used — this packet travels client → server only.
	}

	@Override
	public void processPacketOnServer(PlayerState sender) {
		List<String> keys = SharedDataStore.keys(storeUuid, "");
		Map<String, String> data = new HashMap<>(keys.size());
		for(String key : keys) {
			String value = SharedDataStore.get(storeUuid, key);
			if(value == null) continue;
			if(value.length() > MAX_DISPLAY_VALUE_LENGTH) {
				value = value.substring(0, MAX_DISPLAY_VALUE_LENGTH) + "...";
			}
			data.put(key, value);
		}
		PacketUtil.sendPacket(sender, new PacketSCDataStoreContents(storeUuid, storeName, data));
	}
}
