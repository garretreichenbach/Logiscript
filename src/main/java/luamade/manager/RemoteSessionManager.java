package luamade.manager;

import luamade.system.module.ComputerModule;
import org.schema.game.common.data.player.PlayerState;
import org.schema.schine.graphicsengine.core.MouseEvent;

import java.lang.reflect.Method;

public final class RemoteSessionManager {

	private static volatile ComputerModule activeModule;
	private static volatile long activeAccessPointIndex = Long.MIN_VALUE;
	private static volatile PlayerState activePlayerState;

	private RemoteSessionManager() {
	}

	public static void connect(ComputerModule module, long accessPointIndex) {
		connect(module, accessPointIndex, null);
	}

	public static void connect(ComputerModule module, long accessPointIndex, PlayerState playerState) {
		if(module == null) {
			return;
		}
		activeModule = module;
		activeAccessPointIndex = accessPointIndex;
		if(playerState != null) {
			activePlayerState = playerState;
		}
		module.setTouched();
		module.getInputApi().clear();
		notifyPlayer(activePlayerState, "Remote linked: " + module.getDisplayName() + " (Esc to disconnect)");
	}

	public static void disconnect() {
		ComputerModule module = activeModule;
		PlayerState playerState = activePlayerState;
		activeModule = null;
		activeAccessPointIndex = Long.MIN_VALUE;
		activePlayerState = null;
		if(module != null) {
			module.getInputApi().clear();
			notifyPlayer(playerState, "Remote disconnected: " + module.getDisplayName());
		}
	}

	public static boolean isActive() {
		return activeModule != null;
	}

	public static boolean isActiveFor(ComputerModule module) {
		return module != null && module == activeModule;
	}

	public static long getActiveAccessPointIndex() {
		return activeAccessPointIndex;
	}

	public static ComputerModule getActiveModule() {
		return activeModule;
	}

	public static boolean forwardKeyEvent(int glfwKey, char character, boolean down, boolean shift, boolean ctrl, boolean alt) {
		ComputerModule module = activeModule;
		if(module == null) {
			return false;
		}
		module.setTouched();
		module.getInputApi().pushKeyEvent(glfwKey, character, down, shift, ctrl, alt);
		return true;
	}

	public static boolean forwardMouseEvent(MouseEvent mouseEvent) {
		ComputerModule module = activeModule;
		if(module == null || mouseEvent == null) {
			return false;
		}
		module.setTouched();
		module.getInputApi().pushMouseEvent(
			mouseEvent.button,
			mouseEvent.state,
			mouseEvent.x,
			mouseEvent.y,
			mouseEvent.dx,
			mouseEvent.dy,
			mouseEvent.dWheel,
			-1,
			-1,
			-1,
			-1,
			false,
			false,
			"none"
		);
		return true;
	}

	private static void notifyPlayer(PlayerState playerState, String message) {
		if(playerState == null || message == null || message.isEmpty()) {
			return;
		}
		if(tryInvokeMessageMethod(playerState, "sendServerMessage", message)) {
			return;
		}
		if(tryInvokeMessageMethod(playerState, "sendClientMessage", message)) {
			return;
		}
		tryInvokeMessageMethod(playerState, "sendTextMessage", message);
	}

	private static boolean tryInvokeMessageMethod(PlayerState playerState, String methodName, String message) {
		try {
			Method method = playerState.getClass().getMethod(methodName, String.class);
			method.invoke(playerState, message);
			return true;
		} catch(Exception ignored) {
			return false;
		}
	}
}
