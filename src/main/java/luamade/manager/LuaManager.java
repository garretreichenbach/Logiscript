package luamade.manager;

import api.mod.config.PersistentObjectUtil;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.lua.Channel;
import luamade.lua.Console;
import luamade.system.module.ComputerModule;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.schema.game.common.data.ManagedSegmentController;
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

	private static final String[] WHITELISTED_LIBS = new String[] {
		"base",
		"string",
		"table",
		"math",
		"package",
		"bit32"
	};
	private static final HashMap<String, Channel> channels = new HashMap<>();
	private static final ConcurrentHashMap<SegmentPiece, Thread> threadMap = new ConcurrentHashMap<>();
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
					Globals globals = new Globals();
					globals.load(new JseBaseLib());
					globals.load(new PackageLib());
					globals.load(new StringLib());
					globals.load(new TableLib());
					globals.load(new JseMathLib());
					globals.load(new Bit32Lib());
					LuaC.install(globals);
					LuaString.s_metatable = new ReadOnlyLuaTable(LuaString.s_metatable);
					//Security Patches
					for(LuaValue key : globals.keys()) {
						LuaValue value = globals.get(key);
						if(value instanceof LuaTable) {
							LuaTable table = (LuaTable) value;
							if(table.getmetatable() != null) table.setmetatable(new ReadOnlyLuaTable(table.getmetatable()));
						}
						//Check for whitelisted libs
						boolean whitelisted = false;
						for(String lib : WHITELISTED_LIBS) {
							if(key.tojstring().equals(lib)) {
								whitelisted = true;
								break;
							}
						}
						if(!whitelisted) globals.set(key, LuaValue.NIL);
					}
					//
					LuaValue console = new Console(segmentPiece);
					globals.set("console", console);
					globals.set("print", console.get("print"));
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

	public static void setVariable(Console console, String name, Object value) {
		ComputerModule module = getModule(console.getSegmentPiece());
		if(module != null) module.getData(console.getSegmentPiece()).variables.put(name, value);
	}

	public static Object getVariable(Console console, String name) {
		ComputerModule module = getModule(console.getSegmentPiece());
		if(module != null) return module.getData(console.getSegmentPiece()).variables.get(name);
		return null;
	}

	private static ComputerModule getModule(SegmentPiece segmentPiece) {
		if(segmentPiece.getSegmentController() instanceof ManagedSegmentController<?>) {
			ManagedSegmentController<?> controller = (ManagedSegmentController<?>) segmentPiece.getSegmentController();
			if(controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId()) instanceof ComputerModule) {
				return (ComputerModule) controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId());
			}
		}
		return null;
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