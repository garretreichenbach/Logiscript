package luamade.lua.element.block;

import luamade.luawrap.LuaMadeCallable;
import luamade.luawrap.LuaMadeUserdata;
import org.schema.game.client.controller.element.world.ClientSegmentProvider;
import org.schema.game.common.controller.SendableSegmentProvider;
import org.schema.game.common.data.element.ElementCollection;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.game.network.objects.remote.RemoteTextBlockPair;
import org.schema.game.network.objects.remote.TextBlockPair;

public class BlockControl extends LuaMadeUserdata {
	private final Block block;

	public BlockControl(Block block) {
		this.block = block;
	}

	@LuaMadeCallable
	public void setActive(boolean active) {
		block.getSegmentPiece().setActive(active);
		block.getSegmentPiece().applyToSegment(block.getSegmentPiece().getSegmentController().isOnServer());
		if(block.getSegmentPiece().getSegmentController().isOnServer()) {
			block.getSegmentPiece().getSegmentController().sendBlockActivation(ElementCollection.getEncodeActivation(block.getSegmentPiece(), true, active, false));
		}
	}

	@LuaMadeCallable
	public void setDisplayText(String text) {
		if(block.getSegmentPiece().getType() != ElementKeyMap.TEXT_BOX) {
			return;
		}

		block.getSegmentPiece().getSegmentController().getTextMap().remove(block.getSegmentPiece().getTextBlockIndex());
		block.getSegmentPiece().getSegmentController().getTextMap().put(block.getSegmentPiece().getTextBlockIndex(), text);
		block.getSegmentPiece().applyToSegment(block.getSegmentPiece().getSegmentController().isOnServer());

		if(block.getSegmentPiece().getSegmentController().isOnServer()) {
			TextBlockPair textBlockPair = new TextBlockPair();
			textBlockPair.block = block.getSegmentPiece().getTextBlockIndex();
			textBlockPair.text = text;
			block.getSegmentPiece().getSegmentController().getNetworkObject().textBlockChangeBuffer.add(new RemoteTextBlockPair(textBlockPair, true));
		} else {
			SendableSegmentProvider provider = ((ClientSegmentProvider) block.getSegmentPiece().getSegment().getSegmentController().getSegmentProvider()).getSendableSegmentProvider();
			TextBlockPair pair = new TextBlockPair();
			pair.block = block.getSegmentPiece().getTextBlockIndex();
			pair.text = text;
			provider.getNetworkObject().textBlockResponsesAndChangeRequests.add(new RemoteTextBlockPair(pair, false));
		}
	}
}
