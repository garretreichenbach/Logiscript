package luamade.lua;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;

public class LuaVec extends LuaMadeUserdata {
    public int xx;
    public int yy;
    public int zz;

    public LuaVec(int x, int y, int z) {
        xx = x;
        yy = y;
        zz = z;
    }

    public LuaVec(LuaVec vec) {
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
    public LuaVec inc(LuaVec vec) {
        xx += vec.xx;
        yy += vec.yy;
        zz += vec.zz;
        return this;
    }

    @LuaMadeCallable
    public LuaVec sum(LuaVec vec) {
        return new LuaVec(this).inc(vec);
    }
}
