package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;

public class LuaVec3i extends LuaMadeUserdata {
    public int xx;
    public int yy;
    public int zz;

    public LuaVec3i(int x, int y, int z) {
        xx = x;
        yy = y;
        zz = z;
    }

    public LuaVec3i(Vector3i v) {
        this(v.x, v.y, v.z);
    }

    public LuaVec3i(LuaVec3i v) {
        this(v.xx, v.yy, v.zz);
    }

    @LuaMadeCallable
    public Integer x() {
        return xx;
    }
    @LuaMadeCallable
    public Integer y() {
        return yy;
    }
    @LuaMadeCallable
    public Integer z() {
        return zz;
    }

    @LuaMadeCallable
    public void setX(Integer x) {
        xx = x;
    }

    @LuaMadeCallable
    public void setY(Integer y) {
        yy = y;
    }

    @LuaMadeCallable
    public void setZ(Integer z) {
        zz = z;
    }

    @LuaMadeCallable
    public LuaVec3i incrementX(Integer x) {
        xx += x;
        return this;
    }
    @LuaMadeCallable
    public LuaVec3i incrementY(Integer y) {
        yy += y;
        return this;
    }
    @LuaMadeCallable
    public LuaVec3i incrementZ(Integer z) {
        zz += z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i add(LuaVec3i vec) {
        xx += vec.xx;
        yy += vec.yy;
        zz += vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i sub(LuaVec3i vec) {
        xx -= vec.xx;
        yy -= vec.yy;
        zz -= vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i mul(LuaVec3i vec) {
        xx *= vec.xx;
        yy *= vec.yy;
        zz *= vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i div(LuaVec3i vec) {
        xx /= vec.xx;
        yy /= vec.yy;
        zz /= vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i scale(Float scale) {
        xx *= scale;
        yy *= scale;
        zz *= scale;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i absolute() {
        xx = Math.abs(xx);
        yy = Math.abs(yy);
        zz = Math.abs(zz);
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i negate() {
        xx = -xx;
        yy = -yy;
        zz = -zz;
        return this;
    }

    @LuaMadeCallable
    public Float size() {
        return (float) Math.sqrt(xx * xx + yy * yy + zz * zz);
    }

    @Override
    public String toString() {
        return String.format("LuaVec(%s, %s, %s)", xx, yy, zz);
    }
}
