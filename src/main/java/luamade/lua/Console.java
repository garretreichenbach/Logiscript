package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.element.block.Block;
import luamade.manager.LuaManager;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.data.SegmentPiece;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console {

	private final SegmentPiece segmentPiece;

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	public void print(String string) {
		System.out.println(string);
		Transform transform = new Transform();
		segmentPiece.getTransform(transform);
		RaisingIndication raisingIndication = new RaisingIndication(transform, string, 1.0f, 1.0f, 1.0f, 1.0f);
		raisingIndication.speed = 0.1f;
		raisingIndication.lifetime = 4.6f;
		HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
	}

	public void err(String string) {
		System.err.println(string);
		Transform transform = new Transform();
		segmentPiece.getTransform(transform);
		RaisingIndication raisingIndication = new RaisingIndication(transform, string, 1.0f, 0.3f, 0.3f, 1.0f);
		raisingIndication.speed = 0.1f;
		raisingIndication.lifetime = 4.6f;
		HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
	}

	public Channel getChannel(String name) {
		return LuaManager.getChannel(name);
	}

	public Channel createChannel(String name, String password) {
		return LuaManager.createChannel(name, password);
	}
}
