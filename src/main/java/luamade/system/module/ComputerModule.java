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
			getData(segmentPiece).script = script.toString();
			flagUpdatedData();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
		return getData(segmentPiece).script;
	}

	public ComputerData getData(SegmentPiece segmentPiece) {
		return getComputerMap().get(segmentPiece.getAbsoluteIndex());
	}

	public void setData(SegmentPiece segmentPiece, ComputerData computerData) {
		getComputerMap().remove(segmentPiece.getAbsoluteIndex());
		getComputerMap().put(segmentPiece.getAbsoluteIndex(), computerData);
		flagUpdatedData();
	}

	private HashMap<Long, ComputerData> getComputerMap() {
		HashMap<Long, ComputerData> computerMap = new HashMap<>();
		if(data != null) computerMap = (HashMap<Long, ComputerData>) data;
		else data = computerMap;
		return computerMap;
	}

	public void runScript(SegmentPiece segmentPiece) {
		if(!segmentPiece.getSegmentController().isOnServer()) return;
		try {
			LuaManager.run(getData(segmentPiece).script, segmentPiece);
		} catch(Exception exception) {
			exception.printStackTrace();
			LuaManager.run("console.error(" + exception.getMessage() + ")", segmentPiece);
		}
	}

	public void openGUI(SegmentPiece segmentPiece) {
		try {
			ComputerDialog dialog = new ComputerDialog();
			dialog.getInputPanel().setValues(segmentPiece,this, getData(segmentPiece));
			dialog.getInputPanel().onInit();
			dialog.activate();
		} catch(Exception exception) {
			exception.printStackTrace();
		}
	}

	public static class ComputerData {

		public long index;
		public boolean autoRun;
		public String script;

		public ComputerData(long index, boolean autoRun, String script) {
			this.index = index;
			this.autoRun = autoRun;
			this.script = script;
		}
	}
}
