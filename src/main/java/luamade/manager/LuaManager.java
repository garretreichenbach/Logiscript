package luamade.manager;

import api.common.GameServer;
import api.mod.config.PersistentObjectUtil;
import api.utils.other.HashList;
import luamade.LuaMade;
import luamade.data.PlayerData;
import luamade.element.ElementManager;
import luamade.lua.Channel;
import luamade.lua.Console;
import luamade.system.module.ComputerModuleOld;
import org.luaj.vm2.*;
import org.luaj.vm2.compiler.LuaC;
import org.luaj.vm2.lib.Bit32Lib;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.StringLib;
import org.luaj.vm2.lib.TableLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseMathLib;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.ManagedSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.schine.graphicsengine.movie.craterstudio.data.tuples.Pair;

import java.util.ArrayList;
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
	private static final HashMap<String, PlayerData> playerDataMap = new HashMap<>();
	private static final HashMap<String, Channel> channels = new HashMap<>();
 	private static final ConcurrentHashMap<SegmentPiece, Thread> threadMap = new ConcurrentHashMap<>();
	private static Thread threadChecker;

	public static void initialize(LuaMade instance) {
		for(Object obj : PersistentObjectUtil.getObjects(instance.getSkeleton(), Channel.class)) {
			Channel.ChannelSerializable channelSerializable = (Channel.ChannelSerializable) obj;
			Channel channel = channelSerializable.toChannel();
			channels.put(channel.getName(), channel);
		}

		for(Object obj : PersistentObjectUtil.getObjects(instance.getSkeleton(), PlayerData.class)) {
			PlayerData playerData = (PlayerData) obj;
			playerDataMap.put(playerData.getName(), playerData);
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
		doLagCheck(segmentPiece);

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
					// Create and set up console
					Console console = new Console(segmentPiece);
					globals.set("console", console);
					globals.set("print", console.get("print"));

					// Create and set up file system
					luamade.lua.fs.FileSystem fs = new luamade.lua.fs.FileSystem(segmentPiece);
					globals.set("fs", fs);

					// Create and set up terminal
					luamade.lua.terminal.Terminal term = new luamade.lua.terminal.Terminal(segmentPiece, console, fs);
					globals.set("term", term);

					// Create and set up network interface
					luamade.lua.networking.NetworkInterface net = new luamade.lua.networking.NetworkInterface(segmentPiece);
					globals.set("net", net);
					LuaValue chunk = globals.load(script);
					chunk.call();
				} catch(Exception exception) {
					exception.printStackTrace();
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
					// Create and set up console
					Console console = new Console(segmentPiece);
					globals.set("console", console);
					LuaValue chunk = globals.load("console:printError(\"" + exception.getMessage().replace("\"", "\\\"") + "\")");
					chunk.call();
				}
			}
		});
		threadMap.get(segmentPiece).start();
	}

	/**
	 * Checks if the entity is lagging or over the thread limit. If it is, pauses threads starting with the oldest until the entity is no longer lagging or the thread limit is no longer reached.
	 * @param segmentPiece The segment piece to check for lag.
	 */
	private static void doLagCheck(SegmentPiece segmentPiece) {
		if(isEntityOverThreadLimit(segmentPiece.getSegmentController().railController.getRoot())) {
			HashList<SegmentController, Pair<SegmentPiece, Thread>> entityThreadMap = getEntityThreadMap();
			int index = 0;
			//Pause the oldest thread until the entity is under the thread limit
			while(isEntityOverThreadLimit(segmentPiece.getSegmentController().railController.getRoot())) {
				Pair<SegmentPiece, Thread> pair = entityThreadMap.get(segmentPiece.getSegmentController().railController.getRoot()).get(index);
				pauseThread(pair.first());
				index ++;
			}
		} else { //Unpause any paused computers as long as the entity is under the thread limit
			HashList<SegmentController, Pair<SegmentPiece, Thread>> entityThreadMap = getEntityThreadMap();
			if(entityThreadMap.containsKey(segmentPiece.getSegmentController().railController.getRoot())) {
				for(Pair<SegmentPiece, Thread> pair : entityThreadMap.get(segmentPiece.getSegmentController().railController.getRoot())) {
					if(isEntityOverThreadLimit(segmentPiece.getSegmentController().railController.getRoot())) return;
					if(pair.second().getState() == Thread.State.WAITING) pair.second().notify();
				}
			}
		}
	}

	public static boolean isEntityOverThreadLimit(SegmentController root) {
		int threadCount = 0;
		int maxThreads = ConfigManager.getMainConfig().getConfigurableInt("max-threads-per-root-entity", 5);
		HashList<SegmentController, Pair<SegmentPiece, Thread>> entityThreadMap = getEntityThreadMap();
		if(entityThreadMap.containsKey(root)) threadCount = entityThreadMap.get(root).size();
		return threadCount >= maxThreads;
	}

	public static HashList<SegmentController, Pair<SegmentPiece, Thread>> getEntityThreadMap() {
		HashList<SegmentController, Pair<SegmentPiece, Thread>> entityThreadMap = new HashList<>();
		for(SegmentPiece segmentPiece : threadMap.keySet()) {
			SegmentController segmentController = segmentPiece.getSegmentController().railController.getRoot();
			if(!entityThreadMap.containsKey(segmentController)) entityThreadMap.put(segmentController, new ArrayList<Pair<SegmentPiece, Thread>>());
			entityThreadMap.get(segmentController).add(new Pair<>(segmentPiece, threadMap.get(segmentPiece)));
		}
		return entityThreadMap;
	}

	public static void pauseThread(SegmentPiece segmentPiece) {
		if(threadMap.containsKey(segmentPiece)) {
			Console.sendError(segmentPiece, "[WARNING]: Computer paused due to server lag!");
			threadMap.get(segmentPiece).suspend();
		}
	}

	public static Channel getChannel(String name) {
		return channels.get(name);
	}

	public static Channel createChannel(String name, String password) {
		if(channels.containsKey(name)) return null;
		Channel channel = new Channel(name, password, new String[0]);
		channels.put(name, channel);
		Channel.ChannelSerializable serializable = new Channel.ChannelSerializable(channel);
		PersistentObjectUtil.addObject(LuaMade.getInstance().getSkeleton(), serializable);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
		return channel;
	}

	public static void removeChannel(String name) {
		if(!channels.containsKey(name)) return;
		Channel channel = channels.get(name);
		channels.remove(name);
		Channel.ChannelSerializable serializable = new Channel.ChannelSerializable(channel);
		PersistentObjectUtil.removeObject(LuaMade.getInstance().getSkeleton(), serializable);
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
		ComputerModuleOld module = getModule(console.getSegmentPiece());
		if(module != null) module.getData(console.getSegmentPiece()).variables.put(name, value);
	}

	public static Object getVariable(Console console, String name) {
		ComputerModuleOld module = getModule(console.getSegmentPiece());
		if(module != null) return module.getData(console.getSegmentPiece()).variables.get(name);
		return null;
	}

	public static ComputerModuleOld getModule(SegmentPiece segmentPiece) {
		if(segmentPiece.getSegmentController() instanceof ManagedSegmentController<?>) {
			ManagedSegmentController<?> controller = (ManagedSegmentController<?>) segmentPiece.getSegmentController();
			if(controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId()) instanceof ComputerModuleOld) {
				return (ComputerModuleOld) controller.getManagerContainer().getModMCModule(ElementManager.getBlock("Computer").getId());
			}
		}
		return null;
	}

	public static PlayerData getPlayerData(String name) {
		if(!playerDataMap.containsKey(name)) {
			PlayerData data = new PlayerData(name, "");
			playerDataMap.put(name, data);
			PersistentObjectUtil.addObject(LuaMade.getInstance().getSkeleton(), data);
			PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
		}
		return playerDataMap.get(name);
	}

	public static void savePlayerData(PlayerData data) {
		removePlayerData(data.getName());
		PersistentObjectUtil.addObject(LuaMade.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
	}

	public static void removePlayerData(String name) {
		ArrayList<PlayerData> toRemove = new ArrayList<>();
		for(Object obj : PersistentObjectUtil.getObjects(LuaMade.getInstance().getSkeleton(), PlayerData.class)) {
			if(obj instanceof PlayerData) {
				PlayerData data = (PlayerData) obj;
				if(data.getName().equals(name)) toRemove.add(data);
			}
		}
		for(PlayerData data : toRemove) PersistentObjectUtil.removeObject(LuaMade.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
	}

	public static void removePlayerData(PlayerData data) {
		PersistentObjectUtil.removeObject(LuaMade.getInstance().getSkeleton(), data);
		PersistentObjectUtil.save(LuaMade.getInstance().getSkeleton());
	}

	public static void sendMail(String from, String playerName, String subject, String message, String password) {
		PlayerData data = getPlayerData(playerName);
		if(data.getPassword().equals(password) || data.getPassword().isEmpty()) {
			try {
				GameServer.getServerState().getServerPlayerMessager().send(from, playerName, subject, message);
			} catch(Exception ignored) {}
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
