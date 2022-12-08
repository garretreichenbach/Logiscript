package luamade.manager;

import api.mod.config.PersistentObjectUtil;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.lua.Channel;
import luamade.lua.Console;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	private static final HashMap<SegmentPiece, Thread> threadMap = new HashMap<>();
	private static final HashMap<String, Channel> channels = new HashMap<>();
	private static Thread threadChecker;

	public static void initialize(LuaMade instance) {
		for(Object obj : PersistentObjectUtil.getObjects(instance.getSkeleton(), Channel.class)) {
			Channel channel = (Channel) obj;
			channels.put(channel.getName(), channel);
		}

		if(threadChecker == null || !threadChecker.isAlive() || threadChecker.isInterrupted()) {
			if(threadChecker != null) {
				threadChecker.stop();
				LuaMade.log.log(Level.WARNING, "Lua thread checker was interrupted! Restarting...");
			}
			threadChecker = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {
							Thread.sleep(10000);
							for(SegmentPiece segmentPiece : threadMap.keySet()) {
								if(segmentPiece == null || segmentPiece.getSegmentController() == null || !segmentPiece.isAlive() || !segmentPiece.getSegmentController().getSegmentBuffer().existsPointUnsave(segmentPiece.getAbsoluteIndex()) || segmentPiece.getSegmentController().getSegmentBuffer().getPointUnsave(segmentPiece.getAbsoluteIndex()).getType() != ElementManager.getBlock("Computer").getId()) terminate(segmentPiece);
							}
						} catch(InterruptedException exception) {
							exception.printStackTrace();
						}
					}
				}
			};
			threadChecker.start();
		}
	}

	public static void run(final String script, final SegmentPiece segmentPiece) {
		terminate(segmentPiece);
		threadMap.put(segmentPiece, new Thread(segmentPiece.toString()) {
			@Override
			public void run() {
				try {
					Globals globals = JsePlatform.debugGlobals();
					LuaValue console = new Console(segmentPiece);
					globals.set("luajava", LuaValue.NIL);
					globals.set("console", console);
					LuaValue chunk = globals.load(script);
					chunk.call();
				} catch(Exception exception) {
					exception.printStackTrace();
				}
			}
		});
		threadMap.get(segmentPiece).start();
	}

	public static Channel getChannel(String name) {
		return channels.get(name);
	}

	public static Channel createChannel(String name, String password) {
		if(channels.containsKey(name)) return null;
		Channel channel = new Channel(name, password);
		channels.put(name, channel);
		PersistentObjectUtil.addObject(LuaMade.getInstance().getSkeleton(), channel);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
		return channel;
	}

	public static void removeChannel(String name) {
		if(!channels.containsKey(name)) return;
		Channel channel = channels.get(name);
		channels.remove(name);
		PersistentObjectUtil.removeObject(LuaMade.getInstance().getSkeleton(), channel);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
	}

	public static void terminate(SegmentPiece segmentPiece) {
		if(threadMap.containsKey(segmentPiece)) {
			threadMap.get(segmentPiece).stop();
			threadMap.remove(segmentPiece);
			System.out.println("Terminated script for " + segmentPiece);
		}
	}
}