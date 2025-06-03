package luamade.manager;

import glossar.GlossarCategory;
import glossar.GlossarEntry;
import glossar.GlossarInit;
import luamade.LuaMade;

import java.util.logging.Level;

/**
 * Manages the in-game glossary entries for the mod.
 * Loads documentation from markdown files and converts them to glossary entries.
 *
 * @author Garret Reichenbach
 */
public class GlossaryManager {

	/**
	 * Loads general documentation from markdown files and adds them as entries to the category.
	 * Falls back to hardcoded documentation if markdown files cannot be loaded.
	 * 
	 * @param category The category to add entries to
	 */
	private static void loadGeneralDocumentation(GlossarCategory category) {
		// Try to load documentation from markdown files
		String luaMadeContent = DocumentationManager.loadMarkdownFile("general/luamade.md");
		String channelsContent = DocumentationManager.loadMarkdownFile("general/channels.md");

		// Add entries to the category
		if (luaMadeContent != null) {
			String title = DocumentationManager.extractTitle(luaMadeContent);
			String content = DocumentationManager.extractContent(luaMadeContent);

			if (title != null && content != null) {
				category.addEntry(new GlossarEntry(title, content));
			} else {
				// Fall back to hardcoded documentation
				category.addEntry(new GlossarEntry("LuaMade", "LuaMade is a mod that allows you to run Lua scripts on Computer Blocks. You can use it to automate your ship, create a simple AI, or even create a game inside the game! To get started, place a Computer Block and activate it. You will be presented with a GUI. In the GUI, you can write Lua code and run it. You can also save your scripts to the Computer Block. You can also set the Computer Block to automatically run a script when it is placed."));
			}
		} else {
			// Fall back to hardcoded documentation
			category.addEntry(new GlossarEntry("LuaMade", "LuaMade is a mod that allows you to run Lua scripts on Computer Blocks. You can use it to automate your ship, create a simple AI, or even create a game inside the game! To get started, place a Computer Block and activate it. You will be presented with a GUI. In the GUI, you can write Lua code and run it. You can also save your scripts to the Computer Block. You can also set the Computer Block to automatically run a script when it is placed."));
		}

		if (channelsContent != null) {
			String title = DocumentationManager.extractTitle(channelsContent);
			String content = DocumentationManager.extractContent(channelsContent);

			if (title != null && content != null) {
				category.addEntry(new GlossarEntry(title, content));
			} else {
				// Fall back to hardcoded documentation
				category.addEntry(new GlossarEntry("Channels", "Channels can be used to securely write and receive data to and from different computers. They can also be used to save persistent data for a computer. Each channel is secured with a password that is required in order to read and write to the channel."));
			}
		} else {
			// Fall back to hardcoded documentation
			category.addEntry(new GlossarEntry("Channels", "Channels can be used to securely write and receive data to and from different computers. They can also be used to save persistent data for a computer. Each channel is secured with a password that is required in order to read and write to the channel."));
		}
	}

