package luamade.lua.entity;

import luamade.lua.element.block.Block;
import luamade.lua.entity.ai.EntityAI;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.controller.SegmentController;

public class Entity {
	private final SegmentController segmentController;

	public Entity(SegmentController controller) {
		this.segmentController = controller;
	}

	public String getName() {
		return segmentController.getRealName();
	}

	public void setName(String name) {
		segmentController.setRealName(name);
	}

	public Block getBlockAt(int[] pos) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(pos[0], pos[1], pos[2]));
	}

	public Block getBlockAt(int x, int y, int z) {
		return new Block(segmentController.getSegmentBuffer().getPointUnsave(x, y, z));
	}

	public EntityAI getAI() {
		return new EntityAI(segmentController);
	}

	public int[] getSector() {
		Vector3i sector = segmentController.getSector(new Vector3i());
		return new int[] {sector.x, sector.y, sector.z};
	}

	public int[] getSystem() {
		Vector3i system = segmentController.getSystem(new Vector3i());
		return new int[] {system.x, system.y, system.z};
	}
}
