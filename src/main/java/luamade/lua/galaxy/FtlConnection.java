package luamade.lua.galaxy;

import luamade.lua.data.Vec3i;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.world.FTLConnection;

import java.util.ArrayList;
import java.util.List;

/**
 * Read-only wrapper over a StarMade {@link FTLConnection} — a single warp gate,
 * wormhole, or race-way link on the galaxy map. Each connection has one source
 * sector ({@link #getFrom()}) and one or more destination sectors
 * ({@link #getDestinations()}), aligned by index with {@link #getTypes()}.
 *
 * <p>Obtained via {@link Galaxy#getWarpGates()}.
 */
public class FtlConnection extends LuaMadeUserdata {

	private final FTLConnection connection;

	public FtlConnection(FTLConnection connection) {
		this.connection = connection;
	}

	/** Source sector of the connection, as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getFrom() {
		return new Vec3i(connection.from);
	}

	/** Destination sectors this connection leads to, as a {@code Vec3i[]}. */
	@LuaMadeCallable
	public Vec3i[] getDestinations() {
		return wrapList(connection.to);
	}

	/** Local landing positions inside each destination, aligned with {@link #getDestinations()}. */
	@LuaMadeCallable
	public Vec3i[] getDestinationLocals() {
		return wrapList(connection.toLoc);
	}

	/**
	 * Connection type per destination, aligned with {@link #getDestinations()}.
	 * Each entry is {@code "WARP_GATE"}, {@code "WORM_HOLE"}, {@code "RACE_WAY"},
	 * or {@code "UNKNOWN"}.
	 */
	@LuaMadeCallable
	public String[] getTypes() {
		List<Vector3i> params = connection.param;
		if(params == null) return new String[0];
		String[] out = new String[params.size()];
		for(int i = 0; i < params.size(); i++) {
			Vector3i p = params.get(i);
			out[i] = p == null ? "UNKNOWN" : typeName(p.x);
		}
		return out;
	}

	/** Destination entity/station UID for this connection, or an empty string. */
	@LuaMadeCallable
	public String getDestinationUID() {
		return connection.toUID == null ? "" : connection.toUID;
	}

	/** Number of destinations this connection links to. */
	@LuaMadeCallable
	public Integer getDestinationCount() {
		return connection.to == null ? 0 : connection.to.size();
	}

	private static String typeName(int type) {
		if(type == FTLConnection.TYPE_WARP_GATE) return "WARP_GATE";
		if(type == FTLConnection.TYPE_WORM_HOLE) return "WORM_HOLE";
		if(type == FTLConnection.TYPE_RACE_WAY) return "RACE_WAY";
		return "UNKNOWN";
	}

	private static Vec3i[] wrapList(List<Vector3i> list) {
		if(list == null) return new Vec3i[0];
		ArrayList<Vec3i> out = new ArrayList<>(list.size());
		for(Vector3i v : list) {
			if(v != null) out.add(new Vec3i(v));
		}
		return out.toArray(new Vec3i[0]);
	}
}
