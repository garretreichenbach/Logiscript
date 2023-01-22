package luamade.lua;

import com.bulletphysics.linearmath.Transform;
import luamade.element.ElementManager;
import luamade.lua.element.block.Block;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import luamade.manager.LuaManager;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.Varargs;
import org.schema.game.client.view.effects.RaisingIndication;
import org.schema.game.client.view.gui.shiphud.HudIndicatorOverlay;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;

import java.util.PriorityQueue;
import java.util.Queue;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Console extends LuaMadeUserdata {

	private final SegmentPiece segmentPiece;
	private final Queue<Object[]> printQueue = new PriorityQueue<>();

	public Console(SegmentPiece segmentPiece) {
		this.segmentPiece = segmentPiece;
		startPrintThread();
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
		printQueue.add(new Object[] {new Double[] {1.0, 1.0, 1.0, 1.0}, vargs});
	}

	@LuaMadeCallable
	public void printColor(Double[] color, Varargs vargs) {
		printQueue.add(new Object[] {color, vargs});
	}

	@LuaMadeCallable
	public void printError(Varargs vargs) {
		printQueue.add(new Object[] {new Double[] {1.0, 0.3, 0.3, 1.0}, vargs});
	}

	@LuaMadeCallable
	public Channel getChannel(String name) {
		return LuaManager.getChannel(name);
	}

	@LuaMadeCallable
	public Channel createChannel(String name, String password) {
		return LuaManager.createChannel(name, password);
	}

	@LuaMadeCallable
	public void sendMail(String sender, String playerName, String subject, String message, String password) {
		LuaManager.sendMail(sender, playerName, subject, message, password);
	}

	@LuaMadeCallable
	public void setVar(String name, Object value) {
		LuaManager.setVariable(this, name, value);
	}

	@LuaMadeCallable
	public Object getVar(String name) {
		return LuaManager.getVariable(this, name);
	}

	public SegmentPiece getSegmentPiece() {
		return segmentPiece;
	}

	public void sendOutput(String string) {
		try {
			if(segmentPiece.getSegmentController() instanceof ManagedSegmentController) {
				ManagedSegmentController<?> managedSegmentController = (ManagedSegmentController<?>) segmentPiece.getSegmentController();
				ComputerModule module = (ComputerModule) managedSegmentController.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId());
				module.getData(segmentPiece).lastOutput += string;
				module.flagUpdatedData();
			}
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	public static void sendError(SegmentPiece segmentPiece, String text) {
		try {
			System.err.println(text);
			LuaManager.getModule(segmentPiece).getData(segmentPiece).lastOutput += text;
		} catch(Exception ignored) {}
	}

	private void startPrintThread() {
		Thread printThread = new Thread() {
			@Override
			public void run() {
				try {
					while(true) {
						if(!printQueue.isEmpty()) {
							Object[] objects = printQueue.poll();
							Double[] color = (Double[]) objects[0];
							Varargs vargs = (Varargs) objects[1];
							StringBuilder string = new StringBuilder();
							for(int i = 1; i <= vargs.narg() && i <= 16; ++i) string.append(vargs.arg(i).toString()).append("\n");
							System.out.println(string);
							Transform transform = new Transform();
							segmentPiece.getTransform(transform);
							RaisingIndication raisingIndication = new RaisingIndication(transform, string.toString(), color[0].floatValue(), color[1].floatValue(), color[2].floatValue(), color[3].floatValue());
							raisingIndication.speed = 0.1f;
							raisingIndication.lifetime = 15.0f;
							HudIndicatorOverlay.toDrawTexts.add(raisingIndication);
							sendOutput(string.toString());
						}
						Thread.sleep(2000);
					}
				} catch(Exception exception) {
					exception.printStackTrace();
					startPrintThread();
				}
			}
		};
		printThread.start();
	}
}
