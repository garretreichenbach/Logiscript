package luamade.lua.data;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import javax.vecmath.Vector3f;

public class Vec3f extends LuaMadeUserdata {
    public float x;
    public float y;
    public float z;

    public Vec3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3f(Vector3f v) {
        this(v.x, v.y, v.z);
    }

    @LuaMadeCallable
    public Float getX() {
        return x;
    }

    @LuaMadeCallable
    public Float getY() {
        return y;
    }

    @LuaMadeCallable
    public Float getZ() {
        return z;
    }

    @LuaMadeCallable
    public void setX(Float x) {
        this.x = x;
    }

    @LuaMadeCallable
    public void setY(Float y) {
        this.y = y;
    }

    @LuaMadeCallable
    public void setZ(Float z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("Vec(%s, %s, %s)", x, y, z);
    }
}
