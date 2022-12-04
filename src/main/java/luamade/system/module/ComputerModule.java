package luamade.system.module;

import api.utils.game.module.util.SimpleDataStorageMCModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.gui.ComputerDialog;
import luamade.manager.LuaManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;

import java.util.HashMap;

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

	public String getScriptFromWeb(SegmentPiece segmentPiece, String link) {
		try {
			StringBuilder script = new StringBuilder();
			java.net.URL url = new java.net.URL(link);
			java.net.URLConnection connection = url.openConnection();
			java.io.InputStream inputStream = connection.getInputStream();
			java.io.BufferedReader bufferedReader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream));
			String line;
			while((line = bufferedReader.readLine()) != null) script.append(line).append("\n");
			bufferedReader.close();
			setScript(segmentPiece, script.toString());
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return getScript(segmentPiece);
	}

	public String getScript(SegmentPiece segmentPiece) {
		return getComputerMap().get(segmentPiece.getAbsoluteIndex());
	}

	private HashMap<Long, String> getComputerMap() {
		HashMap<Long, String> computerMap = new HashMap<>();
		if(data instanceof String && !((String) data).isEmpty()) {
			String[] computers = ((String) data).split("\\|");
			for(String computer : computers) {
				if(computer.contains(":")) {
					String[] computerData = computer.split("/");
					computerMap.put(Long.parseLong(computerData[0]), computerData[1]);
				}
			}
		}
		return computerMap;
	}

	private void setComputerMap(HashMap<Long, String> computerMap) {
		StringBuilder computerString = new StringBuilder();
		for(Long id : computerMap.keySet()) computerString.append(id).append("|").append(computerMap.get(id)).append("/");
		data = computerString.toString();
	}

	public void setScript(SegmentPiece segmentPiece, String script) {
		HashMap<Long, String> computerMap = getComputerMap();
		computerMap.put(segmentPiece.getAbsoluteIndex(), script);
		setComputerMap(computerMap);
		flagUpdatedData();
	}

	public void runScript(SegmentPiece segmentPiece) {
		if(!segmentPiece.getSegmentController().isOnServer()) return;
		try {
			LuaManager.run(getScript(segmentPiece), segmentPiece);
		} catch(Exception exception) {
			exception.printStackTrace();
			LuaManager.run("console.error(" + exception.getMessage() + ")", segmentPiece);
		}
	}

	public void openGUI(SegmentPiece segmentPiece) {
		try {
			ComputerDialog dialog = new ComputerDialog();
			dialog.getInputPanel().setValues(segmentPiece, getScript(segmentPiece), this);
			dialog.deactivate();
			dialog.getInputPanel().onInit();
			dialog.activate();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
