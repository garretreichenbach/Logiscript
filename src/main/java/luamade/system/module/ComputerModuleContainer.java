package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.ModManagerContainerModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends ModManagerContainerModule {

	public ComputerModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, LuaMade.getInstance(), ElementManager.getBlock("Computer").getId());
	}

	@Override
	public void handle(Timer timer) {

	}

	@Override
	public void onTagSerialize(PacketWriteBuffer packetWriteBuffer) throws IOException {

	}

	@Override
	public void onTagDeserialize(PacketReadBuffer packetReadBuffer) throws IOException {

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
}
