package luamade.system.module;

import api.utils.game.module.util.SimpleDataStorageMCModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import org.json.JSONObject;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;
import org.schema.game.common.data.SegmentPiece;

/**
 * Container module for the computer data.
 */
public class ComputerModuleContainer extends SimpleDataStorageMCModule {

	private final byte VERSION = 1;

	public ComputerModuleContainer(SegmentController ship, ManagerContainer<?> managerContainer) {
		super(ship, managerContainer, LuaMade.getInstance(), ElementManager.getBlock("Computer").getId());
		checkData();
	}

	@Override
	public String getName() {
		return "Computer";
	}

	public ComputerModule getComputerData(SegmentPiece segmentPiece) {
		JSONObject json = getDataAsJSON();
		if(json.has("computer_" + segmentPiece.getAbsoluteIndex())) {
			JSONObject computerJson = json.getJSONObject("computer_" + segmentPiece.getAbsoluteIndex());
			String uuid = computerJson.getString("uuid");
			return new ComputerModule(segmentPiece, uuid);
		} else {
			String uuid = ComputerModule.generateComputerUUID(segmentPiece.getAbsoluteIndex());
			ComputerModule computerModule = new ComputerModule(segmentPiece, uuid);
			JSONObject computerJson = new JSONObject();
			computerJson.put("uuid", uuid);
			json.put("computer_" + segmentPiece.getAbsoluteIndex(), computerJson);
			data = json.toString();
			flagUpdatedData();
			return computerModule;
		}
	}

	public JSONObject getDataAsJSON() {
		checkData();
		return new JSONObject((String) data);
	}

	public void checkData() {
		if(!(data instanceof String) || ((String) data).isEmpty()) {
			JSONObject json = new JSONObject();
			json.put("version", VERSION);
			data = json.toString();
			flagUpdatedData();
		}
	}
}
