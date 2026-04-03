package luamade.element.block;

import api.config.BlockConfig;
import api.listener.fastevents.segmentpiece.SegmentPieceKilledListener;
import api.listener.fastevents.segmentpiece.SegmentPieceRemoveListener;
import luamade.element.ElementRegistry;
import luamade.system.module.DataStoreModuleContainer;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.schema.game.common.controller.ManagedUsableSegmentController;
import org.schema.game.common.controller.SendableSegmentController;
import org.schema.game.common.controller.damage.Damager;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.common.data.element.FactoryResource;
import org.schema.game.common.data.world.Segment;

public class DataStore extends Block implements SegmentPieceRemoveListener, SegmentPieceKilledListener {

	public DataStore() {
		super("Data Store");
	}

	@Override
	public void initData() {
		super.initData();
		blockInfo.setDescription("A programmable data store. Key-value data persists across server restarts. Place adjacent Permission Modules to control cross-entity or cross-faction access.");
		blockInfo.setInRecipe(true);
		blockInfo.setShoppable(true);
		blockInfo.setPrice(ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).price * 2);
		blockInfo.setOrientatable(true);
		blockInfo.setCanActivate(false);
		blockInfo.volume = 0.2f;
	}

	@Override
	public void postInitData() {
		BlockConfig.addRecipe(blockInfo, ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getProducedInFactoryType(), (int) ElementKeyMap.getInfo(ElementKeyMap.TEXT_BOX).getFactoryBakeTime(), new FactoryResource(1, ElementKeyMap.TEXT_BOX), new FactoryResource(100, (short) 220));
	}

	@Override
	public void initResources() {
		blockInfo.setBuildIconNum(ElementKeyMap.getInfo(451).getBuildIconNum());
		blockInfo.setTextureId(ElementKeyMap.getInfo(451).getTextureIds());
	}

	@Override
	public void onBlockRemove(short type, int segmentSize, byte x, byte y, byte z, byte b3, Segment segment, boolean preserveControl, boolean server) {
		if(type != ElementRegistry.DATA_STORE.getId()) {
			return;
		}
		if(!(segment.getSegmentController() instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		long absIndex = ElementCollection.getIndex(x, y, z);
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) segment.getSegmentController();
		DataStoreModuleContainer container = DataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(absIndex);
		}
	}

	@Override
	public void onBlockKilled(SegmentPiece segmentPiece, SendableSegmentController sendableSegmentController, @Nullable Damager damager, boolean b) {
		if(segmentPiece == null || segmentPiece.getType() != ElementRegistry.DATA_STORE.getId()) {
			return;
		}
		if(!(sendableSegmentController instanceof ManagedUsableSegmentController<?>)) {
			return;
		}
		ManagedUsableSegmentController<?> controller = (ManagedUsableSegmentController<?>) sendableSegmentController;
		DataStoreModuleContainer container = DataStoreModuleContainer.getContainer(controller.getManagerContainer());
		if(container != null) {
			container.removeBlock(segmentPiece.getAbsoluteIndex());
		}
	}
}
