package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import api.utils.element.Blocks;
import luamade.element.ElementRegistry;
import luamade.system.module.NetworkedDataStoreModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.world.Segment;

/**
 * Element block definition for the Networked Data Store.
 *
 * <p>Unlike the regular {@link DataStore}, a Networked Data Store registers
 * itself in a global name registry so that its data can be accessed by any
 * computer without the owning entity being loaded.
 *
 * <p>When the block is removed or destroyed, its registration and backing data
 * are deleted.
 */
public class NetworkedDataStore extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public NetworkedDataStore() {
		super("Networked Data Store");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A networked data store. Registers a named key-value store in a global registry accessible by any computer via the network API, even when this entity is unloaded. Data is destroyed when this block is removed.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 4);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(false);
		blockInfo.volume = 0.2f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(200, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(Blocks.DECORATIVE_SERVER.getInfo().getBuildIconNum());
		blockInfo.setTextureId(Blocks.DECORATIVE_SERVER.getInfo().getTextureIds());
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.NETWORKED_DATA_STORE.getId()) return;
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) return;
		long absIndex = ElementCollection.getIndex(x, y, z);
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
		NetworkedDataStoreModuleContainer container = NetworkedDataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(absIndex);
		}
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.NETWORKED_DATA_STORE.getId()) return;
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) return;
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		NetworkedDataStoreModuleContainer container = NetworkedDataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(segmentPiece.getAbsoluteIndex());
		}
	}
}
