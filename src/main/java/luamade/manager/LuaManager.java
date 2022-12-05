package luamade.manager;

import api.mod.config.PersistentObjectUtil;
import luamade.LuaMade;
import luamade.lua.Channel;
import luamade.lua.Console;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.schema.game.common.data.SegmentPiece;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {

	private static final ConcurrentHashMap<SegmentPiece, Thread> threadMap = new ConcurrentHashMap<>();
	private static final HashMap<String, Channel> channels = new HashMap<>();

	public static void initialize(LuaMade instance) {
		for(Object obj : PersistentObjectUtil.getObjects(instance.getSkeleton(), Channel.class)) {
			Channel channel = (Channel) obj;
			channels.put(channel.getName().tojstring(), channel);
		}
	}

	public static void run(final String script, final SegmentPiece segmentPiece) {
		if(threadMap.containsKey(segmentPiece)) {
			threadMap.get(segmentPiece).interrupt();
			threadMap.remove(segmentPiece);
		}
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
					Globals globals = JsePlatform.debugGlobals();
					Console console = new Console(segmentPiece);
					globals.set("luajava", LuaValue.NIL);
					globals.set("console", console);
					console.printError(LuaString.valueOf(exception.getMessage().split("stack traceback:")[0]));
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
}