	/**
	 * Loads function documentation from markdown files and adds them as entries to the category.
	 * Falls back to hardcoded documentation if markdown files cannot be loaded.
	 * 
	 * @param category The category to add entries to
	 */
	private static void loadFunctionDocumentation(GlossarCategory category) {
		// Try to load documentation from markdown files
		loadFunctionDocumentationFile(category, "console.md", "Console",
				"getTime() - Returns the current system time.\n" +
				"getBlock() - Returns the Computer Block that the script is running on.\n" +
				"print(message<String>) - Prints a message from the console.\n" +
				"printColor(color<Double[]>, message<String>) - Prints a message from the console with a color.\n" +
				"printError(message<String>) - Prints an error message from the console.\n" +
				"getChannel(name<String>) - Returns a channel by name.\n" +
				"createChannel(name<String>, password<String>) - Creates a channel with the given name and password.\n" +
				"sendMail(sender<String>, playerName<String>, subject<String>, message<String>, password<String>) - Sends a mail to the specified player.\n" +
				"setVar(name<String>, value<Object>) - Sets a variable with the given name and value.\n" +
				"getVar(name<String>) - Returns a variable with the given name.");

		loadFunctionDocumentationFile(category, "block.md", "Block",
				"getPos() - Returns the position of the Block as a Vector3.\n" +
				"getId() - Returns the block's id.\n" +
				"getInfo() - Returns the block's element information.\n" +
				"isActive() - Returns whether the block is active.\n" +
				"setActive(active<Boolean>) - Sets whether the block is active.\n" +
				"getEntity() - Returns the block's entity.\n" +
				"hasInventory() - Returns whether the block has an inventory.\n" +
				"getInventory() - Returns the block's inventory.\n" +
				"isDisplayMode() - Returns whether the block can display text.\n" +
				"setDisplayText(text<String>) - Sets the block's display text.\n" +
				"getDisplayText() - Returns the block's display text.");

		loadFunctionDocumentationFile(category, "blockinfo.md", "BlockInfo",
				"getName() - Returns the name of the block.\n" +
				"getDescription() - Returns the description of the block.\n" +
				"getId() - Returns the id of the block.");

		loadFunctionDocumentationFile(category, "inventory.md", "Inventory",
				"getName() - Returns the name of the inventory.\n" +
				"getItems() - Returns an array of ItemStacks representing the items in the inventory.\n" +
				"getVolume() - Returns the volume of the inventory.\n" +
				"transferTo(inventory<Inventory>, items<ItemStack[]>) - Transfers items from this inventory to the specified inventory.");

		loadFunctionDocumentationFile(category, "itemstack.md", "ItemStack",
				"getId() - Returns the id of the ItemStack.\n" +
				"getInfo() - Returns the BlockInfo of the ItemStack.\n" +
				"getCount() - Returns the amount of the item in the ItemStack.");
	}

	/**
	 * Loads a function documentation file and adds it as an entry to the category.
	 * Falls back to hardcoded documentation if the file cannot be loaded.
	 * 
	 * @param category The category to add the entry to
	 * @param filename The name of the markdown file, relative to docs/markdown/functions
	 * @param title The title of the entry
	 * @param fallbackContent The content to use if the file cannot be loaded
	 */
	private static void loadFunctionDocumentationFile(GlossarCategory category, String filename, String title, String fallbackContent) {
		String content = DocumentationManager.loadMarkdownFile("functions/" + filename);

		if (content != null) {
			String extractedTitle = DocumentationManager.extractTitle(content);
			String extractedContent = DocumentationManager.extractContent(content);

			if (extractedTitle != null && extractedContent != null) {
				category.addEntry(new GlossarEntry(extractedTitle, extractedContent));
			} else {
				// Fall back to hardcoded documentation
				category.addEntry(new GlossarEntry(title, fallbackContent));
			}
		} else {
			// Fall back to hardcoded documentation
			category.addEntry(new GlossarEntry(title, fallbackContent));
		}
	}

