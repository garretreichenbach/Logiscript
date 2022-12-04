package luamade.system.module;

import api.utils.game.module.util.SimpleDataStorageMCModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class ComputerModule extends SimpleDataStorageMCModule {

	public ComputerModule(SegmentController segmentController, ManagerContainer<?> managerContainer) {
		super(segmentController, managerContainer, LuaMade.getInstance(), ElementManager.getBlock("Computer").getId());
	}

	@Override
	public String getName() {
		return "Computer";
	}

	public String getScriptFromWeb(String link) {
		try {
			StringBuilder script = new StringBuilder();
			java.net.URL url = new java.net.URL(link);
			java.net.URLConnection connection = url.openConnection();
			java.io.InputStream inputStream = connection.getInputStream();
			java.io.BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
			String line;
			while((line = bufferedReader.readLine()) != null) script.append(line).append("\n");
			bufferedReader.close();
			setCurrentScript(script.toString());
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return getCurrentScript();
	}

	public String getCurrentScript() {
		if(!(data instanceof String)) setCurrentScript("");
		return (String) data;
	}

	public void setCurrentScript(String script) {
		data = script;
		flagUpdatedData();
	}
}
