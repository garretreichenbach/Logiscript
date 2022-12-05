package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;
import org.luaj.vm2.LuaDouble;
import org.luaj.vm2.LuaString;
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

	@LuaMadeCallable
	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	@LuaMadeCallable
	public void print(LuaString string) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.out.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string.tojstring(), 1.0f, 1.0f, 1.0f, 1.0f);
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaMadeCallable
	public void print(LuaString string, LuaDouble[] color) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.out.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string.tojstring(), color[0].tofloat(), color[1].tofloat(), color[2].tofloat(), color[3].tofloat());
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaMadeCallable
	public void printError(LuaString string) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			System.err.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string.tojstring(), 1.0f, 0.3f, 0.3f, 1.0f);
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaMadeCallable
	public Channel getChannel(LuaString name) {
		return LuaManager.getChannel(name.tojstring());
	}

	@LuaMadeCallable
	public Channel createChannel(LuaString name, LuaString password) {
		return LuaManager.createChannel(name.tojstring(), password.tojstring());
	}
}
