package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;
import org.luaj.vm2.Varargs;
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
	public Long getTime() {
		return System.currentTimeMillis();
	}

	@LuaMadeCallable
	public Block getBlock() {
		return new Block(segmentPiece); //Block is basically a wrapper class for SegmentPiece
	}

	@LuaMadeCallable
	public void print(Varargs vargs) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			String string = "";
			for (int i = 1; i <= vargs.narg() && i <= 16; ++i)
				string += vargs.arg(i).toString() + "\n";

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


	@LuaMadeCallable
	public void printColor(Float[] color, Varargs vargs) {
		//Only allow printing every 2 seconds
		if(System.currentTimeMillis() - timer > 2000) {
			String string = "";
			for (int i = 1; i <= vargs.narg() && i <= 16; ++i)
				string += vargs.arg(i).toString() + "\n";

			System.out.println(string);
			Transform transform = new Transform();
			segmentPiece.getTransform(transform);
			RaisingIndication raisingIndication = new RaisingIndication(transform, string, color[0].floatValue(), color[1].floatValue(), color[2].floatValue(), color[3].floatValue());
			raisingIndication.speed = 0.1f;
			raisingIndication.lifetime = 15.0f;
			HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
			timer = System.currentTimeMillis();
		}
	}

	@LuaMadeCallable
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

	@LuaMadeCallable
	public Channel getChannel(String name) {
		return LuaManager.getChannel(name);
	}

	@LuaMadeCallable
	public Channel createChannel(String name, String password) {
		return LuaManager.createChannel(name, password);
	}
}
