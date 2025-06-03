package luamade.manager;

import api.listener.Listener;
import api.listener.events.block.SegmentPieceActivateByPlayer;
import api.listener.events.block.SegmentPieceActivateEvent;
import api.listener.events.register.ManagerContainerRegisterEvent;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.element.block.ActivationInterface;
import luamade.element.block.Block;
import luamade.system.module.ComputerModuleOld;
import luamade.utils.SegmentPieceUtils;
import org.schema.game.common.data.SegmentPiece;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class EventManager {

	public static void initialize(LuaMade instance) {
		StarLoader.registerListener(ManagerContainerRegisterEvent.class, new Listener<ManagerContainerRegisterEvent>() {
			@Override
			public void onEvent(ManagerContainerRegisterEvent event) {
				event.addModMCModule(new ComputerModuleOld(event.getSegmentController(), event.getContainer()));
			}
		}, instance);

		StarLoader.registerListener(SegmentPieceActivateByPlayer.class, new Listener<SegmentPieceActivateByPlayer>() {
			@Override
			public void onEvent(SegmentPieceActivateByPlayer event) {
				for(Block block : ElementManager.getAllBlocks()) {
					if(block instanceof ActivationInterface && block.getId() == event.getSegmentPiece().getType()) {
						((ActivationInterface) block).onPlayerActivation(event);
						return;
					}
				}
			}
		}, instance);

		StarLoader.registerListener(SegmentPieceActivateEvent.class, new Listener<SegmentPieceActivateEvent>() {
			@Override
			public void onEvent(SegmentPieceActivateEvent event) {
				for(Block block : ElementManager.getAllBlocks()) {
					for(SegmentPiece segmentPiece : SegmentPieceUtils.getControlledPieces(event.getSegmentPiece())) {
						if(block instanceof ActivationInterface && segmentPiece.getType() == block.getId()) {
							((ActivationInterface) block).onLogicActivation(segmentPiece, event.getSegmentPiece().isActive());
							break;
						}
					}

				}
			}
		}, instance);

		/*
		StarLoader.registerListener(SegmentPieceAddByMetadataEvent.class, new Listener<SegmentPieceAddByMetadataEvent>() {
			@Override
			public void onEvent(SegmentPieceAddByMetadataEvent event) {
				try {
					if(event.getSegment().getSegmentController() instanceof ManagedSegmentController) {
						ManagedSegmentController<?> controller = (ManagedSegmentController<?>) event.getSegment().getSegmentController();
						ComputerModuleOld module = (ComputerModuleOld) controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId());
						SegmentPiece segmentPiece = event.getSegment().getSegmentController().getSegmentBuffer().getPointUnsave(event.getAbsIndex());
						if(segmentPiece != null && module.getData(segmentPiece).autoRun) module.runScript(segmentPiece);
					}
				} catch(Exception ignored) {}
			}
		}, instance);
		 */
	}
}