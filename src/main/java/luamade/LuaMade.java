package luamade;

import api.config.BlockConfig;
import api.mod.StarMod;
import api.network.packets.PacketUtil;
import luamade.element.ElementManager;
import luamade.element.block.ComputerBlock;
import luamade.manager.ConfigManager;
import luamade.manager.EventManager;
import luamade.manager.LuaManager;
import luamade.manager.ResourceManager;
import luamade.network.client.RunScriptPacket;
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
	}
}