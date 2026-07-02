package luamade;

import api.config.BlockConfig;
import api.mod.StarMod;
import api.network.Packet;
import luamade.element.ElementRegistry;
import luamade.network.PacketCSClipboardImport;
import luamade.network.PacketCSComputerInput;
import luamade.network.PacketCSFileRead;
import luamade.network.PacketCSFileWrite;
import luamade.network.PacketCSPlayerDialogResponse;
import luamade.network.PacketCSRequestDataStoreContents;
import luamade.network.PacketCSRequestVaultView;
import luamade.network.PacketCSVaultDeposit;
import luamade.network.PacketCSVaultScriptOp;
import luamade.network.PacketCSVaultWithdraw;
import luamade.network.PacketSCComputerConnectAck;
import luamade.network.PacketSCConsoleSnapshot;
import luamade.network.PacketSCDataStoreContents;
import luamade.network.PacketSCFileContents;
import luamade.network.PacketSCFileResult;
import luamade.network.PacketSCGfxSnapshot;
import luamade.network.PacketSCOpenSwingEditor;
import luamade.network.PacketSCPlayerDialogRequest;
import luamade.network.PacketSCVaultScriptResponse;
import luamade.network.PacketSCVaultView;
import luamade.lua.peripheral.PeripheralRegistry;
import luamade.lua.datastore.NetworkedDataStoreRegistry;
import luamade.lua.datastore.SharedDataStore;
import luamade.lua.vault.SharedVaultLedger;
import luamade.manager.ComputerDataCleanupManager;
import luamade.manager.ConfigManager;
import luamade.manager.EventManager;
import luamade.manager.ResourceManager;
import luamade.system.module.ComputerModuleContainer;
import org.schema.schine.resource.ResourceLoader;

import java.util.Set;

public class LuaMade extends StarMod {

	//Instance
	private static LuaMade instance;

	public LuaMade() {
		instance = this;
	}

	public static LuaMade getInstance() {
		return instance;
	}

	public static void main(String[] args) {
	}

	@Override
	public void onEnable() {
		instance = this;
		PeripheralRegistry.registerDefaults();
		ConfigManager.initialize(this);
		EventManager.registerEvents(this);
		NetworkedDataStoreRegistry.load();
		registerPackets();
	}

	@Override
	public void onDisable() {
		try {
			Set<String> protectedComputerUUIDs = ComputerModuleContainer.snapshotActiveComputerUUIDs();
			ComputerModuleContainer.saveAndCleanupAll();
			ComputerDataCleanupManager.cleanupOrphanedComputerData(protectedComputerUUIDs);
		} catch(Exception exception) {
			logException("Failed to save computer data on disable", exception);
		}
		try {
			SharedDataStore.saveAll();
		} catch(Exception exception) {
			logException("Failed to save data store state on disable", exception);
		}
		try {
			NetworkedDataStoreRegistry.saveAll();
		} catch(Exception exception) {
			logException("Failed to save networked data store registry on disable", exception);
		}
		try {
			SharedVaultLedger.saveAll();
		} catch(Exception exception) {
			logException("Failed to save vault ledger on disable", exception);
		}
		super.onDisable();
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources(loader);
	}

	public void logDebug(String message) {
		if(ConfigManager.isDebugMode()) {
			logMessage("[DEBUG]: [ResourcesReorganized] " + message);
		}
	}

	private void registerPackets() {
		Packet.registerPacket(PacketCSRequestDataStoreContents.class);
		Packet.registerPacket(PacketSCDataStoreContents.class);
		Packet.registerPacket(PacketCSRequestVaultView.class);
		Packet.registerPacket(PacketSCVaultView.class);
		Packet.registerPacket(PacketCSVaultDeposit.class);
		Packet.registerPacket(PacketCSVaultWithdraw.class);
		Packet.registerPacket(PacketCSVaultScriptOp.class);
		Packet.registerPacket(PacketSCVaultScriptResponse.class);

		// Computer session (scripts execute server-side; these carry input to
		// the server and stream console/gfx output back to viewers).
		Packet.registerPacket(PacketCSComputerInput.class);
		Packet.registerPacket(PacketSCComputerConnectAck.class);
		Packet.registerPacket(PacketSCConsoleSnapshot.class);
		Packet.registerPacket(PacketSCGfxSnapshot.class);
		Packet.registerPacket(PacketCSFileRead.class);
		Packet.registerPacket(PacketSCFileContents.class);
		Packet.registerPacket(PacketCSFileWrite.class);
		Packet.registerPacket(PacketSCFileResult.class);
		Packet.registerPacket(PacketCSClipboardImport.class);
		Packet.registerPacket(PacketSCOpenSwingEditor.class);
		Packet.registerPacket(PacketSCPlayerDialogRequest.class);
		Packet.registerPacket(PacketCSPlayerDialogResponse.class);
	}
}