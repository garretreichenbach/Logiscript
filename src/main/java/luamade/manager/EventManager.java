package luamade.manager;

import api.listener.fastevents.FastListenerCommon;
import luamade.LuaMade;
import luamade.listener.SegmentPieceListener;

public class EventManager {

	private static final SegmentPieceListener segmentPieceListener = new SegmentPieceListener();

	public static void registerEvents(LuaMade instance) {
		FastListenerCommon.segmentPiecePlayerInteractListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceAddByMetadataListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceAddListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceRemoveListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceKilledListeners.add(segmentPieceListener);
	}
}
