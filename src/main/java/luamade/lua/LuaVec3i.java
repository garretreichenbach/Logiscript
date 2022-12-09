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
    public LuaVec3i incx(Integer x) {
        xx += x;
        return this;
    }
    @LuaMadeCallable
    public LuaVec3i incy(Integer y) {
        yy += y;
        return this;
    }
    @LuaMadeCallable
    public LuaVec3i incz(Integer z) {
        zz += z;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i inc(LuaVec3i vec) {
        xx += vec.xx;
        yy += vec.yy;
        zz += vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3i sum(LuaVec3i vec) {
        return new LuaVec3i(this).inc(vec);
    }

    @Override
    public String toString() {
        return String.format("LuaVec(%s, %s, %s)", xx, yy, zz);
    }
}
