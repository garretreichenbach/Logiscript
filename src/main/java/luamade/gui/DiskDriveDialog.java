package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import org.schema.game.client.view.gui.inventory.SingleInventorySlotIcon;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.inventory.Inventory;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUITextOverlay;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIDialogWindow;
import org.schema.schine.input.InputState;

import java.lang.reflect.Constructor;

public class DiskDriveDialog extends PlayerInput {

	private final DiskDrivePanel panel;

	public DiskDriveDialog(Inventory inventory) {
		super(GameClient.getClientState());
		panel = new DiskDrivePanel(getState(), this, inventory);
	}

	@Override
	public DiskDrivePanel getInputPanel() {
		return panel;
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
		if(!isOccluded() && mouseEvent.pressedLeftMouse()) {
			if(callingElement.getUserPointer() instanceof String) {
				String userPointer = (String) callingElement.getUserPointer();
				if("X".equals(userPointer) || "CANCEL".equals(userPointer) || "OK".equals(userPointer)) {
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

	public static boolean isSingleSlotUiAvailable() {
		for(Constructor<?> constructor : SingleInventorySlotIcon.class.getConstructors()) {
			Class<?>[] parameterTypes = constructor.getParameterTypes();
			if(parameterTypes.length >= 3
				&& GameClientState.class.isAssignableFrom(parameterTypes[0])
				&& Inventory.class.isAssignableFrom(parameterTypes[1])) {
				return true;
			}
		}
		return false;
	}

	private static final class DiskDrivePanel extends GUIInputDialogPanel {

		private final Inventory inventory;

		private DiskDrivePanel(InputState inputState, GUICallback callback, Inventory inventory) {
			super(inputState, "LUAMADE_DISK_DRIVE", "Disk Drive", "", 480, 240, callback);
			this.inventory = inventory;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane mainContentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIAncor root = mainContentPane.getContent(0);

			if(!attachSingleSlotWidget(root)) {
				attachFallbackMessage(root, "SingleInventorySlotIcon unavailable in this runtime.");
			}
		}

		private boolean attachSingleSlotWidget(GUIAncor root) {
			if(inventory == null) {
				attachFallbackMessage(root, "No backing inventory found for this disk drive block.");
				return false;
			}

			try {
				SingleInventorySlotIcon slotWidget = new SingleInventorySlotIcon(GameClient.getClientState(), inventory, 0, "Disk");
				slotWidget.setPos(48, 64, 0);
				slotWidget.onInit();
				root.attach(slotWidget);

				attachHint(root, "Insert a LuaMade Disk item into this slot");
				return true;
			} catch(Exception exception) {
				attachFallbackMessage(root, "Disk drive UI not available: " + exception.getClass().getSimpleName());
				return false;
			}
		}

		private void attachHint(GUIAncor root, String text) {
			GUITextOverlay overlay = new GUITextOverlay(320, 20, org.schema.schine.graphicsengine.forms.font.FontLibrary.FontSize.MEDIUM, getState());
			overlay.setTextSimple(text);
			overlay.setPos(48, 128, 0);
			overlay.onInit();
			root.attach(overlay);
		}

		private void attachFallbackMessage(GUIAncor root, String message) {
			GUITextOverlay overlay = new GUITextOverlay(430, 80, org.schema.schine.graphicsengine.forms.font.FontLibrary.FontSize.MEDIUM, getState());
			overlay.setTextSimple(message + "\nUse the default inventory window as a fallback.");
			overlay.setPos(24, 72, 0);
			overlay.onInit();
			root.attach(overlay);
		}
	}
}
