package luamade.manager;

import api.mod.config.PersistentObjectUtil;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.lua.Channel;
import luamade.lua.Console;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.luaj.vm2.lib.jse.JseOsLib;
import org.schema.game.common.data.SegmentPiece;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class LuaManager {
	private static Globals serverGlobals;
	private static final ConcurrentHashMap<SegmentPiece, Thread> threadMap = new ConcurrentHashMap<>();
	private static final HashMap<String, Channel> channels = new HashMap<>();
	private static Thread threadChecker;

	public static void initialize(LuaMade instance) {
		serverGlobals = new Globals();
		serverGlobals.load(new JseBaseLib());
		serverGlobals.load(new PackageLib());
		serverGlobals.load(new StringLib());
		serverGlobals.load(new JseMathLib());
		LoadState.install(serverGlobals);
		LuaC.install(serverGlobals);
		LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);

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
						} catch(Exception exception) {
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
					LuaValue console = new Console(segmentPiece);
					Globals globals = new Globals();
					globals.load(new JseBaseLib());
					globals.load(new PackageLib());
					globals.load(new Bit32Lib());
					globals.load(new TableLib());
					globals.load(new StringLib());
					globals.load(new JseMathLib());
					globals.load(new JseOsLib());
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

	private static class ReadOnlyLuaTable extends LuaTable {
		public ReadOnlyLuaTable(LuaValue table) {
			presize(table.length(), 0);
			for(Varargs n = table.next(LuaValue.NIL); !n.arg1().isnil(); n = table.next(n.arg1())) {
				LuaValue key = n.arg1();
				LuaValue value = n.arg(2);
				super.rawset(key, value.istable() ? new ReadOnlyLuaTable(value) : value);
			}
		}
		public LuaValue setmetatable(LuaValue metatable) { return error("table is read-only"); }
		public void set(int key, LuaValue value) { error("table is read-only"); }
		public void rawset(int key, LuaValue value) { error("table is read-only"); }
		public void rawset(LuaValue key, LuaValue value) { error("table is read-only"); }
		public LuaValue remove(int pos) { return error("table is read-only"); }
	}
}