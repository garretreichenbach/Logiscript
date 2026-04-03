package luamade.element;

import api.config.BlockConfig;
import api.listener.fastevents.FastListenerCommon;
import api.listener.fastevents.segmentpiece.*;
import luamade.LuaMade;
import luamade.element.block.Computer;
import luamade.element.block.DiskDrive;
import luamade.element.block.RemoteAccessPoint;
import luamade.element.item.Disk;
import luamade.element.item.RemoteControl;
import org.schema.game.common.data.element.ElementInformation;


/**
 * Central registry that defines and registers all mod blocks/chambers/weapons/etc. by implementing the ElementInterface and adding them to the enum.
 */
public enum ElementRegistry {

	COMPUTER(new Computer()),
	DISK_DRIVE(new DiskDrive()),
	REMOTE_ACCESS_POINT(new RemoteAccessPoint()),
	DISK(new Disk()),
	REMOTE_CONTROL(new RemoteControl());

	public final ElementInterface elementInterface;

	ElementRegistry(ElementInterface elementInterface) {
		this.elementInterface = elementInterface;
	}

	public static void registerElements() {
		for(ElementRegistry registry : values()) {
			registry.elementInterface.initData();
		}
		LuaMade.getInstance().logDebug("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.postInitData();
		}
		LuaMade.getInstance().logDebug("Initialized element data for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			registry.elementInterface.initResources();
		}
		LuaMade.getInstance().logDebug("Initialized element resources for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			BlockConfig.add(registry.getInfo());
		}
		LuaMade.getInstance().logDebug("Initialized element resources for " + values().length + " elements");

		for(ElementRegistry registry : values()) {
			if(registry.elementInterface instanceof SegmentPieceAddListener) {
				FastListenerCommon.segmentPieceAddListeners.add((SegmentPieceAddListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPieceAddByMetadataListener) {
				FastListenerCommon.segmentPieceAddByMetadataListeners.add((SegmentPieceAddByMetadataListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPieceRemoveListener) {
				FastListenerCommon.segmentPieceRemoveListeners.add((SegmentPieceRemoveListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPieceKilledListener) {
				FastListenerCommon.segmentPieceKilledListeners.add((SegmentPieceKilledListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPiecePlayerInteractListener) {
				FastListenerCommon.segmentPiecePlayerInteractListeners.add((SegmentPiecePlayerInteractListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPieceConsoleInteractListener) {
				FastListenerCommon.consoleInteractListeners.add((SegmentPieceConsoleInteractListener) registry.elementInterface);
			}
			if(registry.elementInterface instanceof SegmentPieceActivateListener) {
				FastListenerCommon.segmentPieceActivateListeners.add((SegmentPieceActivateListener) registry.elementInterface);
			}
		}
		LuaMade.getInstance().logDebug("Registered event listeners for " + values().length + " elements");
	}

	private static ElementInformation getInfoByName(String name) {
		for(ElementInformation info : BlockConfig.getElements()) {
			if(info.getName().equals(name)) {
				return info;
			}
		}
		throw new IllegalStateException("Element with name '" + name + "' not found");
	}

	public ElementInformation getInfo() {
		return elementInterface.getInfo();
	}

	public short getId() {
		return getInfo().id;
	}
}