package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.game.client.controller.PlayerInput;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

public class NetworkedDataStoreDialog extends PlayerInput {

	private final NetworkedDataStorePanel panel;

	public NetworkedDataStoreDialog(String storeUuid, String storeName) {
		super(GameClient.getClientState());
		panel = new NetworkedDataStorePanel(getState(), this, storeUuid, storeName);
	}

	@Override
	public NetworkedDataStorePanel getInputPanel() {
		return panel;
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
		if(!isOccluded() && mouseEvent.pressedLeftMouse()) {
			if(callingElement.getUserPointer() instanceof String) {
				String ptr = (String) callingElement.getUserPointer();
				if("X".equals(ptr) || "CANCEL".equals(ptr) || "OK".equals(ptr)) {
					deactivate();
				}
			}
		}
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
	}

	@Override
	public void onDeactivate() {
	}

	private static final class NetworkedDataStorePanel extends GUIInputDialogPanel {

		private final String storeUuid;

		private NetworkedDataStorePanel(InputState inputState, GUICallback callback, String storeUuid, String storeName) {
			super(inputState, "LUAMADE_NETWORKED_DATA_STORE", "Networked Data Store",
				storeName != null && !storeName.isEmpty() ? "Network Name: " + storeName : "Unregistered",
				620, 420, callback);
			this.storeUuid = storeUuid;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIAncor root = contentPane.getContent(0);

			DataStoreDialog.DataStoreScrollableList list = new DataStoreDialog.DataStoreScrollableList(getState(), 620, 380, root, storeUuid);
			list.onInit();
			root.attach(list);
		}
	}
}
