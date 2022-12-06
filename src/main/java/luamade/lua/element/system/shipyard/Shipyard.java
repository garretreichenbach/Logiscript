package luamade.lua.element.system.shipyard;

import api.utils.game.SegmentControllerUtils;
import luamade.lua.element.inventory.ItemStack;
import luamade.lua.entity.RemoteEntity;
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
	public Boolean isShipyard() {
		return getManagedSegmentController() != null && getShipyardElementManager() != null;
	}

	@LuaMadeCallable
	public Boolean isFinished() {
		return isShipyard() && getCollectionManager().isFinished();
	}

	@LuaMadeCallable
	public Double getCompletionPercentage() {
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
	public RemoteEntity getDocked() {
		if(isDocked()) return new RemoteEntity(getCollectionManager().getCurrentDocked());
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
		if(isShipyard() && isFinished() && getManagedSegmentController().isOnServer()) {
			ShipyardCollectionManager.ShipyardCommandType type = ShipyardCollectionManager.ShipyardCommandType.valueOf(command.toUpperCase(Locale.ENGLISH).replaceAll(" ", "_"));
			if(type.args.length == args.length) getCollectionManager().handleShipyardCommandOnServer(getManagedSegmentController().getFactionId(), type, args);
		}
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
