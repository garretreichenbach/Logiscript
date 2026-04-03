package luamade.lua.peripheral;

import luamade.element.ElementRegistry;
import luamade.lua.element.block.*;
import luamade.system.module.ComputerModule;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementKeyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PeripheralRegistry {
	private static final List<PeripheralProvider> providers = new ArrayList<>();
	private static final Map<String, PeripheralProvider> byName = new HashMap<>();

	/**
	 * Registers a peripheral provider. Type names are matched case-insensitively.
	 * For auto-detection, providers are tried in registration order — register
	 * more specific providers before more general ones (e.g. DiskDrive before Inventory).
	 */
	public static void register(PeripheralProvider provider) {
		providers.add(provider);
		for(String name : provider.getTypeNames()) {
			byName.put(name.toLowerCase(Locale.ROOT), provider);
		}
	}

	/**
	 * Tries each registered provider in order and returns the first match.
	 * Falls back to a plain {@link Block} if no provider claims the piece.
	 */
	public static Block wrapAuto(SegmentPiece piece, ComputerModule module) {
		for(PeripheralProvider provider : providers) {
			if(provider.canWrap(piece)) {
				return provider.wrap(piece, module);
			}
		}
		return new Block(piece, module);
	}

	/**
	 * Wraps a piece as a specific type by name. Returns {@code null} if the
	 * named provider exists but {@link PeripheralProvider#canWrap} returns false,
	 * or if the name is unknown. Passing {@code "auto"} triggers auto-detection;
	 * {@code "block"} / {@code "base"} always return a plain Block.
	 */
	public static Block wrapAs(SegmentPiece piece, String type, ComputerModule module) {
		if(piece == null) return null;
		String key = type == null ? "auto" : type.trim().toLowerCase(Locale.ROOT);

		switch(key) {
			case "auto":
				return wrapAuto(piece, module);
			case "block":
			case "base":
				return new Block(piece, module);
		}

		PeripheralProvider provider = byName.get(key);
		if(provider != null && provider.canWrap(piece)) {
			return provider.wrap(piece, module);
		}
		return null;
	}

	/**
	 * Registers the built-in peripheral providers. Called once from
	 * {@link luamade.LuaMade#onEnable()}. Registration order determines
	 * auto-detection priority.
	 */
	public static void registerDefaults() {
		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"display", "displaymodule", "display_module"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return piece.getType() == ElementKeyMap.TEXT_BOX;
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new DisplayModule(piece);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"diskdrive", "disk_drive", "disk-drive"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return piece.getType() == ElementRegistry.DISK_DRIVE.getId();
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new DiskDriveBlock(piece, module);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"accesspoint", "remoteaccesspoint", "remote_access_point", "remote-access-point"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return piece.getType() == ElementRegistry.REMOTE_ACCESS_POINT.getId();
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new RemoteAccessPointBlock(piece, module);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"factory", "basicfactory", "basic_factory", "standardfactory", "standard_factory", "advancedfactory", "advanced_factory", "microassembler", "micro_assembler", "capsuleassembler", "capsule_assembler"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				short type = piece.getType();
				return type == ElementKeyMap.FACTORY_BASIC_ID
					|| type == ElementKeyMap.FACTORY_STANDARD_ID
					|| type == ElementKeyMap.FACTORY_ADVANCED_ID
					|| type == ElementKeyMap.FACTORY_CAPSULE_ASSEMBLER_ID
					|| type == ElementKeyMap.FACTORY_MICRO_ASSEMBLER_ID;
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new FactoryBlock(piece, module);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"passwordmodule", "password_module", "passwordpermission", "password_permission_module"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return piece.getType() == ElementRegistry.PASSWORD_PERMISSION_MODULE.getId();
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new PasswordPermissionModuleBlock(piece, module);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"datastore", "data_store", "data-store"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return piece.getType() == ElementRegistry.DATA_STORE.getId();
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new DataStoreBlock(piece, module);
			}
		});

		register(new PeripheralProvider() {
			@Override
			public String[] getTypeNames() {
				return new String[]{"inventory"};
			}

			@Override
			public boolean canWrap(SegmentPiece piece) {
				return Block.hasInventoryAt(piece);
			}

			@Override
			public Block wrap(SegmentPiece piece, ComputerModule module) {
				return new InventoryBlock(piece);
			}
		});
	}
}
