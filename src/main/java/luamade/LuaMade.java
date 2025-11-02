package luamade;

import api.common.GameServer;
import api.config.BlockConfig;
import api.listener.events.controller.ClientInitializeEvent;
import api.mod.StarMod;
import luamade.element.ElementManager;
import luamade.element.block.ComputerBlock;
import luamade.manager.*;
import org.schema.schine.resource.ResourceLoader;

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

	@Override
	public void onEnable() {
		instance = this;
		ConfigManager.initialize(this);
		EventManager.initialize(this);
	}

	@Override
	public void onClientCreated(ClientInitializeEvent clientInitializeEvent) {
		super.onClientCreated(clientInitializeEvent);
		GlossaryManager.initialize(this);
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

	@Override
	public void logInfo(String message) {
		super.logInfo(message);
		System.out.println("[INFO]: " + message);
	}

	@Override
	public void logWarning(String message) {
		super.logWarning(message);
		System.err.println("[WARNING]: " + message);
	}

	@Override
	public void logException(String message, Exception exception) {
		super.logException(message, exception);
		System.err.println("[ERROR]: " + message);
		exception.printStackTrace();
	}

	@Override
	public void logFatal(String message, Exception exception) {
		System.err.println("[FATAL]: " + message);
		exception.printStackTrace();
		try {
			GameServer.getServerState().addCountdownMessage(10, "SERVER EXECUTING EMERGENCY SHUTDOWN DUE TO FATAL ERROR");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}