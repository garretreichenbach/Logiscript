package luamade.manager;

import api.event.input.KeyPressListener;
import api.event.input.MousePressListener;
import api.event.lifecycle.ManagerContainerRegisterListener;
import api.event.player.PlayerLeaveWorldListener;
import api.event.render.RegisterWorldDrawersListener;
import api.network.packets.PacketUtil;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.gui.ComputerSessionView;
import luamade.gui.ProjectorWorldDrawer;
import luamade.gui.RemoteControllerDialog;
import luamade.listener.BlockPublicPermissionListener;
import luamade.listener.JumpTargetListener;
import luamade.network.PacketCSRemoteControl;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModuleContainer;
import luamade.system.module.DataStoreModuleContainer;
import luamade.system.module.NetworkModuleContainer;
import luamade.system.module.NetworkedDataStoreModuleContainer;
import luamade.system.module.PasswordPermissionModuleContainer;
import luamade.system.module.ProjectorModuleContainer;
import luamade.system.module.VaultModuleContainer;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.input.Keyboard;
import org.schema.schine.input.KeyboardEvent;

public class EventManager {

	public static void registerEvents(LuaMade instance) {
		// Intercept navigation and completion keys so they don't reach TextAreaInput when the
		// ComputerDialog is open. This prevents the caret from moving into protected
		// console output territory and enables proper terminal history navigation.
		// The listener fires after the game has handled the key and carries no cancel; the
		// setCanceled() calls this used to make were never read by the fire site either.
		KeyPressListener.TYPE.register((event, isServer) -> {
			int key = event.getKey();
			String chars = event.getCharacter();
			char typedChar = (chars != null && !chars.isEmpty()) ? chars.charAt(0) : '\0';
			boolean ctrlDown = isControlDown();
			boolean shiftDown = isShiftDown();
			boolean altDown = isAltDown();

			RemoteControllerDialog controller = RemoteControllerDialog.getActive();
			if(controller != null) {
				if(event.isPressed() && key == GLFW.GLFW_KEY_ESCAPE) {
					controller.deactivate();
				} else {
					controller.forwardKey(key, typedChar, event.isPressed(), shiftDown, ctrlDown, altDown);
				}
				return;
			}

			ComputerDialog.ComputerPanel panel = ComputerDialog.getActivePanel();
			if(panel == null) return;

			if(event.isPressed() && key == GLFW.GLFW_KEY_ESCAPE) {
				ComputerDialog.deactivateActiveDialog();
				return;
			}

			if(!panel.isFileEditMode() && isCtrlCPress(event, ctrlDown)) {
				// Ctrl+C is a hard interrupt in terminal mode and must never be consumed by text selection.
				if(ConfigManager.isDebugMode()) {
					instance.logDebug("[INTERRUPT] Ctrl+C detected: key=" + key + ", char=" + (int) typedChar + ", ctrlDown=" + ctrlDown + ", panelMasked=" + panel.isTerminalInputMaskedByGfx());
				}
				ComputerSessionView ctrlCSession = panel.getSessionView();
				if(ctrlCSession != null) {
					ctrlCSession.sendInterrupt();
				}
				return;
			}

			ComputerSessionView session = panel.getSessionView();
			boolean keyboardConsumed = session != null && session.isKeyboardConsumed();

			if(keyboardConsumed) {
				// Script has exclusive keyboard control: skip terminal shortcuts and just forward
				// the key to the Lua input queue below. This event can't be cancelled; the text
				// bar is kept clean by ComputerPanel's input masking (isTerminalInputMaskingActive).
			} else if(event.isPressed()) {
				// Normal mode: intercept editor shortcuts and navigation keys.
				if(panel.isFileEditMode() && ctrlDown && (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_X || key == GLFW.GLFW_KEY_R)) {
					panel.handleEditorShortcut(key);
					// still forward to InputApi so scripts can react
				} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_F) {
					panel.handleFindText();
				} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_G) {
					panel.handleGoToLine();
				} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_D) {
					panel.handleDuplicateLine();
				} else if(panel.isFileEditMode() && key == GLFW.GLFW_KEY_TAB) {
					panel.handleTabIndent(shiftDown);
				} else if(panel.isFileEditMode() && key == GLFW.GLFW_KEY_ENTER) {
					panel.handleAutoIndentEnter();
				} else if(!panel.isFileEditMode() && key == GLFW.GLFW_KEY_TAB) {
					panel.handleTabAutocomplete();
					// still forward to InputApi below
				} else if(!panel.isFileEditMode() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
					panel.handleNavigationKey(key);
					// still forward to InputApi below
				}
			}

			// ---- forward every key event (press + release) over the network to the server-side InputApi ----
			if(session != null) {
				// NOTE: the per-computer "forward Enter while masked" preference used
				// to gate this and isn't synced client-side in this pass (a narrow,
				// rarely-toggled setting) — Enter is always forwarded now.
				session.sendKeyEvent(key, typedChar, event.isPressed(), shiftDown, ctrlDown, altDown);
			}
		}, instance);

		// Mouse input for open dialogs arrives through their handleMouseEvent; this listener only catches a right-click
		// with a Remote Control in hand while no dialog is open (right-click = terminal, shift+right-click = controller).
		MousePressListener.TYPE.register((event, isServer) -> {
			if(isServer || !event.pressedRightMouse()) return;
			GameClientState client = GameClientState.instance;
			if(client == null || client.getShip() != null || !client.getController().getPlayerInputs().isEmpty()) return;
			int slot = client.getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getPlayerIntercationManager().getSelectedSlot();
			InventorySlot held = slot < 0 ? null : client.getPlayer().getInventory().getSlot(slot);
			if(held != null && held.getType() == ElementRegistry.REMOTE_CONTROL.getId()) {
				PacketUtil.sendPacketToServer(PacketCSRemoteControl.use(slot, isShiftDown()));
			}
		}, instance);

		BlockPublicPermissionListener.register(instance);
		JumpTargetListener.register(instance);
		luamade.listener.CombatEventListener.register(instance);

		ManagerContainerRegisterListener.TYPE.register((event, isServer) -> {
			event.addModMCModule(new ComputerModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new AccessPointModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new DataStoreModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new NetworkModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new NetworkedDataStoreModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new PasswordPermissionModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new VaultModuleContainer(event.getSegmentController(), event.getContainer()));
			event.addModMCModule(new ProjectorModuleContainer(event.getSegmentController(), event.getContainer()));
		}, instance);

		// Render every projector's synced frame in world space each client frame.
		RegisterWorldDrawersListener.TYPE.register((drawer, drawers, isServer) -> drawers.add(new ProjectorWorldDrawer()), instance);

		// Scripts execute server-side now, streaming console/gfx output to whoever
		// is viewing; a disconnected player's viewer entry would otherwise linger
		// forever (only ever cleaned up if the entity itself unloads).
		PlayerLeaveWorldListener.TYPE.register((playerName, factionId, sector, isServer) -> ComputerModuleContainer.removeViewerByPlayerName(playerName), instance);
	}

	private static boolean isControlDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
	}

	private static boolean isShiftDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
	}

	private static boolean isAltDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);
	}

	private static boolean isCtrlCPress(KeyboardEvent event, boolean ctrlDown) {
		return ctrlDown && (event.getKey() == GLFW.GLFW_KEY_C) && event.isPressed();
	}
}