	public static void initialize(LuaMade instance) {
		GlossarInit.initGlossar(instance);

		// Create the LuaMade category and load general documentation
		GlossarCategory luaMade = new GlossarCategory("LuaMade");
		loadGeneralDocumentation(luaMade);
		GlossarInit.addCategory(luaMade);

		// Create the Functions category and load function documentation
		GlossarCategory functions = new GlossarCategory("LuaMade - Functions");
		loadFunctionDocumentation(functions);
		// Add the rest of the function entries with hardcoded documentation
		// These could be moved to markdown files in the future
		functions.addEntry(new GlossarEntry("Entity",
				"getTime() - Returns the current system time.\n" +
						"getBlock() - Returns the Computer Block that the script is running on.\n" +
						"print(message<String>) - Prints a message from the console.\n" +
						"printColor(color<Double[]>, message<String>) - Prints a message from the console with a color.\n" +
						"printError(message<String>) - Prints an error message from the console.\n" +
						"getChannel(name<String>) - Returns a channel by name.\n" +
						"createChannel(name<String>, password<String>) - Creates a channel with the given name and password.\n" +
						"sendMail(sender<String>, playerName<String>, subject<String>, message<String>, password<String>) - Sends a mail to the specified player.\n" +
						"setVar(name<String>, value<Object>) - Sets a variable with the given name and value.\n" +
						"getVar(name<String>) - Returns a variable with the given name."));
		functions.addEntry(new GlossarEntry("Block",
				"getPos() - Returns the position of the Block as a Vector3.\n" +
						"getId() - Returns the block's id.\n" +
						"getInfo() - Returns the block's element information.\n" +
						"isActive() - Returns whether the block is active.\n" +
						"setActive(active<Boolean>) - Sets whether the block is active.\n" +
						"getEntity() - Returns the block's entity.\n" +
						"hasInventory() - Returns whether the block has an inventory.\n" +
						"getInventory() - Returns the block's inventory.\n" +
						"isDisplayMode() - Returns whether the block can display text.\n" +
						"setDisplayText(text<String>) - Sets the block's display text.\n" +
						"getDisplayText() - Returns the block's display text."));
		functions.addEntry(new GlossarEntry("BlockInfo",
				"getName() - Returns the name of the block.\n" +
						"getDescription() - Returns the description of the block.\n" +
						"getId() - Returns the id of the block."));
		functions.addEntry(new GlossarEntry("Inventory",
				"getName() - Returns the name of the inventory.\n" +
						"getItems() - Returns an array of ItemStacks representing the items in the inventory.\n" +
						"getVolume() - Returns the volume of the inventory.\n" +
						"transferTo(inventory<Inventory>, items<ItemStack[]>) - Transfers items from this inventory to the specified inventory."));
		functions.addEntry(new GlossarEntry("ItemStack",
				"getId() - Returns the id of the ItemStack.\n" +
						"getInfo() - Returns the BlockInfo of the ItemStack.\n" +
						"getCount() - Returns the amount of the item in the ItemStack."));
		functions.addEntry(new GlossarEntry("Entity",
				"getId() - Returns the ID of the entity.\n" +
						"getName() - Returns the name of the entity.\n" +
						"setName(name<String>) - Sets the name of the entity.\n" +
						"getBlockAt(pos<Vector3>) - Returns the block at the given position.\n" +
						"getAI() - Returns the entity's AI.\n" +
						"getPos() - Returns the position of the entity as a Vector3.\n" +
						"getSector() - Returns the entity's sector.\n" +
						"getSystem() - Returns the entity's system.\n" +
						"getSystemOwner() - Returns the faction that owns the current system.\n" +
						"getFaction() - Returns the entity's faction.\n" +
						"getNearbyEntities() - Returns an array of nearby (remote) entities.\n" +
						"getNearbyEntities(radius<Integer>) - Returns an array of nearby (remote) entities within the given radius.\n" +
						"hasReactor() - Returns whether the entity has a reactor.\n" +
						"getReactor() - Returns the entity's reactor.\n" +
						"getMaxReactorHP() - Returns the entity's reactor's max HP.\n" +
						"getReactorHP() - Returns the entity's reactor's HP.\n" +
						"getThrust() - Returns the entity's thrust system.\n" +
						"getTurrets() - Returns an array of the entity's turrets.\n" +
						"getDocked() - Returns an array of the entity's docked entities.\n" +
						"isEntityDocked(entity<RemoteEntity>) - Returns whether the given entity is docked to this entity.\n" +
						"undockEntity(entity<RemoteEntity>) - Undocks the given entity from this entity.\n" +
						"undockAll() - Undocks all entities from this entity.\n" +
						"dockTo(entity<RemoteEntity>, railDocker<Block>) - Dock this entity to the nearest dock on the remote entity using the provided rail docker block.\n" +
						"dockTo(entity<RemoteEntity>, railDocker<Block>, dockPos<Vector3>) - Dock this entity to the dock at the given position using the provided rail docker block.\n" +
						"getSpeed() - Returns the entity's speed.\n" +
						"getMass() - Returns the entity's mass.\n" +
						"isJamming() - Returns whether the entity is jamming.\n" +
						"canJam() - Returns whether the entity can jam.\n" +
						"activateJamming(active<Boolean>) - Activates the entity's jamming.\n" +"" +
						"isCloaking() - Returns whether the entity is cloaking.\n" +
						"canCloak() - Returns whether the entity can cloak.\n" +
						"activateCloaking(active<Boolean>) - Activates the entity's cloaking.\n" +
						"getShieldSystem() - Returns the entity's shield system.\n" +
						"getShipyards() - Returns an array of the entity's shipyards.\n" +
						"getEntityType() - Returns the entity's type.\n" +
						"getNamedInventory(name<String>) - Returns the entity's inventory with the given name.\n" +
						"getPilot() - Returns the name of the player currently piloting this entity.\n" +
						"isInFleet() - Returns whether the entity is in a fleet.\n" +
						"getFleet() - Returns the fleet the entity is in."));
		functions.addEntry(new GlossarEntry("RemoteEntity",
				"getId() - Returns the ID of the entity.\n" +
						"getName() - Returns the name of the entity.\n" +
						"getFaction() - Returns the entity's faction.\n" +
						"getSpeed() - Returns the entity's speed.\n" +
						"getMass() - Returns the entity's mass.\n" +
						"getPos() - Returns the position of the entity as a Vector3.\n" +
						"getSector() - Returns the entity's sector.\n" +
						"getSystem() - Returns the entity's system.\n" +
						"getShieldSystem() - Returns the entity's shield system.\n" +
						"getNamedInventory(name<String>) - Returns the entity's inventory with the given name.\n" +
						"getPilot() - Returns the name of the player currently piloting this entity."));
		functions.addEntry(new GlossarEntry("EntityAI",
				"setActive(active<Boolean>) - Sets whether the AI is active.\n" +
						"isActive() - Returns whether the AI is active.\n" +
						"moveToSector(sector<Vector3>) - Moves the entity to the given sector.\n" +
						"getTargetSector() - Returns the target sector of the entity.\n" +
						"setTarget(target<RemoteEntity>) - Sets the target of the entity.\n" +
						"getTarget() - Returns the target of the entity.\n" +
						"getTargetType() - Returns the target type of the AI.\n" +
						"setTargetType(type<String>) - Sets the target type of the AI.\n" +
						"moveToPos(pos<Vector3>) - Moves the entity to the given position within the sector.\n" +
						"moveToEntity(entity<RemoteEntity>) - Moves the entity to the given entity (nearby position, not colliding) as long as they are in the same sector.\n" +
						"stop() - Stops the entity's movement."));
		functions.addEntry(new GlossarEntry("Fleet",
				"getId() - Returns the ID of the fleet.\n" +
						"getName() - Returns the name of the fleet.\n" +
						"getFaction() - Returns the fleet's faction.\n" +
						"getSector() - Returns the fleet's current sector.\n" +
						"getFlagship() - Returns the fleet's flagship.\n" +
						"getMembers() - Returns an array of the fleet's members.\n" +
						"addMember(entity<RemoteEntity>) - Adds the given entity to the fleet.\n" +
						"removeMember(entity<RemoteEntity>) - Removes the given entity from the fleet.\n" +
						"getCurrentCommand() - Returns the current command of the fleet.\n" +
						"setCurrentCommand(command<String>, args<Object[]>) - Sets the current command of the fleet."));
		functions.addEntry(new GlossarEntry("FleetCommand",
				"getCommand() - Returns the command.\n" +
						"getTarget() - Returns the sector target of the command, if there is one.\n" +
						"getArgs() - Returns the command's arguments"));
		functions.addEntry(new GlossarEntry("Reactor",
				"getRecharge() - Returns the reactor's recharge rate.\n" +
						"getConsumption() - Returns the reactor's consumption rate.\n" +
						"getChamberCapacity() - Returns the reactor's chamber capacity.\n" +
						"getChamber(chamberName<String>) - Returns a reactor chamber by name.\n" +
						"getChambers() - Returns an array of the reactor's chambers.\n\"" +
						"getActiveChambers() - Returns an array of the reactor's active chambers.\n" +
						"getMaxHP() - Returns the reactor's max HP.\n" +
						"getHP() - Returns the reactor's HP."));
		functions.addEntry(new GlossarEntry("Chamber",
				"getName() - Returns the chamber's name.\n" +
						"getBlockInfo() - Returns the chamber's block info.\n" +
						"getReactor() - Returns the chamber's reactor.\n" +
						"specify(chamberName<String>) - Specifies the chamber's type by name.\n" +
						"getValidSpecifications() - Returns an array of valid specifications for the chamber.\n" +
						"isUsable() - Returns whether the chamber can be triggered.\n" +
						"getUsable() - Returns a usable instance of the chamber."));
		functions.addEntry(new GlossarEntry("UsableChamber",
				"activate() - Activates the chamber.\n" +
						"deactivate() - Deactivates the chamber.\n" +
						"getCharge() - Returns the chamber's charge."));
		functions.addEntry(new GlossarEntry("ShieldSystem",
				"isShielded() - Returns whether the entity is shielded.\n" +
						"getCurrent() - Returns the entity's current shield capacity.\n" +
						"getCapacity() - Returns the entity's max shield capacity.\n" +
						"getRegen() - Returns the entity's shield regeneration rate.\n" +
						"getAllShields() - Returns an array of all shields.\n" +
						"getActiveShields() - Returns an array of all active shields.\n" +
						"isShieldActive(index<Integer>) - Returns whether the shield with the given index is active."));
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
						"sendCommand(commandName<String>, args<Object[]>) - Sends a command to the shipyard with the given arguments."));
		functions.addEntry(new GlossarEntry("Faction",
				"getName() - Returns the faction's name.\n" +
						"isSameFaction(faction<Faction>) - Returns whether the faction is the same as the given faction.\n" +
						"isFriend(faction<Faction>) - Returns whether the faction is a friend of the given faction.\n" +
						"getFriends() - Returns an array of the faction's friends.\n" +
						"isEnemy(faction<Faction>) - Returns whether the faction is an enemy of the given faction.\n" +
						"getEnemies() - Returns an array of the faction's enemies.\n" +
						"isNeutral(faction<Faction>) - Returns whether the faction is neutral to the given faction."));
		functions.addEntry(new GlossarEntry("Channel",
				"getName() - Returns the channel's name.\n" +
						"getMessages() - Returns an array of the channel's messages. Requires password\n" +
						"getLatestMessage(password<String>) - Returns the latest message of the channel. Requires password.\n" +
						"sendMessage(password<String>, message<String>) - Sends a message to the channel. Requires password.\n" +
						"removeChannel(password<String>) - Removes the channel. Requires password."));
		functions.addEntry(new GlossarEntry("Vector3",
				"getX() - Returns the x coordinate.\n" +
						"getY() - Returns the y coordinate.\n" +
						"getZ() - Returns the z coordinate.\n" +
						"setX(x<Integer>) - Sets the x coordinate.\n" +
						"setY(y<Integer>) - Sets the y coordinate.\n" +
						"setZ(z<Integer>) - Sets the z coordinate.\n" +
						"add(toAdd<Vector3>) - Adds the given vector to this vector.\n" +
						"sub(toSub<Vector3>) - Subtracts the given vector from this vector.\n" +
						"mul(toMul<Vector3>) - Multiplies this vector by the given vector.\n" +
						"div(toDiv<Vector3>) - Divides this vector by the given vector.\n" +
						"scale(scalar<Double>) - Scales this vector by the given amount.\n" +
						"size() - Returns the length of this vector.\n" +
						"absolute() - Returns the absolute value of this vector.\n" +
						"negate() - Returns the negated value of this vector."));
		GlossarInit.addCategory(functions);
	}
}
