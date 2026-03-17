package luamade.system.module;

import api.utils.game.module.util.SystemModule;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.core.Timer;

import java.util.UUID;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends SystemModule {

	private final byte VERSION = 1;
	private final Long2ObjectOpenHashMap<ComputerModule> computerModules = new Long2ObjectOpenHashMap<>();

	public ComputerModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, LuaMade.getInstance(), ElementRegistry.COMPUTER.getId());
	}

	public static ComputerModuleContainer getContainer(ManagerContainer<?> managerContainer) {
		if(managerContainer.getModMCModule(ElementRegistry.COMPUTER.getId()) instanceof ComputerModuleContainer) {
			return (ComputerModuleContainer) managerContainer.getModMCModule(ElementRegistry.COMPUTER.getId());
		}
		return null;
	}

	@Override
	public void handle(Timer timer) {
		//Update loop tick
	}

	@Override
	public void handlePlace(long abs, byte orientation) {
		//Do nothing, because we are handling it ourselves with events
	}

	@Override
	public void handleRemove(long abs) {
		//Do nothing, because we are handling it ourselves with events
	}

	@Override
	public double getPowerConsumedPerSecondResting() {
		return 0;
	}

	@Override
	public double getPowerConsumedPerSecondCharging() {
		return 0;
	}

	@Override
	public String getName() {
		return "Computer";
	}

	public ComputerModule getModule(SegmentPiece segmentPiece) {
		if(computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			return computerModules.get(segmentPiece.getAbsoluteIndex());
		}
		return null;
	}

	public void addModule(SegmentPiece segmentPiece) {
		if(!computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			computerModules.put(segmentPiece.getAbsoluteIndex(), new ComputerModule(segmentPiece, UUID.randomUUID().toString()));
			flagUpdatedData();
		}
	}

	public void removeModule(SegmentPiece segmentPiece) {
		if(computerModules.containsKey(segmentPiece.getAbsoluteIndex())) {
			computerModules.remove(segmentPiece.getAbsoluteIndex());
			flagUpdatedData();
		}
	}

	public void updateModule(ComputerModule module) {
		if(computerModules.containsKey(module.getSegmentPiece().getAbsoluteIndex())) {
			computerModules.put(module.getSegmentPiece().getAbsoluteIndex(), module);
			flagUpdatedData();
		}
	}
}
