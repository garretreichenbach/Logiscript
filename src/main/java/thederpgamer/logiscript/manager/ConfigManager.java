package thederpgamer.logiscript.manager;

import api.mod.config.FileConfiguration;
import thederpgamer.logiscript.Logiscript;

public class ConfigManager {

	private static FileConfiguration mainConfig;
	private static final String[] defaultMainConfig = {
			"debug-mode: false",
			"max-world-logs: 5",
			"generate-shipwrecks-from-combat: true"
	};

	public static void initialize(Logiscript instance) {
		mainConfig = instance.getConfig("config");
		mainConfig.saveDefault(defaultMainConfig);
	}

	public static FileConfiguration getMainConfig() {
		return mainConfig;
	}
}