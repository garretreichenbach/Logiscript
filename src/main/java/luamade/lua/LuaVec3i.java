package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.common.util.linAlg.Vector3i;

public class LuaVec3 extends LuaMadeUserdata {
    public int xx;
    public int yy;
    public int zz;

    public LuaVec3(int x, int y, int z) {
        xx = x;
        yy = y;
        zz = z;
    }

    public LuaVec3(Vector3i v) {
        this.LuaVec3(v.x, v.y, v.z);
    }

    public LuaVec3(LuaVec3 vec) {
        xx = vec.xx;
        yy = vec.yy;
        zz = vec.zz;
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
    public Integer incx(Integer x) {
        return xx += x;
    }
    @LuaMadeCallable
    public Integer incy(Integer y) {
        return yy += y;
    }
    @LuaMadeCallable
    public Integer incz(Integer z) {
        return zz += z;
    }

    @LuaMadeCallable
    public LuaVec3 inc(LuaVec3 vec) {
        xx += vec.xx;
        yy += vec.yy;
        zz += vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec3 sum(LuaVec3 vec) {
        return new LuaVec3(this).inc(vec);
    }
}
