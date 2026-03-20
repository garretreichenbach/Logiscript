package luamade.lua.data;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import javax.vecmath.Vector3f;
import org.schema.common.util.linAlg.Vector3i;

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

    public Vec3f(Vec3f v) {
        this(v.x, v.y, v.z);
    }

    public Vec3f(Vec3i v) {
        this(v.x, v.y, v.z);
    }

    public Vec3f(Vector3i v) {
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

    @LuaMadeCallable
    public Vec3f add(Vec3f vec) {
        x += vec.x;
        y += vec.y;
        z += vec.z;
        return this;
    }

    @LuaMadeCallable
    public Vec3f sub(Vec3f vec) {
        x -= vec.x;
        y -= vec.y;
        z -= vec.z;
        return this;
    }

    @LuaMadeCallable
    public Vec3f mul(Vec3f vec) {
        x *= vec.x;
        y *= vec.y;
        z *= vec.z;
        return this;
    }

    @LuaMadeCallable
    public Vec3f div(Vec3f vec) {
        x /= vec.x;
        y /= vec.y;
        z /= vec.z;
        return this;
    }

    @LuaMadeCallable
    public Vec3f scale(Float scale) {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    @LuaMadeCallable
    public Vec3f absolute() {
        x = Math.abs(x);
        y = Math.abs(y);
        z = Math.abs(z);
        return this;
    }

    @LuaMadeCallable
    public Vec3f negate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    @LuaMadeCallable
    public Double size() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @LuaMadeCallable
    public Vec3i toVec3i() {
        return new Vec3i((int) x, (int) y, (int) z);
    }

    @LuaMadeCallable
    public Vec3i floorToVec3i() {
        return new Vec3i((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
    }

    @LuaMadeCallable
    public Vec3i roundToVec3i() {
        return new Vec3i(Math.round(x), Math.round(y), Math.round(z));
    }

    @Override
    public String toString() {
        return String.format("Vec(%s, %s, %s)", x, y, z);
    }
}
