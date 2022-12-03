package luamade.manager;

import api.mod.config.FileConfiguration;
import luamade.LuaMade;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false",
			"max-world-logs: 5",
			"generate-shipwrecks-from-combat: true"
	};

	public static void initialize(LuaMade instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}