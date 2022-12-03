package luamade.api.entity;

import org.schema.game.common.controller.SegmentController;
import luamade.api.element.block.Block;

public class Entity {
    private final SegmentController segmentController;
    public Entity(SegmentController controller) {
        this.segmentController = controller;
    }

    public String getEntityName() {
        return segmentController.getRealName();
    }

    public void setName(String name) {
        segmentController.setRealName(name);
    }

    public Block getBlockAt(int[] pos) {
        return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
    }
}
