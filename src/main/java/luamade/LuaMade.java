package luamade;

import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarMod;
import luamade.element.ElementRegistry;
import luamade.manager.ConfigManager;
import luamade.manager.EventManager;
import luamade.manager.GlossaryManager;
import luamade.manager.ResourceManager;
import org.schema.schine.resource.ResourceLoader;

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
		ConfigManager.initialize(this);
		EventManager.registerEvents(this);
	}

	@Override
	public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
		super.onClientCreated(clientInitializeEvent);
		GlossaryManager.initialize(this);
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