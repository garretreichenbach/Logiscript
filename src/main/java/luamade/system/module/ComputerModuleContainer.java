package luamade.system.module;

import api.network.PacketReadBuffer;
import api.network.PacketWriteBuffer;
import api.utils.game.module.util.SystemModule;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.schine.graphicsengine.core.Timer;

import java.io.IOException;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends SystemModule {

	private final byte VERSION = 1;

	public ComputerModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, LuaMade.getInstance(), ElementRegistry.COMPUTER.getId());
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

	@Override
	public void handle(Timer timer) {

	}

	@Override
	public void onTagSerialize(PacketWriteBuffer buffer) throws IOException {
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer buffer) throws IOException {
	}
}
