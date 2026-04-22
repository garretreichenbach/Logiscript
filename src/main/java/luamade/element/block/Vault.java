package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPiecePlayerInteractListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import api.network.packets.PacketUtil;
import api.utils.element.Blocks;
import luamade.element.ElementRegistry;
import luamade.network.PacketCSRequestVaultView;
import luamade.system.module.VaultModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.client.controller.manager.ingame.PlayerInteractionControlManager;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.Segment;

/**
 * A programmable credit bank. Each Vault block holds an independent balance
 * identified by a persistent UUID in {@link VaultModuleContainer}.
 *
 * <p>Interacting with the block opens the Vault dialog (balance + deposit /
 * withdraw). Access is gated by adjacent Permission Modules — see
 * {@link luamade.lua.vault.VaultAccessManager} for the full ruleset.
 *
 * <p>Scripts interact with vaults via the {@code vault} Lua global (Stage 2b).
 * All mutations ultimately route through server-authoritative packets so client
 * modifications cannot mint or forge credits.
 */
public class Vault extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener, SegmentPiecePlayerInteractListener {

	public Vault() {
		super("Vault");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A programmable credit bank. Balance persists across server restarts. Place adjacent Permission Modules to control who can deposit and withdraw. Destroying a Vault destroys its balance.");
		blockInfo.setDeprecated(true);
		blockInfo.setInRecipe(false);
		blockInfo.setShoppable(false);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 4);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(true);
		blockInfo.volume = 0.2f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo,
				ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(),
				(int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(),
				new FactoryResource(1, ElementKeyMap.TEXT_BOX),
				new FactoryResource(200, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(Blocks.DECORATIVE_SERVER.getInfo().getBuildIconNum());
		blockInfo.setTextureId(Blocks.DECORATIVE_SERVER.getInfo().getTextureIds());
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.VAULT.getId()) return;
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) return;
		long absIndex = ElementCollection.getIndex(x, y, z);
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) container.removeBlock(absIndex);
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.VAULT.getId()) return;
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) return;
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) container.removeBlock(segmentPiece.getAbsoluteIndex());
	}

	@Override
	public void onInteract(SegmentPiece segmentPiece, PlayerState playerState, PlayerInteractionControlManager playerInteractionControlManager) {
		if(segmentPiece.getType() != ElementRegistry.VAULT.getId()) return;
		if(!(segmentPiece.getSegmentController() instanceof ManagedUsableSegmentController<?>)) return;
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segmentPiece.getSegmentController();
		VaultModuleContainer container = VaultModuleContainer.getContainer(controller.getManagerContainer());
		if(container == null) return;
		String uuid = container.getOrAssignUuid(segmentPiece.getAbsoluteIndex());
		// Pass the block coordinates so the server can locate the piece and run
		// permission checks against its adjacency. Client coordinates are untrusted;
		// the server re-derives the vault UUID from the segment + position.
		PacketUtil.sendPacketToServer(new PacketCSRequestVaultView(
				segmentPiece.getSegmentController().getId(),
				segmentPiece.getAbsoluteIndex()));
	}
}
