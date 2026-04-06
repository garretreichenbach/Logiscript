package luamade.gui;

import api.common.GameClient;
import api.utils.gui.GUIInputDialogPanel;
import luamade.network.ClientDataStoreCache;
import org.schema.game.client.controller.PlayerInput;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIAncor;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.GUIElementList;
import org.schema.schine.graphicsengine.forms.gui.newgui.*;
import org.schema.schine.input.InputState;

import java.util.*;

public class DataStoreDialog extends PlayerInput {

	private final DataStorePanel panel;

	public DataStoreDialog(String storeUuid) {
		super(GameClient.getClientState());
		panel = new DataStorePanel(getState(), this, storeUuid);
	}

	@Override
	public DataStorePanel getInputPanel() {
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

	static final class DataStorePanel extends GUIInputDialogPanel {

		private final String storeUuid;

		DataStorePanel(InputState inputState, GUICallback callback, String storeUuid) {
			super(inputState, "LUAMADE_DATA_STORE", "Data Store", "Stored Key-Value Pairs", 620, 420, callback);
			this.storeUuid = storeUuid;
			setCancelButton(false);
			setOkButton(false);
		}

		@Override
		public void onInit() {
			super.onInit();
			GUIContentPane contentPane = ((GUIDialogWindow) background).getMainContentPane();
			GUIAncor root = contentPane.getContent(0);

			DataStoreScrollableList list = new DataStoreScrollableList(getState(), 620, 380, root, storeUuid);
			list.onInit();
			root.attach(list);
		}
	}

	public static final class DataStoreScrollableList extends ScrollableTableList<Map.Entry<String, String>> {

		private final String storeUuid;

		public DataStoreScrollableList(InputState state, float width, float height, GUIElement parent, String storeUuid) {
			super(state, width, height, parent);
			this.storeUuid = storeUuid;
		}

		@Override
		public void initColumns() {
			addColumn("Key", 3.5f, Map.Entry.comparingByKey());
			addColumn("Value", 8.0f, Map.Entry.comparingByValue());

			addTextFilter(new GUIListFilterText<Map.Entry<String, String>>() {
				@Override
				public boolean isOk(String s, Map.Entry<String, String> entry) {
					String lower = s.toLowerCase();
					return entry.getKey().toLowerCase().contains(lower) || entry.getValue().toLowerCase().contains(lower);
				}
			}, ControllerElement.FilterRowStyle.FULL);

			activeSortColumnIndex = 0;
		}

		@Override
		protected Collection<Map.Entry<String, String>> getElementList() {
			Map<String, String> cached = ClientDataStoreCache.get(storeUuid);
			List<Map.Entry<String, String>> entries = new ArrayList<>(cached.size());
			for(Map.Entry<String, String> e : cached.entrySet()) {
				entries.add(new AbstractMap.SimpleImmutableEntry<>(e.getKey(), e.getValue()));
			}
			return entries;
		}

		@Override
		public void updateListEntries(GUIElementList guiElementList, Set<Map.Entry<String, String>> set) {
			setColumnHeight(28);
			for(Map.Entry<String, String> entry : set) {
				GUITextOverlayTable keyText = new GUITextOverlayTable(10, 10, getState());
				keyText.setTextSimple("  " + entry.getKey());
				GUIClippedRow keyRow = new GUIClippedRow(getState());
				keyRow.attach(keyText);
				keyText.autoWrapOn = keyRow;
				keyText.autoHeight = true;

				GUITextOverlayTable valueText = new GUITextOverlayTable(10, 10, getState());
				valueText.setTextSimple("  " + entry.getValue());
				GUIClippedRow valueRow = new GUIClippedRow(getState());
				valueRow.attach(valueText);
				valueText.autoWrapOn = valueRow;
				valueText.autoHeight = true;

				DataStoreRow row = new DataStoreRow(getState(), entry, keyRow, valueRow);
				row.onInit();
				guiElementList.addWithoutUpdate(row);
			}
			guiElementList.updateDim();
		}

		public class DataStoreRow extends ScrollableTableList<Map.Entry<String, String>>.Row {
			public DataStoreRow(InputState state, Map.Entry<String, String> entry, GUIElement... elements) {
				super(state, entry, elements);
				highlightSelect = true;
				highlightSelectSimple = true;
			}
		}
	}
}
