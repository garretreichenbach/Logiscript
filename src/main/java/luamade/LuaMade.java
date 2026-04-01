package luamade;

import api.config.BlockConfig;
import api.mod.StarMod;
import luamade.element.ElementRegistry;
import luamade.lua.peripheral.PeripheralRegistry;
import luamade.manager.ComputerDataCleanupManager;
import luamade.manager.ConfigManager;
import luamade.manager.EventManager;
import luamade.manager.ResourceManager;
import luamade.system.module.ComputerModuleContainer;
import org.schema.schine.resource.ResourceLoader;

import java.util.Set;

public class LuaMade extends StarMod {

	//Instance
	private static LuaMade instance;

	public LuaMade() {
		instance = this;
	}

	public static LuaMade getInstance() {
		return instance;
	}

	public static void main(String[] args) {
	}

	@Override
	public void onEnable() {
		instance = this;
		PeripheralRegistry.registerDefaults();
		ConfigManager.initialize(this);
		EventManager.registerEvents(this);
	}

	@Override
	public void onDisable() {
		try {
			Set<String> protectedComputerUUIDs = ComputerModuleContainer.snapshotActiveComputerUUIDs();
			ComputerModuleContainer.saveAndCleanupAll();
			ComputerDataCleanupManager.cleanupOrphanedComputerData(protectedComputerUUIDs);
		} catch(Exception exception) {
			logException("Failed to save computer data on disable", exception);
		}
		super.onDisable();
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources(this, loader);
	}

	public void logDebug(String message) {
		if(ConfigManager.isDebugMode()) {
			logMessage("[DEBUG]: [ResourcesReorganized] " + message);
		}
	}
}