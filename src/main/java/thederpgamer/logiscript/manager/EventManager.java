package thederpgamer.logiscript.manager;

import api.listener.Listener;
import api.listener.events.block.SegmentPieceActivateEvent;
import api.mod.StarLoader;
import com.bulletphysics.linearmath.Transform;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.data.SegmentPiece;
import thederpgamer.logiscript.Logiscript;
import thederpgamer.logiscript.listener.TextBlockDrawListener;
import thederpgamer.logiscript.utils.SegmentPieceUtils;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class EventManager {

	public static void initialize(Logiscript instance) {
		StarLoader.registerListener(SegmentPieceActivateEvent.class, new Listener<SegmentPieceActivateEvent>() {
			@Override
			public void onEvent(SegmentPieceActivateEvent event) {
				if(!event.getSegmentPiece().isActive()) return;
				short id = event.getSegmentPiece().getType();
				if(id == 405 || id == 993 || id == 666 || id == 399) {
					ArrayList<SegmentPiece> pieces = SegmentPieceUtils.getControlledPiecesMatching(event.getSegmentPiece(), (short) 479);
					if(!pieces.isEmpty()) {
						for(SegmentPiece piece : pieces) {
							if(TextBlockDrawListener.getInstance().textMap.containsKey(piece)) {
								String text = TextBlockDrawListener.getInstance().textMap.get(piece);
								//Find starting and closing <lua> tags
								int start = text.indexOf("<lua");
								int end = text.indexOf("</lua>");
								if(start != -1 && end != -1) {
									Transform transform = new Transform();
									piece.getTransform(transform);
									try {
										String lua;
										lua = text.substring(start + 5, end);
										LuaManager.run(lua, piece);
									} catch(Exception exception) {
										Logiscript.log.log(Level.WARNING, exception.getMessage(), exception);
										RaisingIndication raisingIndication = new RaisingIndication(transform, exception.getLocalizedMessage(), 1.0f, 0.1f, 0.1f, 1.0f);
										raisingIndication.speed = 0.03f;
										raisingIndication.lifetime = 6.6f;
										HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
									}
								}
							}
						}
					}
				}
			}
		}, instance);
	}
}