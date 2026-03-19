package luamade.manager;

import api.listener.Listener;
import api.listener.events.Event;
import api.listener.events.block.SegmentPieceAddByMetadataEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.input.MousePressEvent;
import api.listener.events.register.ManagerContainerRegisterEvent;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.listener.SegmentPieceListener;
import luamade.system.module.AccessPointModuleContainer;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.input.Keyboard;

import java.lang.reflect.Method;

public class EventManager {

	private static final SegmentPieceListener segmentPieceListener = new SegmentPieceListener();

	public static void registerEvents(LuaMade instance) {
		FastListenerCommon.segmentPiecePlayerInteractListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceAddListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceRemoveListeners.add(segmentPieceListener);
		FastListenerCommon.segmentPieceKilledListeners.add(segmentPieceListener);

		// Intercept arrow/home/end keys so they don't reach TextAreaInput when the
		// ComputerDialog is open. This prevents the caret from moving into protected
		// console output territory and enables proper terminal history navigation.
		StarLoader.registerListener(KeyPressEvent.class, new Listener<KeyPressEvent>() {
			@Override
			public void onEvent(KeyPressEvent event) {
				int key = event.getKey();
				boolean ctrlDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
				boolean shiftDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
				boolean altDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);

				if(RemoteSessionManager.isActive()) {
					if(key == GLFW.GLFW_KEY_ESCAPE && event.isKeyDown()) {
						RemoteSessionManager.disconnect();
						event.setCanceled(true);
						return;
					}
					RemoteSessionManager.forwardKeyEvent(key, event.getChar().isEmpty() ? 0 : event.getChar().charAt(0), event.isKeyDown(), shiftDown, ctrlDown, altDown);
					event.setCanceled(true);
					return;
				}

				ComputerDialog.ComputerPanel panel = ComputerDialog.getActivePanel();
				if(panel == null) return;

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
					} else if(!panel.isFileEditMode() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT || key == GLFW.GLFW_KEY_HOME || key == GLFW.GLFW_KEY_END)) {
						event.setCanceled(true);
						panel.handleNavigationKey(key);
						// still forward to InputApi below
					}
				}

				// ---- forward every key event (press + release) to Lua InputApi ----
				if(module != null) {
					module.getInputApi().pushKeyEvent(
							key,
							event.getChar().isEmpty() ? 0 : event.getChar().charAt(0),
							event.isKeyDown(),
							shiftDown,
							ctrlDown,
							altDown
					);
				}
			}
		}, instance);

		StarLoader.registerListener(MousePressEvent.class, new Listener<MousePressEvent>() {
			@Override
			public void onEvent(MousePressEvent event) {
				if(RemoteSessionManager.isActive() && RemoteSessionManager.forwardMouseEvent(event.getRawEvent())) {
					event.setCanceled(true);
				}
			}
		}, instance);

		registerOptionalMouseMoveListener(instance);

		StarLoader.registerListener(SegmentPieceAddByMetadataEvent.class, new Listener<SegmentPieceAddByMetadataEvent>() {
			@Override
			public void onEvent(SegmentPieceAddByMetadataEvent event) {
				SegmentPiece segmentPiece = event.getSegment().getSegmentController().getSegmentBuffer().getPointUnsave(ElementCollection.getIndex(event.getX(), event.getY(), event.getZ()));
				if(segmentPiece != null) {
					if(event.getSegment().getSegmentController() instanceof ManagedUsableSegmentController<?>) {
						if(segmentPiece.getType() == ElementRegistry.COMPUTER.getId()) {
							ComputerModuleContainer container = ComputerModuleContainer.getContainer(((ManagedUsableSegmentController<?>) event.getSegment().getSegmentController()).getManagerContainer());
							if(container != null) {
								container.addModule(segmentPiece);
							}
						}
					}
				}
			}
		}, instance);

		StarLoader.registerListener(ManagerContainerRegisterEvent.class, new Listener<ManagerContainerRegisterEvent>() {
			@Override
			public void onEvent(ManagerContainerRegisterEvent event) {
				event.addModMCModule(new ComputerModuleContainer(event.getSegmentController(), event.getContainer()));
				event.addModMCModule(new AccessPointModuleContainer(event.getSegmentController(), event.getContainer()));
			}
		}, instance);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private static void registerOptionalMouseMoveListener(LuaMade instance) {
		try {
			Class<?> mouseMoveEventClass = Class.forName("api.listener.events.input.MouseMoveEvent");
			StarLoader.registerListener((Class) mouseMoveEventClass, new Listener() {
				@Override
				public void onEvent(Event event) {
					if(!RemoteSessionManager.isActive()) {
						return;
					}
					MouseEvent rawMouseEvent = extractRawMouseEvent(event);
					if(rawMouseEvent != null && RemoteSessionManager.forwardMouseEvent(rawMouseEvent)) {
						event.setCanceled(true);
					}
				}
			}, instance);
		} catch(ClassNotFoundException ignored) {
			// Runtime API may not expose MouseMoveEvent; MousePressEvent forwarding remains active.
		}
	}

	private static MouseEvent extractRawMouseEvent(Object event) {
		if(event == null) {
			return null;
		}
		try {
			Method rawEventMethod = event.getClass().getMethod("getRawEvent");
			Object raw = rawEventMethod.invoke(event);
			if(raw instanceof MouseEvent) {
				return (MouseEvent) raw;
			}
		} catch(Exception ignored) {
		}
		return null;
	}
}
