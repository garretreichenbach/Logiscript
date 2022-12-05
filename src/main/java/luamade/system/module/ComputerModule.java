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
		return getComputerMap().get(segmentPiece.getAbsoluteIndex())[1];
	}

	public boolean isAutoRun(SegmentPiece segmentPiece) {
		return getComputerMap().get(segmentPiece.getAbsoluteIndex())[0].equals("true");
	}

	public void setAutoRun(SegmentPiece segmentPiece, boolean autoRun) {
		getComputerMap().get(segmentPiece.getAbsoluteIndex())[0] = String.valueOf(autoRun);
	}

	private HashMap<Long, String[]> getComputerMap() {
		HashMap<Long, String[]> computerMap = new HashMap<>();
		if(data instanceof String && !((String) data).isEmpty()) {
			String[] computers = ((String) data).split("\\|");
			for(String computer : computers) {
				if(computer.contains("_,")) {
					try {
						//Format: index[autoRun, script]
						//Find first [ and last ]
						int firstBracket = computer.indexOf("[");
						int lastBracket = computer.lastIndexOf("]");
						//Get index
						long index = Long.parseLong(computer.substring(0, firstBracket));
						//Get autoRun and script
						String[] autoRunAndScript = computer.substring(firstBracket + 1, lastBracket).split(",");
						//Add to map
						computerMap.put(index, autoRunAndScript);
					} catch(Exception ignored) {}
				}
			}
		}
		return computerMap;
	}

	private void setComputerMap(HashMap<Long, String[]> computerMap) {
		StringBuilder computerString = new StringBuilder();
		for(Long id : computerMap.keySet()) {
			//Format: index[autoRun, script]
			computerString.append(id).append("[").append(computerMap.get(id)[0]).append(",").append(computerMap.get(id)[1]).append("]|");
		}
		data = computerString.toString();
		flagUpdatedData();
	}

	public void setScript(SegmentPiece segmentPiece, String script) {
		HashMap<Long, String[]> computerMap = getComputerMap();
		computerMap.put(segmentPiece.getAbsoluteIndex(), new String[] {"false", script});
		setComputerMap(computerMap);
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
			dialog.getInputPanel().setValues(segmentPiece, getScript(segmentPiece), this, isAutoRun(segmentPiece));
			dialog.getInputPanel().onInit();
			dialog.activate();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}
}
