package luamade.gui;

import api.common.GameClient;
import org.schema.game.client.controller.PlayerInput;
import org.schema.game.client.data.GameClientState;
import org.schema.schine.graphicsengine.camera.Camera;
import org.schema.schine.graphicsengine.core.Controller;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;

/**
 * Remote Control "controller mode". Holding an open dialog stops the game moving the player while every key and mouse
 * event goes to the linked computer's {@code input} queue; the computer's own UI stays closed. When the computer's
 * scripts pick a Camera block ({@code console.setCamera}), the view switches to it with a CCTV overlay, or shows
 * "NO SIGNAL" while that camera's entity isn't loaded on this client. Esc ends it and restores the player's camera.
 */
public class RemoteControllerDialog extends PlayerInput {

	private static volatile RemoteControllerDialog active;

	private final ComputerSessionView session;
	private final CctvOverlay overlay;
	private RemoteFeedCamera feedCamera;
	private Camera playerCamera;
	private boolean leftDown;
	private boolean rightDown;
	private boolean middleDown;

	private RemoteControllerDialog(ComputerSessionView session, String computerName) {
		super(GameClient.getClientState());
		this.session = session;
		overlay = new CctvOverlay(getState(), computerName);
		overlay.onInit();
	}

	public static void open(int entityId, long absIndex, String computerName) {
		if(active != null) {
			active.deactivate();
		}
		new RemoteControllerDialog(ComputerSessionRegistry.getOrCreate(entityId, absIndex), computerName).activate();
	}

	/** The open controller session, or null. Key events are routed here from EventManager's key listener. */
	public static RemoteControllerDialog getActive() {
		return active;
	}

	@Override
	public void activate() {
		super.activate();
		active = this;
		session.sendConnect();
	}

	@Override
	public GUIElement getInputPanel() {
		return overlay;
	}

	@Override
	public void update(Timer timer) {
		super.update(timer);
		if(session.getCameraEntityId() < 0) {
			restorePlayerCamera();
			overlay.setMode(CctvOverlay.Mode.STATUS);
			return;
		}
		if(feedCamera == null) {
			feedCamera = new RemoteFeedCamera(GameClientState.instance);
		}
		feedCamera.aim(session.getCameraEntityId(), session.getCameraAbsIndex());
		if(feedCamera.refresh()) {
			if(Controller.getCamera() != feedCamera) {
				playerCamera = Controller.getCamera();
				Controller.setCamera(feedCamera);
			}
			overlay.setMode(CctvOverlay.Mode.LIVE);
		} else {
			restorePlayerCamera();
			overlay.setMode(CctvOverlay.Mode.NO_SIGNAL);
		}
	}

	private void restorePlayerCamera() {
		if(playerCamera != null && Controller.getCamera() == feedCamera) {
			Controller.setCamera(playerCamera);
		}
		playerCamera = null;
	}

	public void forwardKey(int key, char character, boolean down, boolean shift, boolean ctrl, boolean alt) {
		session.sendKeyEvent(key, character, down, shift, ctrl, alt);
	}

	@Override
	public void handleMouseEvent(MouseEvent mouseEvent) {
		int button = mouseEvent.key;
		boolean pressed = mouseEvent.isPressed();
		if(button < 0 && mouseEvent.dWheel == 0 && mouseEvent.dx == 0 && mouseEvent.dy == 0) {
			return;
		}
		if(button == 0) leftDown = pressed;
		else if(button == 1) rightDown = pressed;
		else if(button == 2) middleDown = pressed;
		String dragButton = leftDown ? "left" : rightDown ? "right" : middleDown ? "middle" : "none";
		session.sendMouseEvent(button, pressed, mouseEvent.x, mouseEvent.y, mouseEvent.dx, mouseEvent.dy, mouseEvent.dWheel, -1, -1, false, !"none".equals(dragButton), dragButton);
	}

	@Override
	public void callback(GUIElement callingElement, MouseEvent mouseEvent) {
	}

	@Override
	public void onDeactivate() {
		restorePlayerCamera();
		if(active == this) {
			active = null;
		}
		session.sendDisconnect();
		ComputerSessionRegistry.forget(session.getEntityId(), session.getAbsIndex());
	}
}
