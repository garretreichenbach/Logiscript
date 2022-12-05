package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.data.SegmentPiece;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console extends LuaMadeUserdata {

	private final SegmentPiece segmentPiece;
	private long timer;

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
	}

	@LuaCallable
	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	@LuaCallable
	public void print(String string) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.out.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string, 1.0f, 1.0f, 1.0f, 1.0f);
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaCallable
	public void print(String string, float[] color) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.out.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string, color[0], color[1], color[2], color[3]);
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaCallable
	public void printError(String string) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.err.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string, 1.0f, 0.3f, 0.3f, 1.0f);
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaCallable
	public Channel getChannel(String name) {
		return LuaManager.getChannel(name);
	}

	@LuaCallable
	public Channel createChannel(String name, String password) {
		return LuaManager.createChannel(name, password);
	}
}
