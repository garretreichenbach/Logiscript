package luamade.element.block;

import api.common.GameClient;
import api.config.BlockConfig;
import api.listener.events.block.SegmentPieceActivateByPlayer;
import api.utils.game.module.ModManagerContainerModule;
import luamade.gui.ComputerDialog;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.controller.Ship;
import org.schema.game.common.controller.SpaceStation;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.graphicsengine.core.GraphicsContext;

public class ComputerBlock extends Block implements ActivationInterface {

	public ComputerBlock() {
		super("Computer", ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getType());
	}

	@Override
	public void initialize() {
		blockInfo.setDescription("A fully programmable computer utilizing the LUA language.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.1f;

		blockInfo.controlledBy.add((short) 405);
		blockInfo.controlledBy.add((short) 993);
		blockInfo.controlledBy.add((short) 666);
		blockInfo.controlledBy.add((short) 399);

		ElementKeyMap.getInfo(405).controlling.add(getId());
		ElementKeyMap.getInfo(993).controlling.add(getId());
		ElementKeyMap.getInfo(666).controlling.add(getId());
		ElementKeyMap.getInfo(399).controlling.add(getId());

		if(GraphicsContext.initialized) {
			try {
				//Todo: Make custom icon and textures
				blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
				blockInfo.setTextureId(ElementKeyMap.getInfo(451).getTextureIds());
			} catch(Exception ignored) {}
		}

		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(50, (short) 220));
		BlockConfig.add(blockInfo);
	}

	@Override
	public void onPlayerActivation(SegmentPieceActivateByPlayer event) {
		try {
			ComputerDialog dialog = new ComputerDialog(getModule(event.getSegmentPiece()));
			dialog.activate();
			if(GameClient.getClientState() != null) {
				GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().suspend(true);
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public void onLogicActivation(SegmentPiece target, boolean active) {

	}

	private ComputerModule getModule(SegmentPiece segmentPiece) {
		ManagerContainer<?> managerContainer;
		if(segmentPiece.getSegmentController() instanceof Ship) {
			managerContainer = ((Ship) segmentPiece.getSegmentController()).getManagerContainer();
		} else if(segmentPiece.getSegmentController() instanceof SpaceStation) {
			managerContainer = ((SpaceStation) segmentPiece.getSegmentController()).getManagerContainer();
		} else {
			throw new IllegalStateException("SegmentController is neither Ship nor SpaceStation!");
		}
		ModManagerContainerModule module = managerContainer.getModMCModule(segmentPiece.getType());
		if(module instanceof ComputerModuleContainer) {
			return ((ComputerModuleContainer) module).getComputerData(segmentPiece);
		} else {
			throw new IllegalStateException("ManagerContainer is not a ComputerModuleContainer!");
		}
	}
}