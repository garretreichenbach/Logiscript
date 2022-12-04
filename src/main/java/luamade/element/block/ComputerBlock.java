package luamade.element.block;

import api.config.BlockConfig;
import api.listener.events.block.SegmentPieceActivateByPlayer;
import api.listener.events.block.SegmentPieceActivateEvent;
import luamade.system.module.ComputerModule;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.schine.graphicsengine.core.GraphicsContext;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ComputerBlock extends Block implements ActivationInterface {

	public ComputerBlock() {
		super("Computer", ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getType());
	}

	@Override
	public void initialize() {
		blockInfo.setDescription("A fully programmable computer utilizing the LUA language.\nCan save and load scripts from the client or via pastebin.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setCanActivate(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price);
		blockInfo.setOrientatable(true);

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
				//Todo: Make icon and textures
				//blockInfo.setBuildIconNum(ResourceManager.getTexture("computer-block-icon").getTextureId());
				blockInfo.setTextureId(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getTextureIds());
				//blockInfo.setTextureId(0, (short) ResourceManager.getTexture("computer-block-front").getTextureId());
			} catch(Exception ignored) {}
		}

		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(50, (short) 220));
		BlockConfig.add(blockInfo);
	}

	@Override
	public void onPlayerActivation(SegmentPieceActivateByPlayer event) {
		try {
			ComputerModule computerModule = getModule(event.getSegmentPiece());
			assert computerModule != null;
			computerModule.openGUI(event.getSegmentPiece());
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public void onLogicActivation(SegmentPieceActivateEvent event) {
		if(event.getSegmentPiece().isActive()) {
			try {
				ComputerModule computerModule = getModule(event.getSegmentPiece());
				if(computerModule != null && !computerModule.getScript(event.getSegmentPiece()).isEmpty()) computerModule.runScript(event.getSegmentPiece());
			} catch(Exception exception) {
				exception.printStackTrace();
			}
		}
	}

	private ComputerModule getModule(SegmentPiece segmentPiece) {
		try {
			ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
			return (ComputerModule) controller.getManagerContainer().getModMCModule(getId());
		} catch(Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}
}