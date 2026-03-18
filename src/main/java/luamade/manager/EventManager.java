package luamade.manager;

import api.listener.Listener;
import api.listener.events.block.SegmentPieceAddByMetadataEvent;
import api.listener.events.input.KeyPressEvent;
import api.listener.events.register.ManagerContainerRegisterEvent;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.element.ElementRegistry;
import luamade.gui.ComputerDialog;
import luamade.listener.SegmentPieceListener;
import luamade.system.module.ComputerModule;
import luamade.system.module.ComputerModuleContainer;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.schine.graphicsengine.core.GLFW;
import org.schema.schine.input.Keyboard;

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
				ComputerDialog.ComputerPanel panel = ComputerDialog.getActivePanel();
				if(panel == null) return;

				int key = event.getKey();
				boolean ctrlDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_CONTROL);
				boolean shiftDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
				boolean altDown = Keyboard.isKeyDown(GLFW.GLFW_KEY_LEFT_ALT) || Keyboard.isKeyDown(GLFW.GLFW_KEY_RIGHT_ALT);

				// ---- existing shortcut / navigation interception (key-down only) ----
				if(event.isKeyDown()) {
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
				ComputerModule module = panel.getComputerModule();
				if(module != null) {
					module.getInputApi().pushKeyEvent(
							key,
							event.getChar(),
							event.isKeyDown(),
							shiftDown,
							ctrlDown,
							altDown
					);
				}
			}
		}, instance);

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
			}
		}, instance);
	}
}
