package luamade.gui;

import com.bulletphysics.linearmath.Transform;
import luamade.system.module.ComputerModule;
import org.schema.game.client.data.GameClientState;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.SegmentPiece;
import org.schema.game.common.data.element.Element;
import org.schema.schine.graphicsengine.camera.Camera;
import org.schema.schine.graphicsengine.camera.viewer.PositionableViewer;
import org.schema.schine.graphicsengine.core.Timer;
import org.schema.schine.network.objects.Sendable;

import javax.vecmath.Vector3f;

/**
 * Client camera that looks out of a vanilla Camera block's front face, following its entity's client-side transform
 * every frame (so a camera on a turret swings with it). Built like vanilla's MissileTrackCamera.
 */
final class RemoteFeedCamera extends Camera {

	private final Vector3f forward = new Vector3f();
	private final Vector3f up = new Vector3f();
	private final Vector3f right = new Vector3f();
	private final Vector3f local = new Vector3f();
	private int entityId = -1;
	private long absIndex;
	private boolean live;

	RemoteFeedCamera(GameClientState state) {
		super(state, new PositionableViewer());
		setStable(false);
	}

	void aim(int entityId, long absIndex) {
		this.entityId = entityId;
		this.absIndex = absIndex;
	}

	/** Re-resolves the camera block; false when it isn't loaded on this client (out of range) or no longer exists. */
	boolean refresh() {
		live = false;
		if(entityId < 0) {
			return false;
		}
		Sendable sendable = GameClientState.instance.getLocalAndRemoteObjectContainer().getLocalObjects().get(entityId);
		if(!(sendable instanceof SegmentController entity)) {
			return false;
		}
		SegmentPiece piece = entity.getSegmentBuffer().getPointUnsave(absIndex);
		if(piece == null || piece.getType() != ComputerModule.CAMERA_BLOCK_ID) {
			return false;
		}
		Transform transform = entity.getWorldTransformOnClient();
		Vector3f facing = Element.DIRECTIONSf[piece.getOrientation() % 6];

		forward.set(facing);
		transform.basis.transform(forward);
		// pick an up axis that isn't parallel to the lens
		if(Math.abs(facing.y) > 0.9f) up.set(0, 0, 1);
		else up.set(0, 1, 0);
		transform.basis.transform(up);
		right.cross(forward, up);
		right.normalize();
		up.cross(right, forward);
		up.normalize();

		// block centre, nudged out to the lens so the camera's own block doesn't fill the view
		local.set(piece.getAbsolutePosX() - 16 + facing.x * 0.6f, piece.getAbsolutePosY() - 16 + facing.y * 0.6f, piece.getAbsolutePosZ() - 16 + facing.z * 0.6f);
		transform.basis.transform(local);
		local.add(transform.origin);
		getViewable().getPos().set(local);
		live = true;
		return true;
	}

	boolean isLive() {
		return live;
	}

	@Override
	public void update(Timer timer, boolean server) {
		if(!refresh()) {
			return;
		}
		setForward(forward);
		setUp(up);
		setRight(right);
		updateViewer(timer);
	}
}
