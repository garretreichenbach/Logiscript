package luamade.system.module;

import api.utils.game.module.util.SimpleDataStorageMCModule;
import luamade.LuaMade;
import luamade.element.ElementManager;
import org.json.JSONObject;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.ManagerContainer;

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

	public ComputerModule getComputerData(long index) {
		JSONObject json = getDataAsJSON();
		if(json.has("computer_" + index)) {
			JSONObject computerJson = json.getJSONObject("computer_" + index);
			String uuid = computerJson.getString("uuid");
			return new ComputerModule(uuid);
		} else {
			String uuid = ComputerModule.generateComputerUUID(index);
			ComputerModule computerModule = new ComputerModule(uuid);
			JSONObject computerJson = new JSONObject();
			computerJson.put("uuid", uuid);
			json.put("computer_" + index, computerJson);
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
