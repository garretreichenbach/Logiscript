package luamade.listener;

import api.listener.fastevents.TextBoxDrawListener;
import org.schema.game.client.view.SegmentDrawer;
import org.schema.game.client.view.textbox.AbstractTextBox;
import org.schema.game.common.data.SegmentPiece;

import java.util.concurrent.ConcurrentHashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class TextBlockDrawListener implements TextBoxDrawListener {

	private static TextBlockDrawListener instance;

	public static TextBlockDrawListener getInstance() {
		return instance;
	}

	public static final long UPDATE_INTERVAL = 1000L;

	public final ConcurrentHashMap<SegmentPiece, String> textMap = new ConcurrentHashMap<>();
	private long lastUpdate;

	public TextBlockDrawListener() {
		instance = this;
	}

	@Override
	public void draw(SegmentDrawer.TextBoxSeg.TextBoxElement textBoxElement, AbstractTextBox abstractTextBox) {
		if(System.currentTimeMillis() - lastUpdate > UPDATE_INTERVAL) {
			SegmentPiece segmentPiece = textBoxElement.c.getSegmentBuffer().getPointUnsave(textBoxElement.v);
			if(segmentPiece != null) {
				String text = textMap.get(segmentPiece);
				if(textBoxElement.rawText != null && !textBoxElement.rawText.equals(text)) {
					textMap.remove(segmentPiece);
					textMap.put(segmentPiece, textBoxElement.rawText);
				}
			}
			lastUpdate = System.currentTimeMillis();
		}
	}

	@Override
	public void preDrawBackground(SegmentDrawer.TextBoxSeg textBoxSeg, AbstractTextBox abstractTextBox) {

	}

	@Override
	public void preDraw(SegmentDrawer.TextBoxSeg.TextBoxElement textBoxElement, AbstractTextBox abstractTextBox) {

	}
}
