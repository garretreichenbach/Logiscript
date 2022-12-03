package thederpgamer.logiscript.api.entity;

import org.schema.game.common.controller.SegmentController;
import thederpgamer.logiscript.api.element.block.LuaBlock;

public class LuaEntity {
    private final SegmentController segmentController;
    public LuaEntity(SegmentController controller) {
        this.segmentController = controller;
    }

    public String getEntityName() {
        return segmentController.getRealName();
    }

    public void setName(String name) {
        segmentController.setRealName(name);
    }

    public LuaBlock getBlockAt(int[] pos) {
        return new LuaBlock(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
    }
}
