package luamade.manager;

import api.common.GameClient;
import luamade.element.ElementRegistry;
import luamade.system.module.ComputerModule;
import org.json.JSONObject;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.inventory.InventorySlot;
import org.schema.schine.graphicsengine.core.MouseEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public final class RemoteSessionManager {
	private static final String REMOTE_COMPUTER_UUID_KEY = "luamadeRemoteComputerUuid";
	private static final String REMOTE_ACCESS_POINT_INDEX_KEY = "luamadeRemoteAccessPointIndex";
	private static final Field SELECTED_SLOT_FIELD = resolveSelectedSlotField();

	private static volatile ComputerModule activeModule;
	private static volatile long activeAccessPointIndex = Long.MIN_VALUE;
	private static volatile PlayerState activePlayerState;
	private static volatile PlayerInteractionControlManager cachedInteractionManager;

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
		if(module == null || !isHeldRemoteStillActive()) {
			disconnect();
			return false;
		}
		module.setTouched();
		module.getInputApi().pushKeyEvent(glfwKey, character, down, shift, ctrl, alt);
		return true;
	}

	public static boolean forwardMouseEvent(MouseEvent mouseEvent) {
		ComputerModule module = activeModule;
		if(module == null || mouseEvent == null || !isHeldRemoteStillActive()) {
			if(module != null) {
				disconnect();
			}
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

	private static boolean isHeldRemoteStillActive() {
		ComputerModule module = activeModule;
		if(module == null) {
			return false;
		}
		PlayerState playerState = activePlayerState;
		if(playerState == null) {
			playerState = GameClient.getClientPlayerState();
		}
		InventorySlot heldSlot = getHeldSlot(playerState);
		if(heldSlot == null || heldSlot.getType() != ElementRegistry.REMOTE_CONTROL.getId() || !heldSlot.hasCustomData()) {
			return false;
		}
		JSONObject customData = heldSlot.getCustomData();
		if(customData == null) {
			return false;
		}
		String linkedComputerUuid = customData.optString(REMOTE_COMPUTER_UUID_KEY, null);
		if(linkedComputerUuid == null || !linkedComputerUuid.equals(module.getUUID())) {
			return false;
		}
		return customData.optLong(REMOTE_ACCESS_POINT_INDEX_KEY, Long.MIN_VALUE) == activeAccessPointIndex;
	}

	private static InventorySlot getHeldSlot(PlayerState playerState) {
		if(playerState == null || SELECTED_SLOT_FIELD == null) {
			return null;
		}
		PlayerInteractionControlManager interactionManager = resolveInteractionManager();
		if(interactionManager == null) {
			return null;
		}
		try {
			int selectedSlot = SELECTED_SLOT_FIELD.getInt(interactionManager);
			if(selectedSlot < 0) {
				return null;
			}
			return playerState.getInventory().getSlot(selectedSlot);
		} catch(IllegalAccessException exception) {
			return null;
		}
	}

	private static PlayerInteractionControlManager resolveInteractionManager() {
		PlayerInteractionControlManager cached = cachedInteractionManager;
		if(cached != null) {
			return cached;
		}
		Object clientState = GameClient.getClientState();
		if(clientState == null) {
			return null;
		}
		PlayerInteractionControlManager resolved = findInteractionManager(clientState, 5, Collections.newSetFromMap(new IdentityHashMap<>()));
		if(resolved != null) {
			cachedInteractionManager = resolved;
		}
		return resolved;
	}

	private static PlayerInteractionControlManager findInteractionManager(Object current, int depth, Set<Object> visited) {
		if(current == null || depth < 0 || visited.contains(current)) {
			return null;
		}
		if(current instanceof PlayerInteractionControlManager) {
			return (PlayerInteractionControlManager) current;
		}
		visited.add(current);
		for(Method method : current.getClass().getMethods()) {
			if(method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE || !method.getName().startsWith("get")) {
				continue;
			}
			Class<?> returnType = method.getReturnType();
			Package returnPackage = returnType.getPackage();
			if(returnType.isPrimitive() || returnType.isEnum() || returnType == String.class || returnPackage == null) {
				continue;
			}
			try {
				Object child = method.invoke(current);
				PlayerInteractionControlManager found = findInteractionManager(child, depth - 1, visited);
				if(found != null) {
					return found;
				}
			} catch(Exception ignored) {
			}
		}
		return null;
	}

	private static Field resolveSelectedSlotField() {
		try {
			Field field = PlayerInteractionControlManager.class.getDeclaredField("selectedSlot");
			field.setAccessible(true);
			return field;
		} catch(Exception exception) {
			return null;
		}
	}
}
