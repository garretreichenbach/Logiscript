package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;

public class LuaVec3i extends LuaMadeUserdata {
    public int x;
    public int y;
    public int z;

    public LuaVec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public LuaVec3i(Vector3i v) {
        this(v.x, v.y, v.z);
    }

    public LuaVec3i(LuaVec3i v) {
        this(v.x, v.y, v.z);
    }

    @LuaMadeCallable
    public Integer getX() {
        return x;
    }
    @LuaMadeCallable
    public Integer getY() {
        return y;
    }
    @LuaMadeCallable
    public Integer getZ() {
        return z;
    }

    @LuaMadeCallable
    public void setX(Integer x) {
        this.x = x;
    }

    @LuaMadeCallable
    public void setY(Integer y) {
        this.y = y;
    }

    @LuaMadeCallable
    public void setZ(Integer z) {
        this.z = z;
    }

    @LuaMadeCallable
    public LuaVec3i add(LuaVec3i vec) {
        x += vec.x;
        y += vec.y;
        z += vec.z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i sub(LuaVec3i vec) {
        x -= vec.x;
        y -= vec.y;
        z -= vec.z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i mul(LuaVec3i vec) {
        x *= vec.x;
        y *= vec.y;
        z *= vec.z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i div(LuaVec3i vec) {
        x /= vec.x;
        y /= vec.y;
        z /= vec.z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i scale(Float scale) {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i absolute() {
        x = Math.abs(x);
        y = Math.abs(y);
        z = Math.abs(z);
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i negate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    @LuaMadeCallable
    public Double size() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    @Override
    public String toString() {
        return String.format("LuaVec(%s, %s, %s)", x, y, z);
    }
}
