package luamade.lua.element.system.shipyard;

import api.utils.game.SegmentControllerUtils;
import luamade.lua.element.inventory.ItemStack;
import luamade.lua.entity.Entity;
import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.controller.elements.shipyard.ShipyardCollectionManager;
import org.schema.game.common.controller.elements.shipyard.ShipyardElementManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class Shipyard extends LuaMadeUserdata {

	private final SegmentController segmentController;
	private final ShipyardCollectionManager collectionManager;

	public Shipyard(SegmentController segmentController, ShipyardCollectionManager collectionManager) {
		this.segmentController = segmentController;
		this.collectionManager = collectionManager;
	}

	@LuaMadeCallable
	public Boolean isFinished() {
		return isShipyard() && getCollectionManager().isFinished();
	}

	@LuaMadeCallable
	public Double getCompletion() {
		return isShipyard() ? getCollectionManager().getCompletionOrderPercent() : 0;
	}

	@LuaMadeCallable
	public Boolean isDocked() {
		return isShipyard() && getCollectionManager().getCurrentDocked() != null;
	}

	@LuaMadeCallable
	public Boolean isVirtualDocked() {
		return isShipyard() && getCollectionManager().getCurrentDocked() != null && getCollectionManager().getCurrentDocked().isVirtualBlueprint();
	}

	@LuaMadeCallable
	public Entity getDocked() {
		if(isDocked()) return Entity.wrap(getCollectionManager().getCurrentDocked());
		else return null;
	}

	@LuaMadeCallable
	public Boolean canUndock() {
		return isDocked() && !isVirtualDocked();
	}

	@LuaMadeCallable
	public void undock() {
		if(canUndock()) getCollectionManager().undockRequestedFromShipyard();
	}

	@LuaMadeCallable
	public ItemStack[] getRequired() {
		ArrayList<ItemStack> needed = new ArrayList<>();
		if(isShipyard()) {
			for(ElementInformation info : ElementKeyMap.getInfoArray()) {
				if(info != null && !info.isDeprecated()) {
					int count = getCollectionManager().clientGoalTo.get(info.getId());
					if(count > 0) needed.add(new ItemStack(info.getId(), count));
				}
			}
		}
		return needed.toArray(new ItemStack[0]);
	}

	@LuaMadeCallable
	public ItemStack[] getCurrent() {
		ArrayList<ItemStack> current = new ArrayList<>();
		if(isShipyard()) {
			for(ElementInformation info : ElementKeyMap.getInfoArray()) {
				if(info != null && !info.isDeprecated()) {
					int count = getCollectionManager().clientGoalFrom.get(info.getId());
					if(count > 0) current.add(new ItemStack(info.getId(), count));
				}
			}
		}
		return current.toArray(new ItemStack[0]);
	}

	@LuaMadeCallable
	public ItemStack[] getNeeded() {
		ArrayList<ItemStack> needed = new ArrayList<>();
		if(isShipyard()) {
			ItemStack[] required = getRequired();
			ItemStack[] current = getCurrent();
			for(ItemStack stack : required) {
				int count = stack.getCount();
				for(ItemStack stack2 : current) {
					if(Objects.equals(stack2.getId(), stack.getId())) {
						count -= stack2.getCount();
						break;
					}
				}
				if(count > 0) needed.add(new ItemStack(stack.getId(), count));
			}
		}
		return needed.toArray(new ItemStack[0]);
	}

	@LuaMadeCallable
	public void sendCommand(String command, Object... args) {
		if(!isShipyard()) {
			throw new IllegalStateException("Shipyard system is not available on this entity");
		}
		if(!isFinished()) {
			throw new IllegalStateException("Shipyard commands require the current build or refit to be finished");
		}
		if(!getManagedSegmentController().isOnServer()) {
			throw new IllegalStateException("Shipyard commands can only be sent on the server");
		}

		ShipyardCollectionManager.ShipyardCommandType type = resolveCommandType(command);
		Object[] providedArgs = args == null ? new Object[0] : args;
		int expectedArgCount = type.args == null ? 0 : type.args.length;
		if(expectedArgCount != providedArgs.length) {
			throw new IllegalArgumentException("Shipyard command '" + type.name() + "' expects " + expectedArgCount + " args (" + getExpectedArgs(type) + ") but received " + providedArgs.length);
		}

		getCollectionManager().handleShipyardCommandOnServer(getManagedSegmentController().getFactionId(), type, providedArgs);
	}

	private static ShipyardCollectionManager.ShipyardCommandType resolveCommandType(String command) {
		if(command == null || command.trim().isEmpty()) {
			throw new IllegalArgumentException("Shipyard command name is required");
		}

		String normalized = command.trim().toUpperCase(Locale.ENGLISH).replaceAll("\\s+", "_");
		try {
			return ShipyardCollectionManager.ShipyardCommandType.valueOf(normalized);
		} catch(IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unknown shipyard command '" + command + "'. Allowed commands: " + getAllowedCommandNames(), exception);
		}
	}

	private static String getAllowedCommandNames() {
		StringBuilder builder = new StringBuilder();
		for(ShipyardCollectionManager.ShipyardCommandType type : ShipyardCollectionManager.ShipyardCommandType.values()) {
			if(builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(type.name());
		}
		return builder.toString();
	}

	private static String getExpectedArgs(ShipyardCollectionManager.ShipyardCommandType type) {
		if(type.args == null || type.args.length == 0) {
			return "none";
		}

		StringBuilder builder = new StringBuilder();
		for(Class<?> argType : type.args) {
			if(builder.length() > 0) {
				builder.append(", ");
			}
			builder.append(formatArgType(argType));
		}
		return builder.toString();
	}

	private static String formatArgType(Class<?> argType) {
		if(argType == null) {
			return "Object";
		}
		if(argType.isArray()) {
			return formatArgType(argType.getComponentType()) + "[]";
		}
		return argType.getSimpleName();
	}

	private Boolean isShipyard() {
		return getManagedSegmentController() != null && getShipyardElementManager() != null;
	}

	protected ManagedUsableSegmentController<?> getManagedSegmentController() {
		return (ManagedUsableSegmentController<?>) segmentController;
	}

	protected ShipyardElementManager getShipyardElementManager() {
		return SegmentControllerUtils.getElementManager(getManagedSegmentController(), ShipyardElementManager.class);
	}

	protected ShipyardCollectionManager getCollectionManager() {
		return collectionManager;
	}
}
