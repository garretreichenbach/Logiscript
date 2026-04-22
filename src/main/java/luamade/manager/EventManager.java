package luamade.manager;

import api.listener.Listener;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.input.MousePressEvent;
import api.listener.events.register.ManagerContainerRegisterEvent;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.gui.ComputerDialog;
import luamade.listener.BlockPublicPermissionListener;
import luamade.listener.JumpTargetListener;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import luamade.system.module.DataStoreModuleContainer;
import luamade.system.module.PasswordPermissionModuleContainer;
import luamade.system.module.VaultModuleContainer;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.input.Keyboard;

public class EventManager {

	public static void registerEvents(LuaMade instance) {
		// Intercept navigation and completion keys so they don't reach TextAreaInput when the
		// ComputerDialog is open. This prevents the caret from moving into protected
		// console output territory and enables proper terminal history navigation.
		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent event) {
				int key = event.getKey();
				char typedChar = event.getChar();
				boolean ctrlDown = isControlDown();
				boolean shiftDown = isShiftDown();
				boolean altDown = isAltDown();

				if(RemoteSessionManager.isActive()) {
					if(key == GLFW.GLFW_KEY_ESCAPE && event.isKeyDown()) {
						RemoteSessionManager.disconnect();
						event.setCanceled(true);
						return;
					}
					if(RemoteSessionManager.forwardKeyEvent(event.getKey(), event.getChar(), event.isKeyDown(), (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT)), (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL)), (Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMETA) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMETA)))) {
						event.setCanceled(true);
						return;
					}
				}

				ComputerDialog.ComputerPanel panel = ComputerDialog.getActivePanel();
				if(panel == null) return;

				if(event.isKeyDown() && key == GLFW.GLFW_KEY_ESCAPE) {
					event.setCanceled(true);
					ComputerDialog.deactivateActiveDialog();
					return;
				}

				if(!panel.isFileEditMode() && isCtrlCPress(event, ctrlDown)) {
					// Ctrl+C is a hard interrupt in terminal mode and must never be consumed by text selection.
					event.setCanceled(true);
					if(ConfigManager.isDebugMode()) {
						instance.logDebug("[INTERRUPT] Ctrl+C detected: key=" + key + ", char=" + (int) typedChar + ", ctrlDown=" + ctrlDown + ", panelMasked=" + panel.isTerminalInputMaskedByGfx());
					}
					ComputerModule ctrlCModule = panel.getComputerModule();
					if(ctrlCModule != null && ctrlCModule.getTerminal() != null) {
						boolean interrupted = ctrlCModule.getTerminal().interruptForeground();
						if(ConfigManager.isDebugMode()) {
							instance.logDebug("[INTERRUPT] interruptForeground result=" + interrupted);
						}
					}
					return;
				}

				ComputerModule module = panel.getComputerModule();
				boolean keyboardConsumed = module != null && module.getInputApi().isKeyboardConsumed();

				if(keyboardConsumed) {
					// Script has exclusive keyboard control: cancel the event so
					// the terminal text bar never receives the keystroke but still
					// forward it to the Lua input queue below.
					event.setCanceled(true);
				} else if(event.isKeyDown()) {
					// Normal mode: intercept editor shortcuts and navigation keys.
					if(panel.isFileEditMode() && ctrlDown && (key == GLFW.GLFW_KEY_S || key == GLFW.GLFW_KEY_X || key == GLFW.GLFW_KEY_R)) {
						event.setCanceled(true);
						panel.handleEditorShortcut(key);
						// still forward to InputApi so scripts can react
					} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_F) {
						event.setCanceled(true);
						panel.handleFindText();
					} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_G) {
						event.setCanceled(true);
						panel.handleGoToLine();
					} else if(panel.isFileEditMode() && ctrlDown && key == GLFW.GLFW_KEY_D) {
						event.setCanceled(true);
						panel.handleDuplicateLine();
					} else if(panel.isFileEditMode() && key == GLFW.GLFW_KEY_TAB) {
						event.setCanceled(true);
						panel.handleTabIndent(shiftDown);
					} else if(panel.isFileEditMode() && key == GLFW.GLFW_KEY_ENTER) {
						event.setCanceled(true);
						panel.handleAutoIndentEnter();
					} else if(!panel.isFileEditMode() && key == GLFW.GLFW_KEY_TAB) {
						event.setCanceled(true);
						panel.handleTabAutocomplete();
						// still forward to InputApi below
					} else if(!panel.isFileEditMode() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
						event.setCanceled(true);
						panel.handleNavigationKey(key);
						// still forward to InputApi below
					}
				}

				// ---- forward every key event (press + release) to Lua InputApi ----
				if(module != null) {
					boolean maskedTerminalInput = panel.isTerminalInputMaskedByGfx();
					boolean enterKey = key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
					if(maskedTerminalInput && enterKey && !module.isMaskedEnterForwardingEnabled()) {
						return;
					}
					module.getInputApi().pushKeyEvent(key, typedChar, event.isKeyDown(), shiftDown, ctrlDown, altDown);
				}
			}
		}, instance);

		StarLoader.registerListener(MousePressEvent.class, new Listener<MousePressEvent>() {
			@Override
			public void onEvent(MousePressEvent event) {
				if(RemoteSessionManager.isActive() && RemoteSessionManager.forwardMouseEvent(event.getRawEvent())) {
					event.setCanceled(true);
					return;
				}

				// Forward mouse events to ComputerDialog if it's active
				ComputerDialog.ComputerPanel panel = ComputerDialog.getActivePanel();
				if(panel != null) {
					panel.pushMouseEvent(event.getRawEvent());
				}
			}
		}, instance);

		BlockPublicPermissionListener.register(instance);
		JumpTargetListener.register(instance);

		StarLoader.registerListener(ManagerContainerRegisterEvent.class, new Listener<ManagerContainerRegisterEvent>() {
			@Override
			public void onEvent(ManagerContainerRegisterEvent event) {
				event.addModMCModule(new ComputerModuleContainer(event.getSegmentController(), event.getContainer()));
				event.addModMCModule(new AccessPointModuleContainer(event.getSegmentController(), event.getContainer()));
				event.addModMCModule(new DataStoreModuleContainer(event.getSegmentController(), event.getContainer()));
				event.addModMCModule(new PasswordPermissionModuleContainer(event.getSegmentController(), event.getContainer()));
				event.addModMCModule(new VaultModuleContainer(event.getSegmentController(), event.getContainer()));
			}
		}, instance);
	}

	private static boolean isControlDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RCONTROL);
	}

	private static boolean isShiftDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RSHIFT);
	}

	private static boolean isAltDown() {
		return Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_LMENU) || Keyboard.isKeyDown(org.lwjgl.input.Keyboard.KEY_RMENU);
	}

	private static boolean isCtrlCPress(KeyPressEvent event, boolean ctrlDown) {
		return ctrlDown && (event.getKey() == GLFW.GLFW_KEY_C || event.getKey() == org.lwjgl.input.Keyboard.KEY_C) && event.isKeyDown();
	}
}
