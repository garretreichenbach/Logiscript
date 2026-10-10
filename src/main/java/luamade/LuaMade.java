package luamade;

import api.block.BlockConfig;
import api.mod.StarMod;
import api.network.Packet;
import luamade.docs.DocTopic;
import luamade.docs.DocsRepository;
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
import luamade.network.PacketCSTerminalQuery;
import luamade.network.PacketSCTerminalQueryResult;
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
import org.schema.game.client.view.mainmenu.GuidesRegistry;
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
	public void onRegisterGuides(GuidesRegistry.ModGuideRegistrar registrar) {
		for(DocTopic topic : DocsRepository.getTopics()) {
			registrar.register("luamade-" + topic.getSectionKey(), "LuaMade: " + topic.getSectionLabel(), topic.getTitle(), topic.getMarkdown());
		}
	}

	@Override
	public void onBlockConfigLoad(BlockConfig config) {
		ElementRegistry.registerElements();
	}

	@Override
	public void onResourceLoad(ResourceLoader loader) {
		ResourceManager.loadResources(this, loader);
	}

	public void logDebug(String message) {
		if(ConfigManager.isDebugMode()) {
			logMessage("[DEBUG]: [ResourcesReorganized] " + message);
		}
	}

	private void registerPackets() {
		Packet.registerPacket(this, PacketCSRequestDataStoreContents.class);
		Packet.registerPacket(this, PacketSCDataStoreContents.class);
		Packet.registerPacket(this, PacketCSRequestVaultView.class);
		Packet.registerPacket(this, PacketSCVaultView.class);
		Packet.registerPacket(this, PacketCSVaultDeposit.class);
		Packet.registerPacket(this, PacketCSVaultWithdraw.class);
		Packet.registerPacket(this, PacketCSVaultScriptOp.class);
		Packet.registerPacket(this, PacketSCVaultScriptResponse.class);

		// Computer session (scripts execute server-side; these carry input to
		// the server and stream console/gfx output back to viewers).
		Packet.registerPacket(this, PacketCSComputerInput.class);
		Packet.registerPacket(this, PacketSCComputerConnectAck.class);
		Packet.registerPacket(this, PacketSCConsoleSnapshot.class);
		Packet.registerPacket(this, PacketSCGfxSnapshot.class);
		Packet.registerPacket(this, PacketCSFileRead.class);
		Packet.registerPacket(this, PacketSCFileContents.class);
		Packet.registerPacket(this, PacketCSFileWrite.class);
		Packet.registerPacket(this, PacketSCFileResult.class);
		Packet.registerPacket(this, PacketCSClipboardImport.class);
		Packet.registerPacket(this, PacketSCOpenSwingEditor.class);
		Packet.registerPacket(this, PacketSCPlayerDialogRequest.class);
		Packet.registerPacket(this, PacketCSPlayerDialogResponse.class);
		Packet.registerPacket(this, PacketCSTerminalQuery.class);
		Packet.registerPacket(this, PacketSCTerminalQueryResult.class);
	}
}