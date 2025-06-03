package luamade.manager;

import api.mod.config.FileConfiguration;
import luamade.LuaMade;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false",
			"console-character-limit: 30000",
			"console-line-limit: 1000",
			"max-threads-per-root-entity: 5"
	};

	public static void initialize(LuaMade instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}