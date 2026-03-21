package luamade.lua.data;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

public class BoundingBox extends LuaMadeUserdata {

	private final Vec3f min;
	private final Vec3f max;

	public BoundingBox(org.schema.schine.graphicsengine.forms.BoundingBox boundingBox) {
		this(new Vec3f(boundingBox.min), new Vec3f(boundingBox.max));
	}

	public BoundingBox(Vec3f min, Vec3f max) {
		this.min = new Vec3f(min);
		this.max = new Vec3f(max);
	}

	@LuaMadeCallable
	public Vec3f getMin() {
		return min;
	}

	@LuaMadeCallable
	public Vec3f getMax() {
		return max;
	}

	@LuaMadeCallable
	public Vec3f getCenter() {
		return new Vec3f(
				(min.x + max.x) / 2.0f,
				(min.y + max.y) / 2.0f,
				(min.z + max.z) / 2.0f
		);
	}

	@LuaMadeCallable
	public Vec3f getDimensions() {
		return new Vec3f(
				max.x - min.x,
				max.y - min.y,
				max.z - min.z
		);
	}

	@Override
	public String toString() {
		return String.format("BoundingBox(min=%s, max=%s)", min, max);
	}
}
