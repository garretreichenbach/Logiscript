package luamade.manager;

import api.listener.Listener;
import api.listener.events.register.ManagerContainerRegisterEvent;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.listener.SegmentPieceListener;
import luamade.system.module.ComputerModuleContainer;

public class EventManager {

	private static final SegmentPieceListener segmentPieceListener = new SegmentPieceListener();

	public static void registerEvents(LuaMade instance) {
		FastListenerCommon.segmentPiecePlayerInteractListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceAddByMetadataListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceAddListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceRemoveListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceKilledListeners.add(segmentPieceListener);

		StarLoader.registerListener(ManagerContainerRegisterEvent.class, new Listener<ManagerContainerRegisterEvent>() {
			@Override
			public void onEvent(ManagerContainerRegisterEvent event) {
				event.addModMCModule(new ComputerModuleContainer(event.getSegmentController(), event.getContainer()));
			}
		}, instance);
	}
}
