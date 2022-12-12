package luamade.system.module;

import api.network.PacketReadBuffer;
import api.utils.StarRunnable;
import api.utils.game.module.util.SimpleDataStorageMCModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import luamade.gui.ComputerDialog;
import luamade.manager.LuaManager;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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

	@Override
	public void handleRemove(long indexAndOrientation) {
		super.handleRemove(indexAndOrientation);
		long index = ElementCollection.getPosIndexFrom4(indexAndOrientation);
		SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(index);
		if(segmentPiece != null) LuaManager.terminate(segmentPiece);
	}

	@Override
	public void onTagDeserialize(PacketReadBuffer packetReadBuffer) throws IOException {
		if(packetReadBuffer.readBoolean()) {
			String name = packetReadBuffer.readString();
			try {
				Class<?> cls =  Class.forName(name);
				data = packetReadBuffer.readObject(cls);
				new StarRunnable() {
					@Override
					public void run() {
						for(Map.Entry<Long, ComputerData> entry : getComputerMap().entrySet()) {
							SegmentPiece segmentPiece = segmentController.getSegmentBuffer().getPointUnsave(entry.getKey());
							if(segmentPiece != null && getData(segmentPiece) != null && getData(segmentPiece).autoRun) runScript(segmentPiece);
						}
					}
				}.runLater(LuaMade.getInstance(), 100); //Give the game time to finish loading in the entity before running
			} catch(ClassNotFoundException exception) {
				exception.printStackTrace();
			}
		}
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
		if(getComputerMap().containsKey(segmentPiece.getAbsoluteIndex())) return getComputerMap().get(segmentPiece.getAbsoluteIndex());
		else {
			ComputerData computerData = new ComputerData(segmentPiece.getAbsoluteIndex(), false, "");
			getComputerMap().remove(segmentPiece.getAbsoluteIndex());
			getComputerMap().put(segmentPiece.getAbsoluteIndex(), computerData);
			flagUpdatedData();
			return computerData;
		}
	}

	public void setData(SegmentPiece segmentPiece, ComputerData computerData) {
		getComputerMap().remove(segmentPiece.getAbsoluteIndex());
		getComputerMap().put(segmentPiece.getAbsoluteIndex(), computerData);
		flagUpdatedData();
	}

	private ComputerDataMap getComputerMap() {
		ComputerDataMap computerMap = new ComputerDataMap();
		if(data != null) computerMap = (ComputerDataMap) data;
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

	public static class ComputerDataMap extends HashMap<Long, ComputerData> { }

	public static class ComputerData {

		public long index;
		public boolean autoRun;
		public String script;
		public final HashMap<String, Object> variables;
		public String lastOutput;

		public ComputerData(long index, boolean autoRun, String script) {
			this.index = index;
			this.autoRun = autoRun;
			this.script = script;
			this.variables = new HashMap<>();
			this.lastOutput = "";
		}
	}
}
