package luamade.lua.galaxy;

import luamade.lua.data.Vec3i;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;

/**
 * Lightweight snapshot of one star on the galaxy map: where it is, what kind of
 * star it is and its colour. Derived from the galaxy's generation data, so it
 * never loads the system itself. Use {@link Galaxy#getSystemAt(Vec3i)} on
 * {@link #getSystemPos()} for name / ownership.
 *
 * <p>Obtained via {@link Galaxy#getStars(Vec3i, Integer)}.
 */
public class StarInfo extends LuaMadeUserdata {

	private final Vector3i systemPos;
	private final String type;
	private final Double[] color;

	public StarInfo(Vector3i systemPos, String type, Double[] color) {
		this.systemPos = systemPos;
		this.type = type;
		this.color = color;
	}

	/** Absolute system-grid position of the star's system, as a {@code Vec3i}. */
	@LuaMadeCallable
	public Vec3i getSystemPos() {
		return new Vec3i(systemPos);
	}

	/** {@code "SUN"}, {@code "GIANT"} or {@code "DOUBLE_STAR"}. */
	@LuaMadeCallable
	public String getType() {
		return type;
	}

	/** Star colour as {@code {r, g, b, a}} normalized floats. */
	@LuaMadeCallable
	public Double[] getColor() {
		return color.clone();
	}
}
