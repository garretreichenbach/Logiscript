package luamade;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import luamade.element.ElementManager;
import luamade.element.block.ComputerBlock;
import luamade.manager.ConfigManager;
import luamade.manager.EventManager;
import luamade.manager.LuaManager;
import luamade.manager.ResourceManager;
import luamade.network.client.RunScriptPacket;
import luamade.network.client.SaveScriptPacket;
import luamade.network.client.SetAutoRunPacket;
import luamade.network.client.TerminateScriptPacket;
import luamade.utils.DataUtils;
import org.schema.schine.resource.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class LuaMade extends StarMod {

	//Instance
	private static LuaMade instance;
	public LuaMade() {

	}
	public static LuaMade getInstance() {
		return instance;
	}
	public static void main(String[] args) {
	}

	//Data
	public static Logger log;

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		initLogger();
		EventManager.initialize(this);
		LuaManager.initialize(this);
		registerPackets();
	}

	@Override
	public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
		super.onClientCreated(clientInitializeEvent);
		registerGlossary();
	}


	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementManager.addBlock(new ComputerBlock());
		ElementManager.initialize();
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources(this, loader);
	}

	private void initLogger() {
		String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
		File logsFolder = new File(logFolderPath);
		if(!logsFolder.exists()) logsFolder.mkdirs();
		else {
			if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
				File[] logFiles = new File[logsFolder.listFiles().length];
				int j = logFiles.length - 1;
				for(int i = 0; i < logFiles.length && j >= 0; i++) {
					try {
						if(!logFiles[i].getName().endsWith(".lck")) logFiles[j] = logFiles[i];
						else logFiles[i].delete();
						j--;
					} catch(Exception ignored) { }
				}

				//Trim null entries
				int nullCount = 0;
				for(File value : logFiles) {
					if(value == null) nullCount ++;
				}

				File[] trimmedLogFiles = new File[logFiles.length - nullCount];
				int l = 0;
				for(File file : logFiles) {
					if(file != null) {
						trimmedLogFiles[l] = file;
						l++;
					}
				}

				for(File logFile : trimmedLogFiles) {
					if(logFile == null) continue;
					String fileName = logFile.getName().replace(".txt", "");
					int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
					String newName = logFolderPath + "/log" + logNumber + ".txt";
					if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
					else logFile.delete();
				}
			}
		}
		try {
			File newLogFile = new File(logFolderPath + "/log0.txt");
			if(newLogFile.exists()) newLogFile.delete();
			newLogFile.createNewFile();
			log = Logger.getLogger(newLogFile.getPath());
			FileHandler handler = new FileHandler(newLogFile.getPath());
			log.addHandler(handler);
			SimpleFormatter formatter = new SimpleFormatter();
			handler.setFormatter(formatter);
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	private void registerPackets() {
		PacketUtil.registerPacket(RunScriptPacket.class);
		PacketUtil.registerPacket(SaveScriptPacket.class);
		PacketUtil.registerPacket(TerminateScriptPacket.class);
		PacketUtil.registerPacket(SetAutoRunPacket.class);
	}

	private void registerGlossary() {
		GlossarInit.initGlossar(this);
		GlossarCategory luaMade = new GlossarCategory("LuaMade");
		luaMade.addEntry(new GlossarEntry("LuaMade", "LuaMade is a mod that allows you to run Lua scripts on Computer Blocks. You can use it to automate your ship, create a simple AI, or even create a game inside the game! To get started, place a Computer Block and activate it. You will be presented with a GUI. In the GUI, you can write Lua code and run it. You can also save your scripts to the Computer Block. You can also set the Computer Block to automatically run a script when it is placed."));
		luaMade.addEntry(new GlossarEntry("Channels", "Channels can be used to securely write and receive data to and from different computers. They can also be used to save persistent data for a computer. Each channel is secured with a password that is required in order to read and write to the channel."));
		GlossarInit.addCategory(luaMade);

		GlossarCategory functions = new GlossarCategory("LuaMade - Functions");
		functions.addEntry(new GlossarEntry("Console",
				"getTime() - Returns the current system time.\n" +
				"getBlock() - Returns the Computer Block that the script is running on.\n" +
				"print(String) - Prints a message from the console.\n" +
				"printColor(Float[], String) - Prints a message from the console with a color.\n" +
				"printError(String) - Prints an error message from the console.\n" +
				"getChannel(String) - Returns a channel by name.\n" +
				"createChannel(String, String) - Creates a channel with the given name and password.\n" +
				"setVar(String, Object) - Sets a variable with the given name and value.\n" +
				"getVar(String) - Returns a variable with the given name."));
		functions.addEntry(new GlossarEntry("Block",
				"getPos() - Returns the position of the Block as a Vector3.\n" +
				"getId() - Returns the block's id.\n" +
				"getInfo() - Returns the block's element information.\n" +
				"isActive() - Returns whether the block is active.\n" +
				"setActive(Boolean) - Sets whether the block is active.\n" +
				"getEntity() - Returns the block's entity.\n" +
				"hasInventory() - Returns whether the block has an inventory.\n" +
				"getInventory() - Returns the block's inventory."));
		functions.addEntry(new GlossarEntry("BlockInfo",
				"getName() - Returns the name of the block.\n" +
				"getDescription() - Returns the description of the block.\n" +
				"getId() - Returns the id of the block."));
		functions.addEntry(new GlossarEntry("Inventory",
				"getName() - Returns the name of the inventory.\n" +
				"getItems() - Returns an array of ItemStacks representing the items in the inventory."));
		functions.addEntry(new GlossarEntry("ItemStack",
				"getId() - Returns the id of the ItemStack.\n" +
				"getInfo() - Returns the BlockInfo of the ItemStack.\n" +
				"getCount() - Returns the amount of the item in the ItemStack."));
		functions.addEntry(new GlossarEntry("Entity",
				"getId() - Returns the ID of the entity.\n" +
				"getName() - Returns the name of the entity.\n" +
				"setName(String) - Sets the name of the entity.\n" +
				"getBlockAt(Vector3) - Returns the block at the given position.\n" +
				"getAI() - Returns the entity's AI.\n" +
				"getPos() - Returns the position of the entity as a Vector3.\n" +
				"getSector() - Returns the entity's sector.\n" +
				"getSystem() - Returns the entity's system.\n" +
				"getFaction() - Returns the entity's faction.\n" +
				"getNearbyEntities() - Returns an array of nearby (remote) entities.\n" +
				"getNearbyEntities(Float) - Returns an array of nearby (remote) entities within the given radius.\n" +
				"hasReactor() - Returns whether the entity has a reactor.\n" +
				"getReactor() - Returns the entity's reactor.\n" +
				"getMaxReactorHP() - Returns the entity's reactor's max HP.\n" +
				"getReactorHP() - Returns the entity's reactor's HP.\n" +
				"getThrust() - Returns the entity's thrust system.\n" +
				"getTurrets() - Returns an array of the entity's turrets.\n" +
				"getDocked() - Returns an array of the entity's docked entities.\n" +
				"isEntityDocked(RemoteEntity) - Returns whether the given entity is docked to this entity.\n" +
				"undockEntity(RemoteEntity) - Undocks the given entity from this entity.\n" +
				"getSpeed() - Returns the entity's speed.\n" +
				"getMass() - Returns the entity's mass.\n" +
				"isJamming() - Returns whether the entity is jamming.\n" +
				"canJam() - Returns whether the entity can jam.\n" +
				"activateJamming(Boolean) - Activates the entity's jamming.\n" +"" +
				"isCloaking() - Returns whether the entity is cloaking.\n" +
				"canCloak() - Returns whether the entity can cloak.\n" +
				"activateCloaking(Boolean) - Activates the entity's cloaking.\n" +
				"getShieldSystem() - Returns the entity's shield system.\n" +
				"getShipyards() - Returns an array of the entity's shipyards.\n" +
				"getEntityType() - Returns the entity's type."));
		functions.addEntry(new GlossarEntry("RemoteEntity",
				"getId() - Returns the ID of the entity.\n" +
				"getName() - Returns the name of the entity.\n" +
				"getFaction() - Returns the entity's faction.\n" +
				"getSpeed() - Returns the entity's speed.\n" +
				"getMass() - Returns the entity's mass.\n" +
				"getPos() - Returns the position of the entity as a Vector3.\n" +
				"getSector() - Returns the entity's sector.\n" +
				"getSystem() - Returns the entity's system.\n" +
				"getShieldSystem() - Returns the entity's shield system."));
		functions.addEntry(new GlossarEntry("EntityAI",
				"setActive(Boolean) - Sets whether the AI is active.\n" +
				"isActive() - Returns whether the AI is active.\n" +
				"moveToSector(Vector3) - Moves the entity to the given sector.\n" +
				"getTargetSector() - Returns the target sector of the entity.\n" +
				"setTarget(RemoteEntity) - Sets the target of the entity.\n" +
				"getTarget() - Returns the target of the entity."));
		functions.addEntry(new GlossarEntry("Reactor",
				"getRecharge() - Returns the reactor's recharge rate.\n" +
				"getConsumption() - Returns the reactor's consumption rate.\n" +
				"getChamberCapacity() - Returns the reactor's chamber capacity.\n" +
				"getChamber(String) - Returns a reactor chamber by name.\n" +
				"getChambers() - Returns an array of the reactor's chambers.\n\"" +
				"getActiveChambers() - Returns an array of the reactor's active chambers.\n" +
				"getMaxHP() - Returns the reactor's max HP.\n" +
				"getHP() - Returns the reactor's HP."));
		functions.addEntry(new GlossarEntry("Chamber",
				"getName() - Returns the chamber's name.\n" +
				"getBlockInfo() - Returns the chamber's block info.\n" +
				"getReactor() - Returns the chamber's reactor.\n" +
				"specify(String) - Specifies the chamber's type by name.\n" +
				"getValidSpecifications() - Returns an array of valid specifications for the chamber.\n" +
				"deactivate() - Deactivates the chamber.\n" +
				"canTrigger() - Returns whether the chamber can be triggered.\n" +
				"getCharge() - Returns the chamber's charge.\n" +
				"trigger() - Triggers the chamber."));
		functions.addEntry(new GlossarEntry("ShieldSystem",
				"isShielded() - Returns whether the entity is shielded.\n" +
				"getCurrent() - Returns the entity's current shield capacity.\n" +
				"getCapacity() - Returns the entity's max shield capacity.\n" +
				"getRegen() - Returns the entity's shield regeneration rate.\n" +
				"getAllShields() - Returns an array of all shields.\n" +
				"getActiveShields() - Returns an array of all active shields.\n" +
				"isShieldActive(Integer) - Returns whether the shield with the given index is active."));
		functions.addEntry(new GlossarEntry("Shield",
				"getCurrent() - Returns the shield's current capacity.\n" +
				"getCapacity() - Returns the shield's max capacity.\n" +
				"getRegen() - Returns the shield's regeneration rate.\n" +
				"isActive() - Returns whether the shield is active."));
		functions.addEntry(new GlossarEntry("Thrust",
				"getTMR() - Returns the thrust to mass ratio of the entity.\n" +
				"getThrust() - Returns the thrust power of the entity.\n" +
				"getMaxSpeed() - Returns the max speed of the entity.\n" +
				"getSize() - Returns the amount of thrusters the enemy has."));
		functions.addEntry(new GlossarEntry("Shipyard",
				"isFinished() - Returns whether the shipyard is finished.\n" +
				"getCompletion() - Returns the shipyard's completion.\n" +
				"isDocked() - Returns whether the shipyard has a docked entity.\n" +
				"isVirtualDocked() - Returns whether the shipyard has a virtual docked entity\n" +
				"getDocked() - Returns the (remote) docked entity.\n" +
				"canUndock() - Returns whether the shipyard can undock.\n" +
				"getRequired() - Returns all the resources required for the current command.\n" +
				"getCurrent() - Returns the current resources of the shipyard.\n" +
				"getNeeded() - Returns the needed resources to finish the current command.\n" +
				"sendCommand(String, Object[]) - Sends a command to the shipyard with the given arguments."));
		functions.addEntry(new GlossarEntry("Faction",
				"getName() - Returns the faction's name.\n" +
				"isSameFaciton(Faction) - Returns whether the faction is the same as the given faction.\n" +
				"isFriend(Faction) - Returns whether the faction is a friend of the given faction.\n" +
				"getFriends() - Returns an array of the faction's friends.\n" +
				"isEnemy(Faction) - Returns whether the faction is an enemy of the given faction.\n" +
				"getEnemies() - Returns an array of the faction's enemies.\n" +
				"isNeutral(Faction) - Returns whether the faction is neutral to the given faction."));
		functions.addEntry(new GlossarEntry("Channel",
				"getName() - Returns the channel's name.\n" +
				"getMessages() - Returns an array of the channel's messages. Requires password\n" +
				"getLatestMessage(String) - Returns the latest message of the channel. Requires password.\n" +
				"sendMessage(String, String) - Sends a message to the channel. Requires password.\n" +
				"removeChannel(String) - Removes the channel. Requires password."));
		functions.addEntry(new GlossarEntry("Vector3",
				"x() - Returns the x coordinate.\n" +
				"y() - Returns the y coordinate.\n" +
				"z() - Returns the z coordinate.\n" +
				"setX(Integer) - Sets the x coordinate.\n" +
				"setY(Integer) - Sets the y coordinate.\n" +
				"setZ(Integer) - Sets the z coordinate.\n" +
				"incrementX(Integer) - Increases the x coordinate by the given amount.\n" +
				"incrementY(Integer) - Increases the y coordinate by the given amount.\n" +
				"incrementZ(Integer) - Increases the z coordinate by the given amount.\n" +
				"add(Vector3) - Adds the given vector to this vector.\n" +
				"sub(Vector3) - Subtracts the given vector from this vector.\n" +
				"mul(Vector3) - Multiplies this vector by the given vector.\n" +
				"div(Vector3) - Divides this vector by the given vector.\n" +
				"scale(Float) - Scales this vector by the given amount.\n" +
				"size() - Returns the length of this vector.\n" +
				"absolute() - Returns the absolute value of this vector.\n" +
				"negate() - Returns the negated value of this vector."));
		GlossarInit.addCategory(functions);
	}
}