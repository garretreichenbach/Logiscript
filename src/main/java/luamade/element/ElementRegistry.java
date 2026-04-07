package luamade.element;

import api.config.BlockConfig;
import api.listener.fastevents.FastListenerCommon;
import api.listener.fastevents.segmentpiece.*;
import api.mod.StarLoader;
import luamade.LuaMade;
import luamade.element.block.Computer;
import luamade.element.block.DataStore;
import luamade.element.block.DiskDrive;
import luamade.element.block.NetworkedDataStore;
import luamade.element.block.PasswordPermissionModule;
import luamade.element.block.RemoteAccessPoint;
import luamade.element.item.Disk;
import luamade.element.item.RemoteControl;
import org.schema.game.common.data.element.ElementInformation;

import java.util.Locale;
import java.util.Objects;


/**
 * Central registry that defines and registers all mod blocks/chambers/weapons/etc. by implementing the ElementInterface and adding them to the enum.
 */
public enum ElementRegistry {

	COMPUTER(new Computer()),
	DISK_DRIVE(new DiskDrive()),
	REMOTE_ACCESS_POINT(new RemoteAccessPoint()),
	DATA_STORE(new DataStore()),
	NETWORKED_DATA_STORE(new NetworkedDataStore()),
	PASSWORD_PERMISSION_MODULE(new PasswordPermissionModule()),
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
			if(info.getName().toLowerCase(Locale.ENGLISH).endsWith(name.toLowerCase(Locale.ENGLISH))) {
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

	//RRS Stuff
	public static boolean isRRSInstalled() {
		return StarLoader.getModFromName("Resources ReSourced") != null && Objects.requireNonNull(StarLoader.getModFromName("Resources ReSourced")).isEnabled();
	}

	public enum RRSElements {
		//Factories
		VAPOR_SIPHON("Vapor Siphon"),
		MAGMATIC_EXTRACTOR("Magmatic Extractor"),
		COMPONENT_FABRICATOR("Component Fabricator"),
		BLOCK_ASSEMBLER("Block Assembler"),
		BLOCK_RECYCLER("Block Recycler"),
		COMPONENT_RECYCLER("Component Recycler"),

		//Resources
		METALLIC_ORE("Metallic Ore"),
		CRYSTALLINE_ORE("Crystalline Ore"),
		FERRON_CORE("Ferron Ore"),
		AEGIUM_ORE("Aegium Ore"),
		THERMYN_AMALGAM("Thermyn Amalgam"),
		PARSYNE_PLASMA("Parsyne Plasma"),
		ANBARIC_VAPOR("Anbaric Vapor"),
		MACETINE_AGGREGATE("Macetine Aggregate"),
		THRENS_RELIC("Threns Relic"),

		//Capsules
		FERRON_CAPSULE("Ferron Capsule"),
		AEGIUM_CAPSULE("Aegium Capsule"),
		THERMYN_CAPSULE("Thermyn Capsule"),
		PARSYNE_CAPSULE("Parsyne Capsule"),
		ANBARIC_CAPSULE("Anbaric Capsule"),
		MACETINE_CAPSULE("Macetine Capsule"),
		THRENS_CAPSULE("Threns Capsule"),
		FLORAL_PROTOMATERIAL_CAPSULE("Floral Protomaterial Capsule"),
		ROCK_DUST_CAPSULE("Rock Dust Capsule"),

		//Components
		OMNICHROME_PAINT("Component: Omnichrome Paint"),
		CRYSTAL_PANEL("Component: Crystal Panel"),
		CRYSTAL_LIGHT_SOURCE("Component: Crystal Light Source"),
		CRYSTAL_ENERGY_FOCUS("Component: Crystal Energy Focus"),
		STANDARD_CIRCUITRY("Component: Standard Circuitry"),
		ENERGY_CELL("Component: Energy Cell"),
		METAL_FRAME("Component: Metal Frame"),
		METAL_SHEET("Component: Metal Sheet"),
		PARSYNE_AMPLIFYING_FOCUS("Component: Parsyne Amplifying Focus"),
		PARSYNE_HOLOGRAPHIC_PROCESSOR("Component: Parsyne Holographic Processor"),
		MACETINE_MAGNETIC_RAIL("Component: Macetine Magnetic Rail"),
		FERRON_RESONANT_CIRCUITRY("Component: Ferron Resonant Circuitry"),
		AEGIUM_FIELD_EMITTER("Component: Aegium Field Emitter"),
		ANBARIC_DISTORTION_COIL("Component: Anbaric Distortion Coil"),
		THERMYN_POWER_CHARGER("Component: Thermyn Power Charger"),
		THRENS_WIRE_MATRIX("Component: Threns Wire Matrix"),
		PRISMATIC_CIRCUIT("Component: Prismatic Circuit");

		public final String name;

		private ElementInformation info;

		RRSElements(String name) {
			this.name = name;
		}

		public ElementInformation getInfo() {
			if(info == null) {
				info = getInfoByName(name);
			}
			return info;
		}

		public short getId() {
			if(getInfo() == null) {
				return -1;
			}
			return getInfo().id;
		}
	}
}